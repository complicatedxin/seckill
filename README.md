### 环境

1. nacos

2. sentinel（可以不用，把pom与yml的配置干掉）

3. seata（1.4，其他版本不保证配置是否匹配）

4. redis

5. rocketmq

6. zookeeper

7. mysql

### 配置

> 根据你的喜好？改动yml里的配置

另外：在project-common项目中

+ utils.ZkClient：配置zookeepr地址

+ constants.Constants

    + signature：随便编个你喜欢的盐（不写也行）

    + 如果你不是单机部署：修改带ip:port的请求路径


### 数据库

> 导入seckillProject.sql
>
> 需要一条商品数据，参数在测试中需要（你看着插）

本项目未采取分库分表，如果你想，每个库都应存在一张undo_log表

### 测试

> 分步请求（废除：为了压测一些不必要的操作已忽略）

pre：本项目使用了jwt，需要请求时在header里添加Authorization=[token]（自己拿工具生成） 

1. 获取下单链接：seckill-entrance-service9000 controller.EntranceController

2. 下单请求：seckill-entrance-service9000 controller.OrderSubmitHostController

3. （订单处理与超时回退）：见 seckill-order-service9001 service.impl.OrderServiceImpl

4. 支付请求：seckill-payment-service9003 controller.PaymentController paymentHost()

5. （库存扣减）：见storage-service

6. 手动支付回调：seckill-payment-service9003 controller.PaymentController paymentCallback()

> 全链路测试（压测接口）

测试接口：entrance-service9000 controller.TestProcess begin()

不推荐使用multi()接口来压测
