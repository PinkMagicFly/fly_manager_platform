-- ==============================================================
-- 翔云·低空智慧物流运营平台 常用地址簿与空管账号管理增量补丁
-- 包含：
-- 1. 客户常用地址簿表 customer_address
-- 2. 预置客户常用地址数据
-- ==============================================================

CREATE TABLE IF NOT EXISTS customer_address (
  address_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '地址唯一标识',
  customer_id BIGINT UNSIGNED NOT NULL COMMENT '归属客户账户ID',
  contact_name VARCHAR(64) NOT NULL COMMENT '联系人姓名',
  contact_phone VARCHAR(20) NOT NULL COMMENT '联系人手机号',
  province VARCHAR(40) NOT NULL COMMENT '所属省份',
  city VARCHAR(40) NOT NULL COMMENT '所属城市',
  district VARCHAR(40) NOT NULL COMMENT '所属区县',
  street VARCHAR(60) NOT NULL COMMENT '所属街道/乡镇',
  lng DECIMAL(10,6) NOT NULL COMMENT '地图选点经度',
  lat DECIMAL(10,6) NOT NULL COMMENT '地图选点纬度',
  detail VARCHAR(120) NOT NULL COMMENT '楼栋门牌详细地址',
  is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否为默认地址：0-否 1-是',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (address_id),
  KEY idx_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客户常用地址簿管理表';

-- 预置演示客户常用地址
INSERT INTO customer_address (address_id, customer_id, contact_name, contact_phone, province, city, district, street, lng, lat, detail, is_default)
VALUES
(1, 6, '陈总监', '13800000006', '广东省', '深圳市', '南山区', '粤海街道', 113.943000, 22.540000, '科技南十二路28号康佳研发大厦A座18层低空接驳站', 1),
(2, 6, '李经理', '13800000066', '广东省', '深圳市', '福田区', '莲花街道', 114.062000, 22.553000, '皇岗路5001号深业上城南区B座顶层停机坪', 0)
ON DUPLICATE KEY UPDATE contact_name = VALUES(contact_name), detail = VALUES(detail);
