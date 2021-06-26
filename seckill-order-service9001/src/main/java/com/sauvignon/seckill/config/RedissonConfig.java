package com.sauvignon.seckill.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig
{
    @Value("${redisson.address}")
    private String address;
    @Value("${redisson.password}")
    private String password;

    @Bean
    public RedissonClient redissonClient()
    {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(address)
                .setPassword(password);

        return Redisson.create(config);
    }
}
