package com.sauvignon.seckill.service.impl;

import com.sauvignon.seckill.constants.Constants;
import com.sauvignon.seckill.constants.OrderStatus;
import com.sauvignon.seckill.constants.ResponseCode;
import com.sauvignon.seckill.constants.ServiceCode;
import com.sauvignon.seckill.mapper.OrderMapper;
import com.sauvignon.seckill.mq.MessageProvider;
import com.sauvignon.seckill.pojo.dto.ResponseResult;
import com.sauvignon.seckill.pojo.dto.ServiceResult;
import com.sauvignon.seckill.pojo.entities.Commodity;
import com.sauvignon.seckill.pojo.entities.Order;
import com.sauvignon.seckill.service.OrderService;
import com.sauvignon.seckill.service.StorageService;
import com.sauvignon.seckill.utils.RedisUtil;
import com.sauvignon.seckill.utils.ZkClient;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Service
public class OrderServiceImpl implements OrderService
{
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private StorageService storageService;
    @Autowired
    private MessageProvider messageProvider;

    @Value("${order-handle.mq.topic}")
    private String msgTopic;
    @Value("${order-handle.mq.tag.overTimeTag}")
    private String overTimeTag;

    @Override
    @GlobalTransactional(name = "create-order",rollbackFor = Exception.class)//回退失败可能在库存服务上
    public void handleOrder(Order order)
    {
        //1. 验证信息
        Long commodityId = order.getCommodityId();
        Integer orderCount = order.getCount();
        Long orderId = order.getOrderId();
        if(orderId ==null
                || commodityId ==null
                || order.getUserId()==null
                || orderCount !=1)
            throw new IllegalArgumentException("订单参数错误");
        //2. 从redis获取consumed
        Integer result = (Integer) redisUtil.lPop(Constants.consumedRedisKey(commodityId));
        if(result==null) return;
        //3. 合算价钱
        Commodity commodity = storageService.findOne(commodityId).getBody();
        BigDecimal amount = commodity.getPrice().multiply(new BigDecimal(orderCount));
        if(amount.compareTo(new BigDecimal(0))==-1)//价钱不能比0小
            throw new IllegalArgumentException("总价不能为负：单价或商品数量有误");
        //4. 生成预订单
        order.setAmount(amount);
        order.setOrderStatus(OrderStatus.PRE_CREATE);
        //-- 申请分布式锁
        CuratorFramework client = ZkClient.getClient();
        client.start();
        InterProcessMutex lock=
                new InterProcessMutex(client, Constants.orderStatusLockPath(orderId));
        try {
            boolean acquire = lock.acquire(3, TimeUnit.SECONDS);
            if(acquire)
            {
                this.addOne(order);
                //4. 调用减库存服务修改consumed字段（废除：减少分布式锁争用）
//                ResponseResult response=storageService.increaseConsumed(commodityId, orderCount);
//                if(!response.getCode().equals(ResponseCode.SUCCESS))
//                    throw new RuntimeException("storageService执行increaseConsumed失败");
                //5. 生成订单
                this.updateStatus(orderId,OrderStatus.CREATED);
            }
            else
                throw new RuntimeException("订单创建失败：未获取到锁！consumedNum:"+result);
        } catch (Exception e) {
            //下单失败：回退consumed
            String message = e.getMessage();
            Integer consumedNum=Integer.parseInt(message.substring(message.lastIndexOf(":")+1));
            redisUtil.lPush(Constants.consumedRedisKey(commodityId),consumedNum);
        } finally {
            try {
                lock.release();
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean checkOrderOvertime(String orderFlagKey)
    {
        long remindTime = redisUtil.ttl(orderFlagKey);
        //消息延迟2分钟，判断依据90s，不可能出现接到此消息还没过期的情况
        boolean b = remindTime <= Constants.SECKILL_ORDER_OVERTIME;
        //如果未超时，重发delay消息
        if(!b)
            messageProvider.sendDelay(msgTopic,overTimeTag,orderFlagKey);
        return b;
    }

    @Override
    @GlobalTransactional(name = "abandon-order",rollbackFor = Exception.class)
    public ServiceResult abandonOvertimeOrder(String orderFlagKey)
    {
        //修改order状态并回退consumed
        Long orderId = (Long) redisUtil.get(orderFlagKey);
        Order order=null;
        //1. 获取分布式锁
        CuratorFramework client = ZkClient.getClient();
        client.start();
        InterProcessMutex lock=new InterProcessMutex(client,
                Constants.orderStatusLockPath(orderId));
        try {
            //尝试2s：略低于要对状态修改的操作中获取锁的时间
            boolean acquire = lock.acquire(2, TimeUnit.SECONDS);
            if(acquire)
            {
                //2. 检查订单状态
                order = orderMapper.findOne(orderId);
                //如果订单没有创建成功，直接return
                if(order==null)
                    return new ServiceResult(ServiceCode.SUCCESS,"订单没有创建成功");
                Integer status = order.getOrderStatus();
                //没有支付结果，订单失效
                if( ! ( status.equals(OrderStatus.PURCHASED)
                                || status.equals(OrderStatus.PURCHASE_FAIL) ) )
                    orderMapper.updateStatus(orderId,OrderStatus.PURCHASE_FAIL);
                else
                    return new ServiceResult(ServiceCode.SUCCESS,"订单已结束");
            }
            else
                return new ServiceResult(ServiceCode.RETRY,"未获取到锁");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                lock.release();
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //3. 回退库存：调用库存服务（弃用：减少分布式锁）
//        ResponseResult response=
//                storageService.decreaseConsumed(order.getCommodityId(),-order.getCount());
//        if(!response.getCode().equals(ResponseCode.SUCCESS))
//            throw new RuntimeException("storageService执行decreaseConsumed异常");
//        else
//            return new ServiceResult(ServiceCode.SUCCESS,"回退库存成功");
        //3. 释放consumed
        Long commodityId = Constants.commodityIdInOrderFlagKey(orderFlagKey);
        Long consumedIndex = redisUtil.lPush(Constants.consumedRedisKey(commodityId), 0);
        if(consumedIndex==null)
            throw new RuntimeException("redis执行lPush->consumed异常");
        return new ServiceResult(ServiceCode.SUCCESS,"回退库存成功");
    }

    @Override
    public Order findOne(Long orderId)
    {
        return orderMapper.findOne(orderId);
    }

    @Override
    public ServiceResult<Integer> addOne(Order order)
    {
        int addCount = 0;
        try {
            addCount = orderMapper.addOne(order);
        } catch (Exception e) {
            return new ServiceResult<>(ServiceCode.FAIL,"addOne失败",null);
        }
        return new ServiceResult<>(ServiceCode.SUCCESS,"addOne完毕",addCount);
    }

    @Override
    public ServiceResult<Integer> updateStatus(Long orderId, Integer orderStatus)
    {
        int updateCount = 0;
        try {
            updateCount = orderMapper.updateStatus(orderId, orderStatus);
        } catch (Exception e) {
            return new ServiceResult<>(ServiceCode.FAIL,"updateStatus失败",null);
        }
        return new ServiceResult<>(ServiceCode.SUCCESS,"updateStatus完毕",updateCount);
    }


    @Override
    public ServiceResult<Integer> paymentPreCheck(Long orderId, String orderFlag)
    {
        //1.判断用户的订单超时时间
        //如果没超时那么用户可能支付的上(退单服务在30s后执行)
        //如果30s外支付完，那么失败，退钱(期间内库存已回退了)
        //超时当失败处理，不允许在付款，等待库存回退
        long expireTime = redisUtil.ttl(orderFlag);
        if(expireTime <= Constants.SECKILL_ORDER_OVERTIME)
            return new ServiceResult<>(ServiceCode.FAIL,"订单已结束",OrderStatus.FINISHED);
        //2. 获取分布式锁
        CuratorFramework client = ZkClient.getClient();
        client.start();
        InterProcessMutex lock=new InterProcessMutex(client,
                Constants.orderStatusLockPath(orderId));
        try {
            //尝试2s：略低于其他订单状态修改操作（如：下订单时的频繁修改）
            boolean acquire = lock.acquire(2, TimeUnit.SECONDS);
            if(acquire)
            {
                //3. 检查订单状态
                Order order = findOne(orderId);
                //如果订单没有创建成功，直接return
                if(order==null)
                    return new ServiceResult<>(ServiceCode.FAIL, "order==null",OrderStatus.CREATE_FAIL);
                Integer status = order.getOrderStatus();
                //如果状态为已创建，可以直接支付
                if(status.equals(OrderStatus.CREATED))
                {
                    //3. 标记预支付状态
                    updateStatus(orderId,OrderStatus.PRE_PURCHASE);
                    return new ServiceResult<>(ServiceCode.SUCCESS,"订单已经创建完",OrderStatus.CREATED);
                }
                //现在是秒杀！如果状态为支付前，说明用户之前进入到扫码支付界面，但支付结果未知，再次请求不接受
                else if(status.equals(OrderStatus.PRE_PURCHASE))
                    return new ServiceResult<>(ServiceCode.FAIL,"已开启了一次支付操作",OrderStatus.PRE_PURCHASE);
                else //订单结束，订单创建中间态（有问题了）
                    return new ServiceResult<>(ServiceCode.FAIL,"订单已结束",OrderStatus.FINISHED);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                lock.release();
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new ServiceResult<>(ServiceCode.RETRY,"未获取到锁",null);
    }

    @Override
    public ServiceResult<Order> paymentPostCheck(Long orderId)
    {
        //获取锁
        CuratorFramework client = ZkClient.getClient();
        client.start();
        InterProcessMutex lock=new InterProcessMutex(client,
                Constants.orderStatusLockPath(orderId));
        try {
            boolean acquire = lock.acquire(2, TimeUnit.SECONDS);
            if(acquire)
            {
                Order order = findOne(orderId);
                Integer status = order.getOrderStatus();
                //正常情况下，状态紧接pre_purchase
                if(status.equals(OrderStatus.PRE_PURCHASE))
                {
                    updateStatus(orderId,OrderStatus.PURCHASED);
                    return new ServiceResult<>(ServiceCode.SUCCESS,
                            "订单状态正常",order);
                }
                else //不正常状态都退款
                {
                    updateStatus(orderId,OrderStatus.WAIT_TO_REFUND);
                    return new ServiceResult<>(ServiceCode.FAIL,
                            "订单状态异常，需要回退",order);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                lock.release();
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new ServiceResult<>(ServiceCode.RETRY,"未获取到锁",null);
    }
}
