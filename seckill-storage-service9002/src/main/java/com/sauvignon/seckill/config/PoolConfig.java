package com.sauvignon.seckill.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class PoolConfig
{
    @Bean
    public ThreadPoolExecutor dealExecutor()
    {
        return new ThreadPoolExecutor(1,3,
                60, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(200),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    @Bean
    public ThreadPoolExecutor dealAccExecutor()
    {
        return new ThreadPoolExecutor(1,1,
                0, TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<>(300000),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
    }
}
