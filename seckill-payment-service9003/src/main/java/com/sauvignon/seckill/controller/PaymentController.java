package com.sauvignon.seckill.controller;

import com.sauvignon.seckill.constants.Constants;
import com.sauvignon.seckill.constants.OrderStatus;
import com.sauvignon.seckill.constants.ResponseCode;
import com.sauvignon.seckill.constants.ServiceCode;
import com.sauvignon.seckill.pojo.dto.ResponseResult;
import com.sauvignon.seckill.pojo.dto.ServiceResult;
import com.sauvignon.seckill.pojo.entities.Order;
import com.sauvignon.seckill.service.PaymentAspectService;
import com.sauvignon.seckill.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController
{
    @Value("${server.port}")
    private String serverPort;

    @Autowired
    private PaymentAspectService paymentAspectService;
    @Autowired
    private RedisUtil redisUtil;

    @RequestMapping("/seckill/paymentHost/{orderId}")
    public ResponseResult<Long> paymentHost(@PathVariable("orderId")Long orderId,
                                      @RequestParam("userId")Long userId,
                                      @RequestParam("commodityId")Long commodityId)
    {
        //0. 验证参数
        String orderFlag = Constants.seckillOrderFlag(userId, commodityId);
        Long oId = (Long) redisUtil.get(orderFlag);
        if(oId==null || !oId.equals(orderId))
            return new ResponseResult(ResponseCode.NO_RESPONSE,"订单失效",null);
        //1. 验证订单状态
        ServiceResult<Integer> serviceResult = paymentAspectService.paymentPreCheck(orderId,orderFlag);
        Integer code = serviceResult.getCode();
        int reties=2;
        while(code.equals(ServiceCode.RETRY) && reties-->0)
        {
            serviceResult=paymentAspectService.paymentPreCheck(orderId,orderFlag);
            code=serviceResult.getCode();
        }
        if(code.equals(ServiceCode.SUCCESS))
            //2. TODO: 调用支付接口返回付款二维码
            return new ResponseResult<>(ResponseCode.SUCCESS,"请扫码支付",orderId);
        else if(code.equals(ServiceCode.FAIL))//失败
        {
            if(serviceResult.getBody().equals(OrderStatus.PRE_PURCHASE))
                //之前开启过支付，但再次付款不接受（现在是秒杀！）
                return new ResponseResult(ResponseCode.REQUEST_FAIL,"重试请求付款，不接受！",null);
            else
                return new ResponseResult(ResponseCode.REQUEST_FAIL,"订单失效！",null);
        }
        else //未获取到锁
            return new ResponseResult<>(ResponseCode.RETRY,"请重试",null);
    }

    @RequestMapping("/seckill/payment/ali/callback")
    public void paymentAliCallback(@RequestParam("orderId")Long orderId)
    {
        ServiceResult<Long> serviceResult = paymentAspectService.paymentDeal(orderId);

        if(serviceResult.getCode().equals(ServiceCode.FAIL))
        {
            System.out.println("日你妈！退钱");
            //TODO: 发消息给mq退钱。在秒杀时，这个业务可以降级
        }
    }
}
