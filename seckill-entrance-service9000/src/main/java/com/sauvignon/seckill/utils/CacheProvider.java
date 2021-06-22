package com.sauvignon.seckill.utils;

import com.sauvignon.seckill.constants.Constants;
import com.sauvignon.seckill.pojo.dto.ResponseResult;
import com.sauvignon.seckill.pojo.entities.Commodity;
import com.sauvignon.seckill.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CacheProvider
{
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private StorageService storageService;

    public Commodity getCommodity(Long commodityId)
    {
        //1. 从缓存拿
        Commodity commodity=(Commodity)redisUtil.get(Constants.seckillCommodity(commodityId));
        if(commodity!=null)
            return commodity;
        //2. 缓存拿不到，从数据库拿
        ResponseResult<Commodity> responseResult = storageService.findOne(commodityId);
        commodity=responseResult.getBody();
        //3. 更新缓存
        if(commodity!=null)
            redisUtil.set(Constants.seckillCommodity(commodityId),commodity);

        return commodity;
    }
}
