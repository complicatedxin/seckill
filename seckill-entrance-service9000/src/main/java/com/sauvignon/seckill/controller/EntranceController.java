package com.sauvignon.seckill.controller;

import com.auth0.jwt.exceptions.AlgorithmMismatchException;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.sauvignon.seckill.constants.SkActivityStatus;
import com.sauvignon.seckill.pojo.dto.ResponseResult;
import com.sauvignon.seckill.pojo.entities.SeckillActivity;
import com.sauvignon.seckill.service.SkActivityService;
import com.sauvignon.seckill.utils.JwtUtil;
import com.sauvignon.seckill.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@RestController
public class EntranceController
{
    @Value("${server.port}")
    private String serverPort;

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private SkActivityService skActivityService;

    //todo
    @RequestMapping("/seckill/getEntrance/{skActivityId}/{userId}")
    public ResponseResult<String> getEntrance(@PathVariable("skActivityId")String skActivityId,
                                              @PathVariable("userId")Long userId,
                                              HttpServletRequest request)
    {
        //TODO:对恶意请求处理：sentinel? gateway?
        //必须是用户请求
//        Long userId= null;
//        try {
//            userId = JwtUtil.getUserId(request.getHeader(HttpHeaders.AUTHORIZATION));
//        } catch (AlgorithmMismatchException
//                | SignatureVerificationException
//                | TokenExpiredException
//                | InvalidClaimException e) {
//            e.printStackTrace();
//        }
        //验证活动是否开始
        SeckillActivity activity = skActivityService.getActivity(skActivityId);
        if(activity==null)
            return new ResponseResult<String>(404,"未找到该页面",null);
        //如果活动时间到了，但后台开启指令没接收到，那么服务层自动开启
        if(!activity.getActivityStatus().equals(SkActivityStatus.OPENING))
        {
            Date openTime = activity.getOpenTime();
            Date now=new Date(System.currentTimeMillis());
            if(openTime.compareTo(now)<0)
                return new ResponseResult<String>(404,"未找到该页面",null);
            else
                activity.setActivityStatus(SkActivityStatus.OPENING);
        }
        //====活动已开启：业务流程=====
        //返回一个带有其用户id编码后的流水号的下单地址: url=xxx/xx/userId
        String orderUrl = skActivityService.getOrderUrl(userId);
        return new ResponseResult<>(200,"下单入口开启",orderUrl);
    }








    //======== 接口：活动开关服务 ==========
    @PostMapping("/seckill/entrance/warmUp")
    public ResponseResult<Boolean> warmUp(@RequestBody SeckillActivity activity)
    {
        try {
            skActivityService.activityWarmUp(activity);
        } catch (Exception e) {
            return new ResponseResult<>(500,"entrance warmUp fail: "+e.getMessage(),false);
        }
        return new ResponseResult<>(200,"entrance warmUp success",true);
    }
    @PostMapping("/seckill/activity/open/{activityId}")
    public ResponseResult<Boolean> open(@PathVariable("activityId")String activityId)
    {
        try {
            skActivityService.activityOpen(activityId);
        } catch (Exception e) {
            return new ResponseResult(500,"seckill activity 开启失败"+e.getMessage(),false);
        }
        return new ResponseResult(200,"开启成功",true);
    }
    @PostMapping("/seckill/activity/close/{activityId}")
    public ResponseResult<Boolean> close(@PathVariable("activityId")String activityId)
    {
        try {
            skActivityService.activityClose(activityId);
        } catch (Exception e) {
            return new ResponseResult(500,"seckill activity 移除失败"+e.getMessage(),false);
        }
        return new ResponseResult(200,"移除成功",true);
    }
}
