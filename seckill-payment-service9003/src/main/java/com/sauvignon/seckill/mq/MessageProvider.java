package com.sauvignon.seckill.mq;

import com.sauvignon.seckill.pojo.entities.Order;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MessageProvider
{
    //TODO: 对 callback error 降级处理

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    public void asyncSendOrderly(String topic,String tag,Object payload,String hashKey)
    {
        rocketMQTemplate.asyncSendOrderly(topic+":"+tag, payload, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult)
            { }
            @Override
            public void onException(Throwable e)
            {
                e.printStackTrace();
                log.info("error!"+payload);
            }
        },1000);
    }

    /**
     * 默认延迟 2 min
     * 底层使用异步发送
     * @param payload redis中订单延时标记的key
     */
    public String sendDelay(String topic,String tag,Object payload)
    {
        String msgReply = null;
        try {
            msgReply = rocketMQTemplate.sendAndReceive(topic + ":" + tag,
                    payload, String.class, 1000, 6);
        } catch (Exception e) {
            log.info("ERROR:"+e.getMessage()+":"+payload);
        }
        return msgReply;
    }
}
