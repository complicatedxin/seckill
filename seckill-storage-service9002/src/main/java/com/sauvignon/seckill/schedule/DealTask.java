package com.sauvignon.seckill.schedule;

import com.sauvignon.seckill.pojo.dto.ServiceResult;
import com.sauvignon.seckill.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.concurrent.Callable;

@Component
public class DealTask
{
    @Autowired
    private StorageService storageService;

    public Callable<ServiceResult<Integer>> deal(long commodityId, int count)
    {
        return new Callable<ServiceResult<Integer>>() {
            @Override
            public ServiceResult<Integer> call() throws Exception
            {
                return storageService.deal(commodityId,count);
            }
        };
    }
}
