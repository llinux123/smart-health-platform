package com.smart.health.registration.util;

import com.smart.health.common.sequence.DistributedSequenceGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderSnGenerator 单元测试")
class OrderSnGeneratorTest {

    @Mock
    private DistributedSequenceGenerator sequenceGenerator;

    @InjectMocks
    private OrderSnGenerator orderSnGenerator;

    @Test
    @DisplayName("生成编号符合 REG_yyyyMMdd_XXXXXX 格式")
    void generate_matchesFormat() {
        when(sequenceGenerator.nextFormatted(anyString())).thenReturn("000001");

        String sn = orderSnGenerator.generate();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        assertThat(sn).matches("REG_\\d{8}_\\d{6}");
        assertThat(sn).startsWith("REG_" + today + "_");
        assertThat(sn).endsWith("000001");
    }

    @Test
    @DisplayName("DistributedSequenceGenerator 返回序号正确拼接")
    void generate_usesSequenceGenerator() {
        when(sequenceGenerator.nextFormatted(anyString())).thenReturn("000042");

        String sn = orderSnGenerator.generate();

        assertThat(sn).endsWith("000042");
    }

    @Test
    @DisplayName("不同序号产生不同订单号")
    void generate_differentSequences_produceDifferentOrders() {
        when(sequenceGenerator.nextFormatted(anyString()))
                .thenReturn("000001")
                .thenReturn("000002");

        String first = orderSnGenerator.generate();
        String second = orderSnGenerator.generate();

        assertThat(first).isNotEqualTo(second);
        assertThat(first).endsWith("000001");
        assertThat(second).endsWith("000002");
    }
}
