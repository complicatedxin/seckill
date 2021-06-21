package com.sauvignon.seckill.mapper;

import com.sauvignon.seckill.pojo.entities.Commodity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommodityMapper
{
    Commodity findOne(@Param("commodityId") Long commodityId);
    int updateConsumed(@Param("commodityId") Long commodityId,@Param("num")Integer num);
    int updateDeal(@Param("commodityId") Long commodityId,@Param("num")Integer num);
}
