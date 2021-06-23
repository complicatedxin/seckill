package com.sauvignon.seckill.controller;

import cn.hutool.json.JSONObject;
import com.sauvignon.seckill.constants.Constants;
import com.sauvignon.seckill.pojo.dto.ResponseResult;
import com.sauvignon.seckill.pojo.entities.Commodity;
import com.sauvignon.seckill.utils.CacheProvider;
import com.sauvignon.seckill.utils.JwtUtil;
import com.sauvignon.seckill.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.sql.Time;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class TestProcess
{
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private CacheProvider cacheProvider;
    @Autowired
    private RedisUtil redisUtil;

    private String entranceUrl="http://localhost:9000/seckill/getEntrance/test/";
    private String paymentCallbackUrl ="http://localhost:9003/seckill/payment/ali/callback?orderId={orderId}";

    @RequestMapping("/ping")
    public String ping()
    {
        return "pong";
    }

    @RequestMapping("/begin")
    public ResponseResult begin() throws InterruptedException
    {
        //随机userId
        int id=new Random().nextInt(10000);
        System.out.println(id);
        Long userId=new Integer(id).longValue();
//        HttpHeaders header=new HttpHeaders();
//        header.setContentType(MediaType.APPLICATION_JSON);
//        header.set("Accept", "application/json");
//        header.add("Authorization",jwtToken);
//        JSONObject param=new JSONObject();
//        param.set("activityId","test");
//        HttpEntity httpEntity=new HttpEntity(param,header);

        //1. 请求下单地址
//        ResponseEntity<ResponseResult> responseEntity = restTemplate.exchange(entranceUrl, HttpMethod.GET,httpEntity,ResponseResult.class);
        ResponseResult responseResult = restTemplate.postForObject(entranceUrl + userId, null, ResponseResult.class);
//        ResponseResult responseResult = responseEntity.getBody();

        String orderUrl = (String) responseResult.getBody();
        if(orderUrl==null)
            return new ResponseResult(404,"活动结束",null);
        orderUrl+="?activityId=test";

        TimeUnit.MILLISECONDS.sleep(200);

        //2. 下单
        responseResult = restTemplate.postForObject(orderUrl, null, ResponseResult.class);
        String paymentUrl= (String) responseResult.getBody();
        System.out.println(paymentUrl);
        if(paymentUrl==null)
            return new ResponseResult(404,"id repeat",null);

        TimeUnit.MILLISECONDS.sleep(2000);

        //3. 支付
        responseResult=restTemplate.getForObject(paymentUrl,ResponseResult.class);
        Long orderId = (Long) responseResult.getBody();
        if(orderId==null)
            return new ResponseResult(404,"订单关闭或需重新支付",null);

        TimeUnit.MILLISECONDS.sleep(2000);

        //4. 支付回调
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("orderId", orderId);
        restTemplate.getForObject(paymentCallbackUrl,Void.class,parameters);

        return new ResponseResult(200,"success",orderId);
    }



    @RequestMapping("/multi")
    public void multiOrder() throws InterruptedException
    {
        for(int i=0;i<50;i++)
        {
            new Thread(()->{
                for(int j=0;j<10;j++)
                {
                    try {
                        this.begin();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
//                    int id=new Random().nextInt(10000);
//
//                    System.out.println(id);
//
//                    Long userId=new Integer(id).longValue();
//                    Map<String,Object> map=new HashMap<>();
//                    map.put("userId",userId);
//                    String token = JwtUtil.generateToken(map, Calendar.HOUR, 24 * 7);

//                    this.begin(token);

                    try {
                        TimeUnit.MILLISECONDS.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }).start();

            TimeUnit.MILLISECONDS.sleep(200);
        }
    }

    @RequestMapping("/spy")
    public Commodity spyOn()
    {
        return cacheProvider.getCommodity(1L);
    }

    private AtomicInteger e=new AtomicInteger(0);
    //模拟数据
    @RequestMapping("/stuff")
    public void stuffData()
    {
        boolean b = e.compareAndSet(0, 1);
        if(!b) return;
        int totalNum=400;
        System.out.println("===初始化===");
        Commodity commodity=new Commodity(1L,new BigDecimal(0.99),totalNum,0,0);
        redisUtil.set(Constants.seckillCommodity(1L),commodity);
        for(int i=0;i<totalNum;i++)
            System.out.println(redisUtil.lPush(Constants.consumedRedisKey(1L), i + 1));
        System.out.println("=====初始化完成=====");
    }

}
