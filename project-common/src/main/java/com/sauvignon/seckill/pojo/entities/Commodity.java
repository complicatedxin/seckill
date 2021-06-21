package com.sauvignon.seckill.pojo.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Commodity
{
    private Long commodityId;
    private BigDecimal price;
    private Integer total;
    private Integer consumed;
    private Integer deal;
}
