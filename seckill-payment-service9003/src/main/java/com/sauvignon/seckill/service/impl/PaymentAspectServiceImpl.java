package com.sauvignon.seckill.service.impl;

import com.sauvignon.seckill.constants.Constants;
import com.sauvignon.seckill.constants.OrderStatus;
import com.sauvignon.seckill.constants.ResponseCode;
import com.sauvignon.seckill.constants.ServiceCode;
import com.sauvignon.seckill.pojo.dto.ResponseResult;
import com.sauvignon.seckill.pojo.dto.ServiceResult;
import com.sauvignon.seckill.pojo.entities.Order;
import com.sauvignon.seckill.service.OrderService;
import com.sauvignon.seckill.service.PaymentAspectService;
import com.sauvignon.seckill.service.StorageService;
import com.sauvignon.seckill.utils.RedisUtil;
import com.sauvignon.seckill.utils.ZkClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class PaymentAspectServiceImpl implements PaymentAspectService
{
    @Autowired
    private OrderService orderService;
    @Autowired
    private StorageService storageService;
    @Autowired
    private RedisUtil redisUtil;

    @Override
    public ServiceResult<Integer> paymentPreCheck(Long orderId, String orderFlag)
    {
        return orderService.paymentPreCheck(orderId,orderFlag);
    }


    @Override
    public ServiceResult<Long> paymentDeal(Long orderId)
    {
        //1. 调用订单服务修改状态（如果支付完发现该订单已被回退（超时），那么回退付款）
        ResponseResult<Order> checkResult = orderService.paymentPostCheck(orderId);
        Order order=checkResult.getBody();
        if(!checkResult.getCode().equals(ResponseCode.SUCCESS))
            return new ServiceResult<>(ServiceCode.FAIL,"退款",orderId);
        //2. 再调仓储服务修改数量
        ResponseResult dealResult = storageService.deal(order.getCommodityId(), order.getCount());
        if(!dealResult.getCode().equals(ResponseCode.SUCCESS))
            throw new RuntimeException("storage执行deal失败");
        return new ServiceResult<>(ServiceCode.SUCCESS,"success",orderId);
    }


}
