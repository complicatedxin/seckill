package com.sauvignon.seckill.mapper;

import com.sauvignon.seckill.pojo.entities.OrderLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderLogMapper
{
    int insertOne(OrderLog orderLog);
}
