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

}
