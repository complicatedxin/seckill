create database seckillproject;
use seckillproject;

create table user(
	user_id BIGINT(11) NOT NULL AUTO_INCREMENT PRIMARY KEY
);
create table sk_order(
	order_id BIGINT(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    commodity_id bigint(11) NOT null,
    count INT(11) not NULL COMMENT '购买商品数量',
    user_id BIGINT(11) NOT NULL COMMENT '用户id',
    amount DECIMAL(11,0) DEFAULT NULL COMMENT '总价',
    order_status INT(4) not NULL COMMENT '订单状态：0预创建，1创建完成，
				2预消费，3消费完成，4消费失败，
				5预支付，6支付完成，7支付失败（超时），
                19创建失败，8订单结束，
                9等待退款，10已退款...;'
);
create table sk_commodity(
	commod_id BIGINT(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    total INT(11) DEFAULT NULL COMMENT '总量',
    consumed INT(11) DEFAULT NULL COMMENT '已消费数量（含未支付）',
    deal INT(11) DEFAULT null COMMENT '交易量（已支付）'
);
create table sk_activity(
	activ_id varchar(32) not null primary key,
    commod_id BIGINT(11) default NULL,
    open_time datetime default null,
    activ_status int(4) default null comment '0未开启，1已开启，2已结束...',
    dead_time datetime default null comment 'null商品秒杀完关闭，not null到时间关闭'
);

#seata：undo_log
CREATE TABLE `undo_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'increment id',
  `branch_id` bigint(20) NOT NULL COMMENT 'branch transaction id',
  `xid` varchar(100) NOT NULL COMMENT 'global transaction id',
  `context` varchar(128) NOT NULL COMMENT 'undo_log context,such as serialization',
  `rollback_info` longblob NOT NULL COMMENT 'rollback info',
  `log_status` int(11) NOT NULL COMMENT '0:normal status,1:defense status',
  `log_created` datetime NOT NULL COMMENT 'create datetime',
  `log_modified` datetime NOT NULL COMMENT 'modify datetime',
  `ext` varchar(100) DEFAULT NULL COMMENT 'reserved field',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='AT transaction mode undo table';