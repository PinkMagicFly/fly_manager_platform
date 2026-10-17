package com.fly.platform.common.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据库单据基础实体（包含审计三件套与逻辑删除）
 *
 * @author SoarCloud Team
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /** 记录最后更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;

    /** 逻辑删除标记：0-正常 1-已删除 */
    @TableLogic
    private Integer isDeleted;

    /** 初始化创建与更新审计信息 */
    public void initAudit() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isDeleted = 0;
    }

    /** 更新最后审计时间 */
    public void updateAudit() {
        this.updatedAt = LocalDateTime.now();
    }
}
