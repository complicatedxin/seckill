package com.sauvignon.seckill.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import java.net.UnknownHostException;

@Configuration
public class RedisConfig
{
    @Bean("redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) throws UnknownHostException
    {
        RedisTemplate<String, Object> template = new RedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        //自定义序列化配置
        Jackson2JsonRedisSerializer<Object> jacksonSerializer=new Jackson2JsonRedisSerializer<Object>(Object.class);
        ObjectMapper objectMapper=new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jacksonSerializer.setObjectMapper(objectMapper);
        //String方式的序列化
        StringRedisSerializer stringSerializer=new StringRedisSerializer();

        //key采用String方式序列化
        template.setKeySerializer(stringSerializer);
        //value采用jackson方式序列化
        template.setValueSerializer(jacksonSerializer);

        //Hash的key采用String方式的序列化
        template.setHashKeySerializer(stringSerializer);
        //Hash的value采用jackson方式序列化
        template.setHashValueSerializer(jacksonSerializer);

        //将配置保存
        template.afterPropertiesSet();

        //自定义结束，返回自定义redisTemplate
        return template;
    }
}
