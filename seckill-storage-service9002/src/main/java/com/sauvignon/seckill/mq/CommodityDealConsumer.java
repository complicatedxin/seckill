package com.sauvignon.seckill.mq;

import com.sauvignon.seckill.pojo.entities.Order;
import com.sauvignon.seckill.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.LongAdder;

@Component
@Slf4j
@RocketMQMessageListener(
        consumerGroup = "${storage-manage.mq.consumerGroup.dealConsumer}",
        topic = "${storage-manage.mq.topic.paymentSideTopic}",
        selectorExpression = "${storage-manage.mq.tag.dealTag}")
public class CommodityDealConsumer implements RocketMQListener<Order>
{
    @Autowired
    private StorageService storageService;

    @Override
    public void onMessage(Order order)
    {
        //TODO: LongAdder 减少锁争用
        storageService.deal(order.getCommodityId(),order.getCount());
    }
}
