package com.sauvignon.seckill.service;

import com.sauvignon.seckill.pojo.dto.ResponseResult;
import com.sauvignon.seckill.pojo.entities.Commodity;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

//TODO: 服务降级
@FeignClient(value = "${order-submit-host.service.storageService}")
public interface StorageService
{
    @PostMapping("/storage/consumed/increase/{commodityId}/{count}")
    ResponseResult increaseConsumed(@PathVariable("commodityId")Long commodityId,
                                    @PathVariable("count")Integer count);

    @PostMapping("/storage/consumed/decrease/{commodityId}/{count}")
    ResponseResult decreaseConsumed(@PathVariable("commodityId")Long commodityId,
                                    @PathVariable("count")Integer count);

    @PostMapping("/storage/find/one/{commodityId}")
    ResponseResult<Commodity> findOne(@PathVariable("commodityId") Long commodityId);

    @PostMapping("/storage/deal/{commodityId}/{count}")
    ResponseResult deal(@PathVariable("commodityId") Long commodityId,
                               @PathVariable("count") Integer count);
}
