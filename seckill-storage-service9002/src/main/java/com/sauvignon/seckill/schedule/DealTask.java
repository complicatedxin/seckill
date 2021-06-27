package com.sauvignon.seckill.schedule;

import com.sauvignon.seckill.pojo.dto.ServiceResult;
import com.sauvignon.seckill.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class DealTask
{
    @Autowired
    @Qualifier("dealExecutor")
    private ThreadPoolExecutor dealExecutor;
    @Autowired
    private StorageService storageService;

    public Callable<ServiceResult<Integer>> dealTask(long commodityId, int count)
    {
        return new Callable<ServiceResult<Integer>>() {
            @Override
            public ServiceResult<Integer> call() throws Exception
            {
                return storageService.deal(commodityId,count);
            }
        };
    }

    public Callable<Integer> accTask(Map<Long,Integer> dealAccMap, long commodityId, int count)
    {
        return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception
            {
                Integer c = dealAccMap.get(commodityId);
                if(c==null) c=0;
                return dealAccMap.put(commodityId, c +count);
            }
        };
    }


    public Callable<Boolean> submitTask(StorageOperator operator)
    {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception
            {
                for(Map.Entry<Long,Integer> e:operator.getDealAccMap().entrySet())
                {
                    Long k = e.getKey();
                    Integer v = e.getValue();
                    if(k==null || v==null || v==0)
                        continue;
                    dealExecutor.submit(dealTask(k,v));
                }
                operator.setDealAccMap(new HashMap<>(128));
                return true;
            }
        };
    }
}
