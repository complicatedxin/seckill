package com.sauvignon.seckill.service.impl;

import com.sauvignon.seckill.constants.Constants;
import com.sauvignon.seckill.constants.ServiceCode;
import com.sauvignon.seckill.mapper.CommodityMapper;
import com.sauvignon.seckill.pojo.dto.ServiceResult;
import com.sauvignon.seckill.pojo.entities.Commodity;
import com.sauvignon.seckill.service.StorageService;
import com.sauvignon.seckill.utils.RedisUtil;
import com.sauvignon.seckill.utils.ZkClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class StorageServiceImpl implements StorageService
{
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private CommodityMapper commodityMapper;

    @Override
    public ServiceResult increaseConsumed(Long commodityId, Integer count)
    {
        //1. 申请分布式锁
        CuratorFramework client = ZkClient.getClient();
        client.start();
        InterProcessMutex lock=
                new InterProcessMutex(client, Constants.storageManageLockPath(commodityId));
        try {
            //获取时间：3s：可能存在很多业务在这里执行增减，故尝试3s
            boolean acquire = lock.acquire(3, TimeUnit.SECONDS);
            if(acquire)
            {
                //2. 检查数量
                Commodity commodity = commodityMapper.findOne(commodityId);
                if(commodity.getDeal() >= commodity.getTotal())
                    return new ServiceResult(ServiceCode.FAIL,
                            "销售完毕",null);
                if(commodity.getConsumed() >= commodity.getTotal())
                    return new ServiceResult(ServiceCode.RETRY,
                            "订单堆积，请重试",null);
                //3. 改consumed字段
                commodityMapper.updateConsumed(commodityId,count);
                return new ServiceResult(ServiceCode.SUCCESS,
                        "修改成功",null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                lock.release();
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new ServiceResult(ServiceCode.RETRY, "获取锁失败，请重试",null);
    }

    @Override
    public ServiceResult decreaseConsumed(Long commodityId, Integer count)
    {
        //1. 申请分布式锁
        CuratorFramework client = ZkClient.getClient();
        client.start();
        InterProcessMutex lock=
                new InterProcessMutex(client, Constants.storageManageLockPath(commodityId));
        try {
            boolean acquire = lock.acquire(3, TimeUnit.SECONDS);
            if(acquire)
            {
                //2. 检查数量
                Commodity commodity = commodityMapper.findOne(commodityId);
                if(commodity.getConsumed() <= 0)
                    throw new IllegalStateException("consumed数量少于0，不能再减少！");
                //3. 改consumed字段
                commodityMapper.updateConsumed(commodityId,count);
                return new ServiceResult(ServiceCode.SUCCESS,
                        "修改成功",null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                lock.release();
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new ServiceResult(ServiceCode.RETRY,
                "分布式锁失败！未能decreaseConsumed",null);
    }

    @Override
    public Commodity findOne(Long commodityId)
    {
        return commodityMapper.findOne(commodityId);
    }

    @Override
    public ServiceResult<Integer> deal(Long commodityId, Integer count)
    {
        //redis mysql 双写一致：延时缓存双删
        redisUtil.del(Constants.seckillCommodity(commodityId));
        ServiceResult<Integer> serviceResult = increaseDeal(commodityId, count);
        //第二次删除睡眠时间：400ms，参见CacheProvider流程，其耗时292ms
        try {
            TimeUnit.MILLISECONDS.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        redisUtil.del(Constants.seckillCommodity(commodityId));
        return serviceResult;
    }
    public ServiceResult<Integer> increaseDeal(Long commodityId, Integer count)
    {
        //1. 申请分布式锁
        CuratorFramework client = ZkClient.getClient();
        client.start();
        InterProcessMutex lock=
                new InterProcessMutex(client, Constants.storageManageLockPath(commodityId));
        try {
            boolean acquire = lock.acquire(3, TimeUnit.SECONDS);
            if(acquire)
            {
                //2. 检查数量
                Commodity commodity = commodityMapper.findOne(commodityId);
                if(commodity.getDeal() >= commodity.getTotal()) //正常不会出现deal溢出
                    throw new IllegalAccessException("成交量溢出总量，无法消费！");
                //3. 改deal字段
                int i = commodityMapper.updateDeal(commodityId, count);
                return new ServiceResult<>(ServiceCode.SUCCESS, "修改成功",i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                lock.release();
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new ServiceResult<>(ServiceCode.RETRY,"未获取到锁",null);
    }
}
