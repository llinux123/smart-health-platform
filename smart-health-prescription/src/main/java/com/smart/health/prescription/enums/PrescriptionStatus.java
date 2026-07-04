package com.smart.health.prescription.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 处方流转状态
 */
@Getter
public enum PrescriptionStatus {

    NOT_DISPENSED(0, "未配药"),
    DISPENSING(1, "配药中"),
    DISPENSED(2, "已发药");

    @JsonValue
    private final int code;
    private final String description;

    PrescriptionStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static PrescriptionStatus fromCode(int code) {
        for (PrescriptionStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Invalid PrescriptionStatus code: " + code);
    }
}
