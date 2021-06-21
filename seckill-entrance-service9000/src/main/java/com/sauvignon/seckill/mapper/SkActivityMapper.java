package com.sauvignon.seckill.mapper;

import com.sauvignon.seckill.pojo.entities.SeckillActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SkActivityMapper
{
    SeckillActivity findOne(@Param("activityId")String activityId);

    int updateStatus(@Param("activityId")String activityId,
                     @Param("activityStatus") Integer activityStatus);

    int addOne(SeckillActivity activity);

}
