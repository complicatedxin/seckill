package com.sauvignon.seckill.schedule;

import com.sauvignon.seckill.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DealSchedule
{
    @Autowired
    private StorageService storageService;
    @Autowired
    private ThreadPoolExecutor threadPool;
    @Autowired
    private DealTask dealTask;

    private ConcurrentHashMap<Long, AtomicInteger> dealAccMap =new ConcurrentHashMap<>();
    private ConcurrentHashMap<Long, Integer> accThresholdMap =new ConcurrentHashMap<>();

    public synchronized void init(Long commodityId,Integer count)
    {
        if(dealAccMap.get(commodityId)!=null)
        {
            acc(commodityId,count);
            return;
        }
        int threshold = storageService.findOne(commodityId).getTotal()*5/100;
        threshold = threshold==0?1:threshold;
        accThresholdMap.put(commodityId,threshold);
        dealAccMap.put(commodityId,new AtomicInteger(1));
    }
    public void acc(Long commodityId,Integer count)
    {
        AtomicInteger batch=dealAccMap.get(commodityId);
        if(batch==null)
        {
            init(commodityId, count);
            return;
        }
        synchronized (batch=dealAccMap.get(commodityId))
        {
            count = batch.addAndGet(count);
            if(count < accThresholdMap.get(commodityId))
                return;
            else
                dealAccMap.get(commodityId).compareAndSet(count,0);
        }
        threadPool.submit(dealTask.deal(commodityId,count));
    }

}
