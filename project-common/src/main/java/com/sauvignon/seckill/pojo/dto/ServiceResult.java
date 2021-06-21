package com.sauvignon.seckill.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResult<E> implements Serializable
{
    private Integer code;
    private String  message;
    private E       body;

    public ServiceResult(Integer code,String message)
    {
        this.code=code;
        this.message=message;
    }
}
