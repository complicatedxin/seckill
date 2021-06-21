package com.sauvignon.seckill.controller;

import com.sauvignon.seckill.constants.ResponseCode;
import com.sauvignon.seckill.constants.ServiceCode;
import com.sauvignon.seckill.pojo.dto.ResponseResult;
import com.sauvignon.seckill.pojo.dto.ServiceResult;
import com.sauvignon.seckill.pojo.entities.Commodity;
import com.sauvignon.seckill.pojo.entities.Order;
import com.sauvignon.seckill.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class OrderController
{
    @Autowired
    private OrderService orderService;

    @PostMapping("/order/find/one/{orderId}")
    public ResponseResult<Order> findOne(@PathVariable("orderId") Long orderId)
    {
        if(orderId==null || orderId <=0 )
            return new ResponseResult<>(ResponseCode.ILLEGAL_ARGUMENT,"订单id必须>0",null);
        Order order = orderService.findOne(orderId);
        return new ResponseResult<>(ResponseCode.SUCCESS,"success",order);
    }

    @PostMapping("/order/update/status/{orderId}/{status}")
    public ResponseResult<Integer> updateStatus(@PathVariable("orderId") Long orderId,
                                                @PathVariable("status") Integer status)
    {
        if(orderId==null || orderId <=0 || status==null || status<0)
            return new ResponseResult<>(ResponseCode.ILLEGAL_ARGUMENT,"参数有误",null);
        ServiceResult<Integer> serviceResult = orderService.updateStatus(orderId, status);
        return new ResponseResult<>(ResponseCode.SUCCESS,"success",serviceResult.getBody());
    }

    @PostMapping("/order/payment/pre/check/{orderId}/{orderFlag}")
    public ServiceResult<Integer> paymentPreCheck(@PathVariable("orderId")Long orderId,
                                                  @PathVariable("orderFlag")String orderFlag)
    {
        return orderService.paymentPreCheck(orderId, orderFlag);
    }

    @PostMapping("/order/payment/pre/check/{orderId}")
    public ResponseResult<Order> paymentPostCheck(@PathVariable("orderId")Long orderId)
    {
        ServiceResult<Order> serviceResult = orderService.paymentPostCheck(orderId);
        Integer code = serviceResult.getCode();
        int retires=2;
        while(code.equals(ServiceCode.RETRY) && retires-->0)
        {
            serviceResult=orderService.paymentPostCheck(orderId);
            code=serviceResult.getCode();
        }
        if(serviceResult.getCode().equals(ServiceCode.SUCCESS))
            return new ResponseResult<>(ResponseCode.SUCCESS,"订单状态无异常",serviceResult.getBody());
        else
            return new ResponseResult<>(ResponseCode.REQUEST_FAIL,"订单已结束或异常，退款",serviceResult.getBody());
    }

}
