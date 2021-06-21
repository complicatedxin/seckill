package com.sauvignon.seckill.service;

import com.sauvignon.seckill.pojo.dto.ServiceResult;

public interface PaymentAspectService
{
    /**
     * @return (serviceCode,msg,OrderStatus before update)
     */
    ServiceResult<Integer> paymentPreCheck(Long orderId, String orderFlag);
    ServiceResult<Long> paymentDeal(Long orderId);
}
