package com.fly.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fly.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 资质证照文件附件实体
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cert_file")
public class CertFile extends BaseEntity {

    /** 证照附件ID */
    @TableId(type = IdType.AUTO)
    private Long certId;

    /** 主体类型：1-商户 2-无人机 3-机组 */
    private Integer ownerType;

    /** 主体ID */
    private Long ownerId;

    /** 证照类型 (如 LICENSE, AIR_CERT) */
    private String certType;

    /** 存储文件路径 */
    private String filePath;

    /** 文件哈希 */
    private String fileHash;

    /** 有效期截止 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expireAt;
}
