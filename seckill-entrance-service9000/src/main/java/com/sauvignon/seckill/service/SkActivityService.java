package com.sauvignon.seckill.service;

import com.sauvignon.seckill.pojo.entities.SeckillActivity;

import java.util.Date;

public interface SkActivityService
{
    SeckillActivity getActivity(String activityId);
    void activityWarmUp(SeckillActivity seckillActivity);
    void activityOpen(String activityId);
    void activityClose(String activityId);
    String getOrderUrl(Long userId);
}
