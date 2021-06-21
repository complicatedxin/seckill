package com.sauvignon.seckill.pojo.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillActivity
{
    private String activityId;
    private Long commodityId;
    private Date openTime;
    private Integer activityStatus;
    private Date deadTime;
}
