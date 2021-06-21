package com.sauvignon.seckill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class StorageService9002
{
    public static void main(String[] args)
    {
        SpringApplication.run(StorageService9002.class);
    }
}
