package com.sauvignon.seckill.mq;

import com.sauvignon.seckill.constants.Constants;
import com.sauvignon.seckill.constants.OrderStatus;
import com.sauvignon.seckill.mapper.OrderMapper;
import com.sauvignon.seckill.pojo.entities.Commodity;
import com.sauvignon.seckill.pojo.entities.Order;
import com.sauvignon.seckill.service.OrderService;
import com.sauvignon.seckill.service.StorageService;
import com.sauvignon.seckill.utils.RedisUtil;
import com.sauvignon.seckill.utils.ZkClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
    private OrderMapper orderMapper;
    @Autowired
    private OrderService orderService;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private StorageService storageService;

    @Override
    public void onMessage(Order order)
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
        Integer consumedNo = (Integer) redisUtil.lPop(Constants.consumedRedisKey(commodityId));
        if(consumedNo==null) return;
        //3. 合算价钱
        Commodity commodity = storageService.findOne(commodityId).getBody();
        BigDecimal amount = commodity.getPrice().multiply(new BigDecimal(orderCount));
        if(amount.compareTo(new BigDecimal(0))==-1)//价钱不能比0小
            throw new IllegalArgumentException("总价不能为负：单价或商品数量有误");
        //4. 生成预订单
        try {
            //4. 生成预订单
            order.setAmount(amount);
            order.setOrderStatus(OrderStatus.PRE_CREATE);
            orderMapper.addOne(order);
            //5. 执行下单分布式事务
            orderService.handleOrder(order);
        } catch (Exception e) {
            redisUtil.lPush(Constants.consumedRedisKey(order.getCommodityId()),consumedNo);
            log.info("下单失败！" + orderId);
        }
    }
}
