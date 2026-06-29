package com.smart.health.prescription.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("处方编号生成器测试")
class PrescriptionCodeGeneratorTest {

    @Test
    @DisplayName("默认生成编号符合 RX_yyyyMMdd_001_XXXXXX 格式")
    void generate_matchesExpectedFormat() {
        String code = PrescriptionCodeGenerator.generate();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        assertThat(code).startsWith("RX_" + today + "_001_");
        assertThat(code).matches("RX_\\d{8}_001_\\d{6}");
    }

    @Test
    @DisplayName("指定医院ID生成编号符合 RX_yyyyMMdd_hospitalId_XXXXXX 格式")
    void generateWithHospitalId_matchesExpectedFormat() {
        String code = PrescriptionCodeGenerator.generate("002");
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        assertThat(code).startsWith("RX_" + today + "_002_");
        assertThat(code).matches("RX_\\d{8}_002_\\d{6}");
    }

    @Test
    @DisplayName("连续生成不重复")
    void generate_consecutiveCalls_producesUniqueCodes() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            codes.add(PrescriptionCodeGenerator.generate());
        }
        assertThat(codes).hasSize(100);
    }

    @Test
    @DisplayName("序号递增")
    void generate_sequentialNumbers_incrementCorrectly() {
        String first = PrescriptionCodeGenerator.generate();
        String second = PrescriptionCodeGenerator.generate();

        // 提取序号部分（最后6位）
        String seq1 = first.substring(first.lastIndexOf('_') + 1);
        String seq2 = second.substring(second.lastIndexOf('_') + 1);

        assertThat(Integer.parseInt(seq2)).isEqualTo(Integer.parseInt(seq1) + 1);
    }
}
