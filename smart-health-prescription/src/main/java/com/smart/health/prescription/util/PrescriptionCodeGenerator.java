package com.smart.health.prescription.util;

import com.smart.health.common.constant.CommonConstants;
import com.smart.health.common.sequence.DistributedSequenceGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 处方编号生成器
 * 格式: RX_yyyyMMdd_hospitalId_XXXXXX（6位自增序号，每日重置，基于 Redis INCR 分布式安全）
 */
@Component
@RequiredArgsConstructor
public class PrescriptionCodeGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DistributedSequenceGenerator sequenceGenerator;

    /**
     * 生成唯一处方编号（使用默认医院ID）
     * 格式: RX_yyyyMMdd_001_XXXXXX
     *
     * @return 唯一处方编号
     */
    public String generate() {
        return generate(CommonConstants.DEFAULT_HOSPITAL_ID);
    }

    /**
     * 生成唯一处方编号（指定医院ID）
     * 格式: RX_yyyyMMdd_hospitalId_XXXXXX
     *
     * @param hospitalId 院区/医院ID
     * @return 唯一处方编号
     */
    public String generate(String hospitalId) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String seq = sequenceGenerator.nextFormatted(CommonConstants.RX_SN_COUNTER_PREFIX);
        return CommonConstants.RX_SN_PREFIX + today + "_" + hospitalId + "_" + seq;
    }
}
