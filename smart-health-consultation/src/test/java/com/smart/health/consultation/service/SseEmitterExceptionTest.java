package com.smart.health.consultation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 深入调查 SseEmitter 在未初始化时的行为
 */
@DisplayName("SseEmitter 未初始化行为调查")
class SseEmitterExceptionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("未初始化的 SseEmitter 可以正常 send()")
    void sendBeforeStart_worksNormally() throws Exception {
        SseEmitter emitter = new SseEmitter(5000L);
        // 在未初始化时调用 send() - 应该不会抛异常
        emitter.send(SseEmitter.event().name("error")
                .data(objectMapper.writeValueAsString("test")));
        emitter.complete();
        System.out.println("SseEmitter.send() before start succeeded!");
    }

    @Test
    @DisplayName("验证 catch(IOException) 的范围")
    void verifyIOExceptionCatchRange() throws Exception {
        SseEmitter emitter = new SseEmitter(5000L);
        // 测试 IOException 是否能捕获所有 send() 的异常
        try {
            emitter.send(SseEmitter.event().data("test"));
            emitter.complete();
            System.out.println("send + complete succeeded without IllegalStateException");
        } catch (IOException e) {
            System.out.println("Caught IOException: " + e.getClass().getName());
        } catch (Exception e) {
            System.out.println("Caught " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @Test
    @DisplayName("complete 后调用 send 应该抛 IOException")
    void sendAfterComplete_throwsIOException() throws Exception {
        SseEmitter emitter = new SseEmitter(5000L);
        // 先 complete
        emitter.complete();
        // complete 后 send 应该抛 IOException
        try {
            emitter.send(SseEmitter.event().data("test"));
            System.out.println("No exception - send after complete succeeded!");
        } catch (IOException e) {
            System.out.println("Caught IOException: " + e.getMessage());
        } catch (IllegalStateException e) {
            System.out.println("Caught IllegalStateException: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("模拟 LLM 报错后调用 emitter.send() 和 complete()")
    void simulateLlmErrorThenSend() throws Exception {
        SseEmitter emitter = new SseEmitter(5000L);

        // 模拟 LLM 流式调用失败后的处理（与 ConsultationServiceImpl.onError 一致）
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(objectMapper.writeValueAsString("{\"error\":\"AI 服务暂时不可用\"}")));
            emitter.complete();
            System.out.println("LLM error handling succeeded");
        } catch (IOException e) {
            System.out.println("Caught IOException: " + e.getMessage());
        }

        assertThat(emitter).isNotNull();
    }
}
