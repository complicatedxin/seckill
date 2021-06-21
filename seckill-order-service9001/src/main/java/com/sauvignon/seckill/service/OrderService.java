package com.sauvignon.seckill.service;

import com.sauvignon.seckill.pojo.dto.ServiceResult;
import com.sauvignon.seckill.pojo.entities.Order;

public interface OrderService
{
    void handleOrder(Order order);
    boolean checkOrderOvertime(String orderFlagKey);
    ServiceResult abandonOvertimeOrder(String orderFlagKey);

    Order findOne(Long orderId);

    ServiceResult addOne(Order order);

    ServiceResult<Integer> updateStatus(Long orderId, Integer orderStatus);

    ServiceResult<Integer> paymentPreCheck(Long orderId,String orderFlag);
    ServiceResult<Order> paymentPostCheck(Long orderId);
}
