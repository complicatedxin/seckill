package com.sauvignon.seckill.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class PoolConfig
{
    @Bean
    public ThreadPoolExecutor threadPool()
    {
        return new ThreadPoolExecutor(1,4,
                60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
    }
}
