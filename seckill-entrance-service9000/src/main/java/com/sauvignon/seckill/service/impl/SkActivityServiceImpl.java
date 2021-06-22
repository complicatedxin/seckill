package com.sauvignon.seckill.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.sauvignon.seckill.constants.Constants;
import com.sauvignon.seckill.mapper.SkActivityMapper;
import com.sauvignon.seckill.pojo.entities.Commodity;
import com.sauvignon.seckill.pojo.entities.SeckillActivity;
import com.sauvignon.seckill.service.SkActivityService;
import com.sauvignon.seckill.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SkActivityServiceImpl implements SkActivityService
{
    private static final ConcurrentHashMap<String,SeckillActivity> skActivityMap=
            new ConcurrentHashMap<>(64);

    //TODO: 测试
    @Autowired
    private RedisUtil redisUtil;

    {
        skActivityMap.put("test",
                new SeckillActivity("test",
                        1L,new Date(),1,null));
    }


    public SeckillActivity getActivity(String activityId)
    {
        return skActivityMap.get(activityId);
    }

    @Autowired
    private SkActivityMapper skActivityMapper;

    @Override
    public void activityWarmUp(SeckillActivity activity)
    {
        skActivityMap.put(activity.getActivityId(),activity);
    }

    @Override
    public void activityOpen(String activityId)
    {
        skActivityMap.get(activityId).setActivityStatus(1);
    }

    @Override
    public void activityClose(String activityId)
    {
        skActivityMap.remove(activityId);
    }

    //todo
    @Override
    public String getOrderUrl(Long userId)
    {
        String userIdHex= DigestUtil.md5Hex(String.valueOf(userId));
        return Constants.orderUrl(userIdHex)+"/"+userId;
    }
}
