package com.sauvignon.seckill.service.impl;

import com.sauvignon.seckill.constants.Constants;
import com.sauvignon.seckill.constants.OrderStatus;
import com.sauvignon.seckill.constants.ResponseCode;
import com.sauvignon.seckill.constants.ServiceCode;
import com.sauvignon.seckill.mapper.OrderMapper;
import com.sauvignon.seckill.mq.MessageProvider;
import com.sauvignon.seckill.pojo.dto.ResponseResult;
import com.sauvignon.seckill.pojo.dto.ServiceResult;
import com.sauvignon.seckill.pojo.entities.Order;
import com.sauvignon.seckill.service.PaymentAspectService;
import com.sauvignon.seckill.service.StorageService;
import com.sauvignon.seckill.utils.RedisUtil;
import com.sauvignon.seckill.utils.ZkClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class PaymentAspectServiceImpl implements PaymentAspectService
{
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private StorageService storageService;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private RedissonClient redisson;
    @Autowired
    private MessageProvider messageProvider;

    @Value("${payment-aspect.mq.topic}")
    private String msgTopic;
    @Value("${payment-aspect.mq.tag.dealTag}")
    private String dealTag;

    @Override
    public ServiceResult<Integer> paymentPreCheck(Long orderId, String orderFlag)
    {
        //1.1 判断用户的订单超时时间
        //如果没超时那么用户可能支付的上(退单服务在30s后执行)
        //如果30s外支付完，那么失败，退钱(期间内库存已回退了)
        //超时当失败处理，不允许在付款，等待库存回退
        long expireTime = redisUtil.ttl(orderFlag);
        boolean b = expireTime <= Constants.SECKILL_ORDER_OVERTIME;
        if(b)
            return new ServiceResult<>(ServiceCode.FAIL,"订单已结束", OrderStatus.FINISHED);
        //1.2 查看订单是否存在：下单业务中失败
        Order order = orderMapper.findOne(orderId);
        //如果订单没有创建成功，直接return
        if(order==null)
            return new ServiceResult<>(ServiceCode.RETRY, "order==null：订单未处理",OrderStatus.PRE_CREATE);
        //2. 获取分布式锁
        RLock lock = redisson.getFairLock(Constants.orderStatusLockPath(orderId));
        try {
            //尝试2s: 争用高，多次尝试
            boolean acquire = lock.tryLock(2, TimeUnit.SECONDS);
            if(acquire)
            {
                //3. 检查订单状态
                order = orderMapper.findOne(orderId);//保证状态
                //如果订单没有创建成功，直接return
                Integer orderStat;
                if(order==null ||
                        ((orderStat=order.getOrderStatus()).equals(OrderStatus.PRE_CREATE)
                                || orderStat.equals(OrderStatus.CREATE_FAIL)) )
                    return new ServiceResult<>(ServiceCode.FAIL, "订单创建失败",OrderStatus.CREATE_FAIL);
                orderStat = order.getOrderStatus();
                //如果状态为已创建，可以直接支付
                if(orderStat.equals(OrderStatus.CREATED))
                {
                    //3. 标记预支付状态
                    orderMapper.updateStatus(orderId,OrderStatus.PRE_PURCHASE);
                    return new ServiceResult<>(ServiceCode.SUCCESS,"订单已经创建完",OrderStatus.CREATED);
                }
                //现在是秒杀:如果状态为支付前，说明用户之前进入到扫码支付界面，但支付结果未知，再次请求不接受
                else if(orderStat.equals(OrderStatus.PRE_PURCHASE))
                    return new ServiceResult<>(ServiceCode.FAIL,"已开启了一次支付操作",OrderStatus.PRE_PURCHASE);
                else //订单结束，订单创建中间态（有问题）
                    return new ServiceResult<>(ServiceCode.FAIL,"订单已结束",OrderStatus.FINISHED);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                lock.unlock();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new ServiceResult<>(ServiceCode.RETRY,"未获取到锁",null);
    }


    @Override
    public ServiceResult<Long> paymentDeal(Long orderId)
    {
        //1. 调用订单服务修改状态（如果支付完发现该订单已被回退（超时），那么回退付款）
        ServiceResult<Order> checkResult = paymentPostCheck(orderId);
        int retries=2;
        while(checkResult.getCode().equals(ServiceCode.RETRY) && retries-->0)
            checkResult=paymentPostCheck(orderId);
        if(!checkResult.getCode().equals(ServiceCode.SUCCESS))
            return new ServiceResult<>(ServiceCode.FAIL,"退款",orderId);
        //2. 发送消息通知仓储服务扣减数据库数据
        Order order=checkResult.getBody();
        ServiceResult serviceResult = sendDealMsgToStorage(order);
        int reties=2;
        while(serviceResult.getCode().equals(ServiceCode.RETRY) && reties-->0)
            serviceResult=sendDealMsgToStorage(order);
        if(!serviceResult.getCode().equals(ServiceCode.SUCCESS))
            return new ServiceResult<>(ServiceCode.FAIL,"退款",orderId);
        return new ServiceResult<>(ServiceCode.SUCCESS,"success",orderId);
    }
    private ServiceResult sendDealMsgToStorage(Order order)
    {
        boolean send = messageProvider.syncSendOrderly(msgTopic, dealTag,
                order,
                Constants.ORDER_MESSAGE_HASHKEY);
        if(!send)
            return new ServiceResult(ServiceCode.RETRY,"发送失败");
        return new ServiceResult(ServiceCode.SUCCESS,"发送成功");
    }
    private ServiceResult<Order> paymentPostCheck(Long orderId)
    {
        //获取锁
        RLock lock = redisson.getFairLock(Constants.orderStatusLockPath(orderId));
        try {
            boolean acquire = lock.tryLock(3, TimeUnit.SECONDS);
            if(acquire)
            {
                Order order = orderMapper.findOne(orderId);
                Integer status = order.getOrderStatus();
                //正常情况下，状态紧接pre_purchase
                if(status.equals(OrderStatus.PRE_PURCHASE))
                {
                    orderMapper.updateStatus(orderId,OrderStatus.PURCHASED);
                    return new ServiceResult<>(ServiceCode.SUCCESS, "订单状态正常",order);
                }
                else //不正常状态都退款
                {
                    orderMapper.updateStatus(orderId,OrderStatus.WAIT_TO_REFUND);
                    return new ServiceResult<>(ServiceCode.FAIL, "订单状态异常，需要回退",order);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                lock.unlock();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new ServiceResult<>(ServiceCode.RETRY,"未获取到锁",null);
    }


}
