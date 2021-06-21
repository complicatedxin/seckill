package com.sauvignon.seckill.mq;

import com.sauvignon.seckill.constants.Constants;
import com.sauvignon.seckill.pojo.entities.Order;
import com.sauvignon.seckill.service.OrderService;
import com.sauvignon.seckill.utils.ZkClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RocketMQMessageListener(
        consumerGroup = "${order-handle.mq.consumerGroup.orderConsumer}",
        topic = "${order-handle.mq.topic}",
        selectorExpression = "${order-handle.mq.tag.consumeTag}")
public class OrderHandleConsumer implements RocketMQListener<Order>
{
    @Autowired
    private OrderService orderService;

    @Override
    public void onMessage(Order order)
    {
        orderService.handleOrder(order);
    }
}
