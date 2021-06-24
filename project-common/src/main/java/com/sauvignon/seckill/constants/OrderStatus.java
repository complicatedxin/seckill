package com.sauvignon.seckill.constants;

public class OrderStatus
{
    public static final Integer PRE_CREATE=0;
    public static final Integer CREATED=1;
    /**
     *  CONSUME_FAIL or &lt 4
     */
    public static final Integer CREATE_FAIL=19;

    @Deprecated
    public static final Integer PRE_CONSUME=2;
    @Deprecated
    public static final Integer CONSUMED=3;
    @Deprecated
    public static final Integer CONSUME_FAIL=4;


    public static final Integer PRE_PURCHASE=5;
    public static final Integer PURCHASED=6;
    public static final Integer PURCHASE_FAIL=7;
    /**
     *  PURCHASED or PURCHASE_FAIL
     *  此状态只用作结果传输，与比较
     *  不能用作存储
     */
    @Deprecated
    public static final Integer FINISHED=8;


    public static final Integer WAIT_TO_REFUND =9;
    public static final Integer REFUNDED=10;




}
