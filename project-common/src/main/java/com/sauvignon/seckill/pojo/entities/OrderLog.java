package com.sauvignon.seckill.pojo.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Date;

@Data
@AllArgsConstructor
public class OrderLog
{
    private Long orderId;
    private Long commodityId;
    private Integer count;
    private Long userId;
    private Date recordTime;

    public OrderLog(Long orderId,Long commodityId,Integer count,Long userId)
    {
        this.orderId=orderId;
        this.commodityId=commodityId;
        this.count=count;
        this.userId=userId;
    }
}
