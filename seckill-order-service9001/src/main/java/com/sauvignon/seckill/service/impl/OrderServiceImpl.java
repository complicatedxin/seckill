package com.sauvignon.seckill.service.impl;

import com.sauvignon.seckill.constants.Constants;
import com.sauvignon.seckill.constants.OrderStatus;
import com.sauvignon.seckill.constants.ServiceCode;
import com.sauvignon.seckill.mapper.OrderMapper;
import com.sauvignon.seckill.mq.MessageProvider;
import com.sauvignon.seckill.pojo.dto.ServiceResult;
import com.sauvignon.seckill.pojo.entities.Commodity;
import com.sauvignon.seckill.pojo.entities.Order;
import com.sauvignon.seckill.service.OrderService;
import com.sauvignon.seckill.service.StorageService;
import com.sauvignon.seckill.utils.RedisUtil;
import com.sauvignon.seckill.utils.ZkClient;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
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
    @GlobalTransactional(name = "create-order", timeoutMills = 15000,rollbackFor = Exception.class)
    public void handleOrder(Order order)
    {
        //4.1 调用减库存服务修改consumed字段（废除：减少分布式锁争用）
//        ResponseResult response=storageService.increaseConsumed(commodityId, orderCount);
//        if(!response.getCode().equals(ResponseCode.SUCCESS))
//            throw new RuntimeException("storageService执行increaseConsumed失败");
        //4.2 调用其他用户服务
        //
        //5. 生成订单
        int updateSign=orderMapper.updateStatus(order.getOrderId(), OrderStatus.CREATED);
        if(updateSign!=1)
            throw new RuntimeException("订单创建失败！");
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
        InterProcessSemaphoreMutex lock=new InterProcessSemaphoreMutex(client,
                Constants.orderStatusLockPath(orderId));
        try {
            //尝试2s：略低于要对状态修改的操作中获取锁的时间
            boolean acquire = lock.acquire(2, TimeUnit.SECONDS);
            if(acquire)
            {
                //2. 检查订单状态
                order = orderMapper.findOne(orderId);
                //如果订单没有创建成功，直接return
                if(order==null || order.getOrderStatus().equals(OrderStatus.PRE_CREATE))
                {
                    orderMapper.updateStatus(orderId,OrderStatus.CREATE_FAIL);
                    return new ServiceResult(ServiceCode.SUCCESS,"订单没有创建成功");
                }
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
    public int addOne(Order order)
    {
        int addCount = 0;
        try {
            addCount = orderMapper.addOne(order);
        } catch (Exception e) {
            throw new RuntimeException("订单添加数据库失败！");
        }
        return addCount;
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


}
