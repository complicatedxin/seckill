package com.sauvignon.seckill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class EntranceService9000
{
    public static void main(String[] args)
    {
        SpringApplication.run(EntranceService9000.class);
    }
}
