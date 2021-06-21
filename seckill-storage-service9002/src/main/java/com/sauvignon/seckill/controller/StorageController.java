package com.sauvignon.seckill.controller;

import com.sauvignon.seckill.constants.ResponseCode;
import com.sauvignon.seckill.constants.ServiceCode;
import com.sauvignon.seckill.pojo.dto.ResponseResult;
import com.sauvignon.seckill.pojo.dto.ServiceResult;
import com.sauvignon.seckill.pojo.entities.Commodity;
import com.sauvignon.seckill.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StorageController
{
    @Value("${server.port}")
    private String serverPort;

    @Autowired
    private StorageService storageService;

    @PostMapping("/storage/consumed/increase/{commodityId}/{count}")
    public ResponseResult increaseConsumed(@PathVariable("commodityId")Long commodityId,
                                           @PathVariable("count")Integer count)
    {
        ServiceResult serviceResult = storageService.increaseConsumed(commodityId, count);
        //尝试
        int retries=2;
        while(serviceResult.getCode().equals(ServiceCode.RETRY) && retries-->0)
            serviceResult=storageService.increaseConsumed(commodityId, count);
        //重试两次后依旧不成功，当失败处理
        if(serviceResult.getCode().equals(ServiceCode.RETRY)
                || serviceResult.getCode().equals(ServiceCode.FAIL))
            return new ResponseResult(ResponseCode.ERROR,"fail",null);
        if(!serviceResult.getCode().equals(ServiceCode.SUCCESS))
            return new ResponseResult(404,"未知情况",null);
        //执行成功：
        return new ResponseResult(ResponseCode.SUCCESS,"success",null);
    }

    @PostMapping("/storage/consumed/decrease/{commodityId}/{count}")
    public ResponseResult decreaseConsumed(@PathVariable("commodityId")Long commodityId,
                                           @PathVariable("count")Integer count)
    {
        if(count >= 0)
            return new ResponseResult(500,"count需小于0",null);
        ServiceResult serviceResult = null;
        //尝试catch consumed=0异常
        try {
            serviceResult = storageService.decreaseConsumed(commodityId, count);
            int retries=2;
            while(serviceResult.getCode().equals(ServiceCode.RETRY) && retries-->0)
                serviceResult=storageService.decreaseConsumed(commodityId, count);
        } catch (Exception e) {
            return new ResponseResult(ResponseCode.ERROR,"consumed为0！不能减少");
        }
        if(!serviceResult.getCode().equals(ServiceCode.SUCCESS))
            return new ResponseResult(404,"未知情况",null);
        //执行成功：
        return new ResponseResult(ResponseCode.SUCCESS,"success",null);
    }

    @PostMapping("/storage/find/one/{commodityId}")
    public ResponseResult<Commodity> findOne(@PathVariable("commodityId") Long commodityId)
    {
        if(commodityId==null || commodityId <=0 )
            return new ResponseResult<>(ResponseCode.ILLEGAL_ARGUMENT,"商品id必须>0",null);
        Commodity commodity = storageService.findOne(commodityId);
        return new ResponseResult<>(ResponseCode.SUCCESS,"success",commodity);
    }

    @PostMapping("/storage/deal/{commodityId}/{count}")
    public ResponseResult deal(@PathVariable("commodityId") Long commodityId,
                                             @PathVariable("count") Integer count)
    {
        if(commodityId==null || commodityId <=0 || count==null || count<=0)
            return new ResponseResult(ResponseCode.ILLEGAL_ARGUMENT,"参数有误");
        //catch deal量溢出
        Integer code = null;
        try {
            code = storageService.deal(commodityId, count).getCode();
            int retires=1;
            while(code.equals(ServiceCode.RETRY) && retires-->0)
                code=storageService.deal(commodityId, count).getCode();
        } catch (Exception e) {
            return new ResponseResult(ResponseCode.ERROR,"成交量溢出总量，无法消费！");
        }
        if(!code.equals(ServiceCode.SUCCESS))
            return new ResponseResult(ResponseCode.ERROR,"所获取失败，无法deal！");
        return new ResponseResult(ResponseCode.SUCCESS,"success");
    }

}
