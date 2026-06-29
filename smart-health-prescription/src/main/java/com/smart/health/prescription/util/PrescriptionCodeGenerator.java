package com.smart.health.prescription.util;

import com.smart.health.common.constant.CommonConstants;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 处方编号生成器
 * 格式: RX_yyyyMMdd_hospitalId_XXXXXX（6位自增序号，每日重置）
 */
public final class PrescriptionCodeGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);
    private static volatile String CURRENT_DATE = LocalDate.now().format(DATE_FORMATTER);

    private PrescriptionCodeGenerator() {}

    /**
     * 生成唯一处方编号（使用默认医院ID）
     * 格式: RX_yyyyMMdd_XXXXXX
     *
     * @return 唯一处方编号
     */
    public static synchronized String generate() {
        return generate(CommonConstants.DEFAULT_HOSPITAL_ID);
    }

    /**
     * 生成唯一处方编号（指定医院ID）
     * 格式: RX_yyyyMMdd_hospitalId_XXXXXX
     *
     * @param hospitalId 院区/医院ID
     * @return 唯一处方编号
     */
    public static synchronized String generate(String hospitalId) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        if (!today.equals(CURRENT_DATE)) {
            CURRENT_DATE = today;
            SEQUENCE.set(0);
        }
        int seq = SEQUENCE.incrementAndGet();
        return CommonConstants.RX_SN_PREFIX + today + "_" + hospitalId + "_" + String.format("%06d", seq);
    }
}
