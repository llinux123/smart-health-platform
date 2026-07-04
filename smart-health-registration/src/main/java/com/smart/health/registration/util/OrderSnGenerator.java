package com.smart.health.registration.util;

import com.smart.health.common.constant.CommonConstants;
import com.smart.health.common.sequence.DistributedSequenceGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 挂号订单号生成器
 * 格式: REG_yyyyMMdd_XXXXXX（6位自增序号，每日重置，基于 Redis INCR 分布式安全）
 */
@Component
@RequiredArgsConstructor
public class OrderSnGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DistributedSequenceGenerator sequenceGenerator;

    /**
     * 生成唯一订单号（基于 Redis INCR，分布式安全）
     * 格式: REG_yyyyMMdd_XXXXXX
     *
     * @return 唯一订单号
     */
    public String generate() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String seq = sequenceGenerator.nextFormatted(CommonConstants.ORDER_SN_COUNTER_PREFIX);
        return CommonConstants.ORDER_SN_PREFIX + today + "_" + seq;
    }
}
