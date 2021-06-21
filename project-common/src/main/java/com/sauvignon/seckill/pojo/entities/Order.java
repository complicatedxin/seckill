package com.sauvignon.seckill.pojo.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order
{
    private Long orderId;
    private Long commodityId;
    private Integer count;
    private Long userId;
    private BigDecimal amount;
    private Integer orderStatus;
}
