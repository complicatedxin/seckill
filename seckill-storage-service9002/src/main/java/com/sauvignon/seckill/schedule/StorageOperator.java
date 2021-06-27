package com.sauvignon.seckill.schedule;

import com.sauvignon.seckill.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class StorageOperator
{
    @Autowired
    private DealTask dealTask;
    @Autowired
    @Qualifier("dealAccExecutor")
    private ThreadPoolExecutor dealExecutor;

    private Map<Long,Integer> dealAccMap=new HashMap<>(128);


    public void acc(Long commodityId,Integer count)
    {
        dealExecutor.submit(dealTask.accTask(dealAccMap,commodityId,count));
    }

    @Scheduled(cron = "*/5 * * * * ?")
    public void batchDeal()
    {
        dealExecutor.submit(dealTask.submitTask(this));
    }





    public Map<Long,Integer> getDealAccMap()
    {
        return dealAccMap;
    }
    public void setDealAccMap(Map<Long,Integer> map)
    {
        this.dealAccMap=map;
    }
}
