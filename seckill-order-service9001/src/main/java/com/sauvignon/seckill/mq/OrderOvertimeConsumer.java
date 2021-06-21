package com.sauvignon.seckill.mq;

import com.sauvignon.seckill.constants.ServiceCode;
import com.sauvignon.seckill.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQReplyListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RocketMQMessageListener(
        consumerGroup = "${order-handle.mq.consumerGroup.overtimeConsumer}",
        topic = "${order-handle.mq.topic}",
        selectorExpression = "${order-handle.mq.tag.overTimeTag}")
public class OrderOvertimeConsumer implements RocketMQReplyListener<String,String>
{
    @Autowired
    private OrderService orderService;

    @Override
    public String onMessage(String orderFlagKey)
    {
        //尝试：为了保证库存的数量
        int retries=2;
        //1. 找redis,看时间
        if(!orderService.checkOrderOvertime(orderFlagKey))
            throw new RuntimeException("异常：此消息到达时间早于超时时间！");
        //2. 干掉超时订单
        Integer code = orderService.abandonOvertimeOrder(orderFlagKey).getCode();
        while(code.equals(ServiceCode.RETRY) && retries-->0)
            code=orderService.abandonOvertimeOrder(orderFlagKey).getCode();

        return orderFlagKey;
    }
}
