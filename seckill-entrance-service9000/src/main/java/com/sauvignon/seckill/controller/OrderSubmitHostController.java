package com.sauvignon.seckill.controller;

import cn.hutool.crypto.digest.DigestUtil;
import com.sauvignon.seckill.constants.Constants;
import com.sauvignon.seckill.constants.SkActivityStatus;
import com.sauvignon.seckill.mq.MessageProvider;
import com.sauvignon.seckill.pojo.dto.ResponseResult;
import com.sauvignon.seckill.pojo.entities.Order;
import com.sauvignon.seckill.pojo.entities.SeckillActivity;
import com.sauvignon.seckill.service.SkActivityService;
import com.sauvignon.seckill.utils.JwtUtil;
import com.sauvignon.seckill.utils.RedisUtil;
import com.sauvignon.seckill.utils.SnowflakeIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
public class OrderSubmitHostController
{
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private MessageProvider messageProvider;
    @Autowired
    private SkActivityService skActivityService;

    @Value("${server.port}")
    private String serverPort;
    @Value("${order-submit-host.mq.topic}")
    private String msgTopic;
    @Value("${order-submit-host.mq.tag.consumeTag}")
    private String consumeTag;
    @Value("${order-submit-host.mq.tag.overTimeTag}")
    private String overTimeTag;

    private SnowflakeIdWorker snowflake=new SnowflakeIdWorker(0,0);

    //todo
    @PostMapping("/seckill/order/host/{idHex}/{userId}")
    public ResponseResult<String> host(@PathVariable("idHex")String idHex,
                                       @PathVariable("userId")Long userId,
                                       HttpServletRequest request,
                                       @RequestParam String activityId)
    {
        //1. 校验
        //1.1 验证idHex是否与请求id一致
//        Long userId = JwtUtil.getUserId((request.getHeader(HttpHeaders.AUTHORIZATION)));
        if(!DigestUtil.md5Hex(String.valueOf(userId)).equals(idHex))
            return new ResponseResult(300,"用户请求错误",null);
        //1.2 验证活动是否开启
        SeckillActivity activity = skActivityService.getActivity(activityId);
        if(activity==null
                || !activity.getActivityStatus().equals(SkActivityStatus.OPENING))
            return new ResponseResult<String>(404,"未找到该页面",null);
        //1.3 redis验证是否提交过订单
        Long commodityId = activity.getCommodityId();
        String orderFlagKey=Constants.seckillOrderFlag(userId,commodityId);
        Long orderFlag = (Long) redisUtil.get(orderFlagKey);
        if(orderFlag!=null)
            return new ResponseResult(301,"重复下单",null);

        //2. 转交给下单业务并记录
        long orderId = snowflake.nextId();
        Order order=new Order(orderId,
                commodityId, Constants.SECKILL_COMMODITY_COUNT,
                userId,null,null);
        //2.- TODO：记录:异步存入mysql
        //2.1 标记redis（key用于判断是否下过单，时长0.5小时；value存储订单号用于支付超时）
        redisUtil.setnx(orderFlagKey, orderId,
                Constants.SECKILL_ORDER_TIME_RESTRICTION,
                TimeUnit.SECONDS);
        //2.2 通知rocketMQ：
        //   ①通知处理订单
        boolean sendResult = messageProvider.syncSendOrderly(msgTopic, consumeTag,
                order,
                Constants.ORDER_MESSAGE_HASHKEY);
        if(!sendResult)
        {
            redisUtil.del(orderFlagKey);
            return new ResponseResult(200,"下单失败",null);
        }
        //   ②通知检查订单支付超时
        messageProvider.sendDelay(msgTopic,overTimeTag, orderFlagKey);

        //3. 返回支付链接地址
        String paymentUrl=Constants.paymentHostPath(orderId,userId,commodityId);
        return new ResponseResult(200,"下单成功",paymentUrl);
    }


}
