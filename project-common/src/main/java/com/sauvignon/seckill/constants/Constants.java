package com.sauvignon.seckill.constants;

public class Constants
{
    public static final String signature="!sau#c^1@_Z/z~+(p=skl";
    private static final String SECKILL_OPEN_TIME_="seckill_open_time:";
    private static final String PLACE_ORDER_PATH_ ="http://localhost:9000/seckill/order/host/";
    private static final String SECKILL_ORDER_FLAG__ ="seckill_order_flag:%s:%s";
    public static final String ORDER_MESSAGE_HASHKEY="orderId";
    private static final String STORAGE_MANAGE_LOCK_PATH_ ="/storage_manage_lock:";
    private static final String ORDER_STATUS_LOCK_PATH_ ="/order_status_lock:";
    private static final String PAYMENT_HOST_PATH_$userId$commodityId = "http://localhost:9003/seckill/paymentHost/";

    public static final Integer SECKILL_ORDER_TIME_RESTRICTION=60*30;
    public static final Integer SECKILL_ORDER_OVERTIME=SECKILL_ORDER_TIME_RESTRICTION-90;
    public static final Integer SECKILL_COMMODITY_COUNT=1;














    public static String seckillOpenTime(Long commodityId)
    {
        return SECKILL_OPEN_TIME_+commodityId;
    }
    public static String orderUrl(String userIdHex)
    {
        return PLACE_ORDER_PATH_ +userIdHex;
    }
    public static String seckillOrderFlag(Long userId,Long commodityId)
    {
        return String.format(SECKILL_ORDER_FLAG__, userId.toString(), commodityId.toString());
    }
    public static String orderStatusLockPath(Long orderId)
    {
        return ORDER_STATUS_LOCK_PATH_+orderId;
    }
    public static String storageManageLockPath(Long commodityId)
    {
        return STORAGE_MANAGE_LOCK_PATH_+commodityId;
    }
    public static String paymentHostPath(Long orderId,Long userId,Long commodityId)
    {
        return PAYMENT_HOST_PATH_$userId$commodityId+orderId+"?userId="+userId+"&commodityId="+commodityId;
    }
}
