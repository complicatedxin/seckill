package com.sauvignon.seckill.service;

import com.sauvignon.seckill.pojo.dto.ServiceResult;
import com.sauvignon.seckill.pojo.entities.Commodity;

public interface StorageService
{
    ServiceResult increaseConsumed(Long commodityId, Integer count);
    ServiceResult decreaseConsumed(Long commodityId, Integer count);
    Commodity findOne(Long commodityId);
    ServiceResult<Integer> deal(Long commodityId, Integer count);
}
