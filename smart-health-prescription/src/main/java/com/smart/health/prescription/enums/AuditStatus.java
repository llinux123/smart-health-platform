package com.smart.health.prescription.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 药师审核状态
 */
@Getter
public enum AuditStatus {

    PENDING(0, "待审核"),
    APPROVED(1, "审核通过"),
    REJECTED(2, "驳回");

    @JsonValue
    private final int code;
    private final String description;

    AuditStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static AuditStatus fromCode(int code) {
        for (AuditStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Invalid AuditStatus code: " + code);
    }
}
