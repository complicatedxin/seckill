package com.sauvignon.seckill.mapper;

import com.sauvignon.seckill.pojo.entities.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderMapper
{
    Order findOne(@Param("orderId") Long orderId);
    int addOne(Order order);
    int updateStatus(@Param("orderId") Long orderId,
                     @Param("orderStatus")Integer orderStatus);
}
