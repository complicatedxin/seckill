package com.sauvignon.seckill.service.impl;

import com.sauvignon.seckill.constants.Constants;
import com.sauvignon.seckill.constants.ResponseCode;
import com.sauvignon.seckill.constants.ServiceCode;
import com.sauvignon.seckill.mq.MessageProvider;
import com.sauvignon.seckill.pojo.dto.ResponseResult;
import com.sauvignon.seckill.pojo.dto.ServiceResult;
import com.sauvignon.seckill.pojo.entities.Order;
import com.sauvignon.seckill.service.OrderService;
import com.sauvignon.seckill.service.PaymentAspectService;
import com.sauvignon.seckill.service.StorageService;
import com.sauvignon.seckill.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentAspectServiceImpl implements PaymentAspectService
{
    @Autowired
    private OrderService orderService;
    @Autowired
    private StorageService storageService;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private MessageProvider messageProvider;

    @Value("${payment-aspect.mq.topic}")
    private String msgTopic;
    @Value("${payment-aspect.mq.tag.dealTag}")
    private String dealTag;

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
        //2. 发送消息通知仓储服务扣减数据库数据
        messageProvider.asyncSendOrderly(msgTopic,dealTag,
                order,
                Constants.ORDER_MESSAGE_HASHKEY);

        return new ServiceResult<>(ServiceCode.SUCCESS,"success",orderId);
    }


}
