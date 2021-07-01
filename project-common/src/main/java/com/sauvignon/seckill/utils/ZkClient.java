package com.sauvignon.seckill.utils;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.apache.curator.retry.ExponentialBackoffRetry;

public class ZkClient
{
    private static String zkUrl="xxx:xxx,xxx:xxx,xxx:xxx";

    public static CuratorFramework getClient()
    {
        CuratorFramework client;
        RetryPolicy retryPolicy=new BoundedExponentialBackoffRetry(500,10*1000,10);

        client= CuratorFrameworkFactory.builder()
                .connectString(zkUrl)
                .sessionTimeoutMs(60*1000)
                .connectionTimeoutMs(10*1000)
                .retryPolicy(retryPolicy)
                .namespace("seckill-project")
                .build();

        return client;
    }
}
