package com.fly.platform.asset.vo;

import com.fly.platform.asset.entity.SiteBooking;
import lombok.Data;

import java.util.List;

/**
 * 场站时空防重预约冲突检测结果 VO
 *
 * @author SoarCloud Team
 */
@Data
public class BookingCheckResultVO {

    /** 是否存在时空冲突 (true-存在冲突不可预约，false-时空安全允许预约) */
    private Boolean hasConflict;

    /** 重叠占用预约数 */
    private Integer conflictCount;

    /** 场站总容量 */
    private Integer capacity;

    /** 校验结果描述提示信息 */
    private String message;

    /** 重叠冲突的预约记录列表 */
    private List<SiteBooking> conflictingBookings;
}
