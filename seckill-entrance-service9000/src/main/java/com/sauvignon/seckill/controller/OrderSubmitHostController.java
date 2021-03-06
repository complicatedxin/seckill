package com.sauvignon.seckill.controller;

import cn.hutool.crypto.digest.DigestUtil;
import com.sauvignon.seckill.constants.Constants;
import com.sauvignon.seckill.constants.SkActivityStatus;
import com.sauvignon.seckill.mq.MessageProvider;
import com.sauvignon.seckill.pojo.dto.ResponseResult;
import com.sauvignon.seckill.pojo.entities.Order;
import com.sauvignon.seckill.pojo.entities.OrderLog;
import com.sauvignon.seckill.pojo.entities.SeckillActivity;
import com.sauvignon.seckill.schedule.RecordOrderTask;
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
import java.util.concurrent.ThreadPoolExecutor;
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
    @Autowired
    private ThreadPoolExecutor threadPool;
    @Autowired
    private RecordOrderTask recordOrderTask;

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
        //1. ??????
        //1.1 ??????idHex???????????????id??????
//        Long userId = JwtUtil.getUserId((request.getHeader(HttpHeaders.AUTHORIZATION)));
        if(!DigestUtil.md5Hex(String.valueOf(userId)).equals(idHex))
            return new ResponseResult(300,"??????????????????",null);
        //1.2 ????????????????????????
        SeckillActivity activity = skActivityService.getActivity(activityId);
        if(activity==null
                || !activity.getActivityStatus().equals(SkActivityStatus.OPENING))
            return new ResponseResult<String>(404,"??????????????????",null);
        //1.3 redis???????????????????????????
        Long commodityId = activity.getCommodityId();
        String orderFlagKey=Constants.seckillOrderFlag(userId,commodityId);
        Long orderFlag = (Long) redisUtil.get(orderFlagKey);
        if(orderFlag!=null)
            return new ResponseResult(301,"????????????",null);

        //2. ??????????????????????????????
        long orderId = snowflake.nextId();
        Order order=new Order(orderId,
                commodityId, Constants.SECKILL_COMMODITY_COUNT,
                userId,null,null);
        //2.1 ??????redis???key????????????????????????????????????0.5?????????value????????????????????????????????????
        redisUtil.setnx(orderFlagKey, orderId,
                Constants.SECKILL_ORDER_TIME_RESTRICTION,
                TimeUnit.SECONDS);
        //2.2 ??????rocketMQ???
        //   ?????????????????????
        boolean sendResult = messageProvider.syncSendOrderly(msgTopic, consumeTag,
                order,
                Constants.ORDER_MESSAGE_HASHKEY);
        if(!sendResult)
        {
            redisUtil.del(orderFlagKey);
            return new ResponseResult(200,"????????????",null);
        }
        //   ?????????????????????????????????
        messageProvider.sendDelay(msgTopic,overTimeTag, orderFlagKey);
        //2.3 ??????:????????????mysql
        threadPool.submit(recordOrderTask.record(
                new OrderLog(orderId,commodityId,order.getCount(),userId) ));
        //3. ????????????????????????
        String paymentUrl=Constants.paymentHostPath(orderId,userId,commodityId);
        return new ResponseResult(200,"????????????",paymentUrl);
    }


}
