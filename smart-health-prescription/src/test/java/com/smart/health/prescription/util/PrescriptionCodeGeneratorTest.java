package com.smart.health.prescription.util;

import com.smart.health.common.sequence.DistributedSequenceGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("处方编号生成器测试")
class PrescriptionCodeGeneratorTest {

    @Mock
    private DistributedSequenceGenerator sequenceGenerator;

    private PrescriptionCodeGenerator createGenerator() {
        return new PrescriptionCodeGenerator(sequenceGenerator);
    }

    @Test
    @DisplayName("默认生成编号符合 RX_yyyyMMdd_001_XXXXXX 格式")
    void generate_matchesExpectedFormat() {
        when(sequenceGenerator.nextFormatted(anyString())).thenReturn("000001");

        String code = createGenerator().generate();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        assertThat(code).startsWith("RX_" + today + "_001_");
        assertThat(code).matches("RX_\\d{8}_001_\\d{6}");
    }

    @Test
    @DisplayName("指定医院ID生成编号符合 RX_yyyyMMdd_hospitalId_XXXXXX 格式")
    void generateWithHospitalId_matchesExpectedFormat() {
        when(sequenceGenerator.nextFormatted(anyString())).thenReturn("000001");

        String code = createGenerator().generate("002");
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        assertThat(code).startsWith("RX_" + today + "_002_");
        assertThat(code).matches("RX_\\d{8}_002_\\d{6}");
    }

    @Test
    @DisplayName("序号来自 DistributedSequenceGenerator")
    void generate_usesSequenceGenerator() {
        when(sequenceGenerator.nextFormatted(anyString())).thenReturn("000042");

        String code = createGenerator().generate();

        assertThat(code).endsWith("000042");
    }

    @Test
    @DisplayName("不同序号不重复")
    void generate_differentSequences_producesUniqueCodes() {
        when(sequenceGenerator.nextFormatted(anyString()))
                .thenReturn("000001")
                .thenReturn("000002");

        PrescriptionCodeGenerator gen = createGenerator();
        String first = gen.generate();
        String second = gen.generate();

        assertThat(first).isNotEqualTo(second);
        assertThat(first).endsWith("000001");
        assertThat(second).endsWith("000002");
    }
}
