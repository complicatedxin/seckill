package com.sauvignon.seckill.schedule;

import com.sauvignon.seckill.mapper.OrderLogMapper;
import com.sauvignon.seckill.pojo.entities.OrderLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.concurrent.Callable;

@Component
public class RecordOrderTask
{
    @Autowired
    private OrderLogMapper orderLogMapper;

    public Callable<Integer> record(OrderLog orderLog)
    {
         return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception
            {
                return orderLogMapper.insertOne(orderLog);
            }
        };
    }
}
