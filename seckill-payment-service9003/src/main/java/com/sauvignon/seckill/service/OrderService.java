package com.sauvignon.seckill.service;

import com.sauvignon.seckill.pojo.dto.ResponseResult;
import com.sauvignon.seckill.pojo.dto.ServiceResult;
import com.sauvignon.seckill.pojo.entities.Order;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(value = "${payment-aspect.service.orderService}")
public interface OrderService
{
    @PostMapping("/order/find/one/{orderId}")
    ResponseResult<Order> findOne(@PathVariable("orderId") Long orderId);

    @PostMapping("/order/payment/pre/check/{orderId}/{orderFlag}")
    ServiceResult<Integer> paymentPreCheck(@PathVariable("orderId")Long orderId,
                                           @PathVariable("orderFlag")String orderFlag);

    @PostMapping("/order/payment/pre/check/{orderId}")
    ResponseResult<Order> paymentPostCheck(@PathVariable("orderId")Long orderId);
}
