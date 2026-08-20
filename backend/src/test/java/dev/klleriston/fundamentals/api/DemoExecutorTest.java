package dev.klleriston.fundamentals.api;

import dev.klleriston.fundamentals.core.Category;
import dev.klleriston.fundamentals.core.Demo;
import dev.klleriston.fundamentals.core.DemoInputException;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.core.ParameterSpec;
import dev.klleriston.fundamentals.trace.Trace;
import dev.klleriston.fundamentals.trace.TraceResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoExecutorTest {

    private static Demo demoRunning(Supplier<Trace> body) {
        return new Demo() {
            @Override public String id() { return "stub"; }
            @Override public String titleKey() { return "demo.stub.title"; }
            @Override public Category category() { return Category.ALGORITHMS; }
            @Override public String descriptionKey() { return "demo.stub.description"; }
            @Override public List<ParameterSpec> parameters() { return List.of(); }
            @Override public String displaySource() { return "code"; }
            @Override public Trace run(DemoParams params) { return body.get(); }
        };
    }

    private static Trace emptyTrace() {
        return new Trace("stub", "demo.stub.title", "code", List.of(), new TraceResult(null, 0, false));
    }

    @Test
    void returnsTheTraceWhenTheDemoFinishesInTime() {
        DemoExecutor executor = new DemoExecutor(Duration.ofSeconds(5));

        Trace trace = executor.run(demoRunning(DemoExecutorTest::emptyTrace), DemoParams.of(Map.of(), List.of()));

        assertThat(trace.demoId()).isEqualTo("stub");
    }

    @Test
    void abortsADemoThatRunsTooLong() {
        DemoExecutor executor = new DemoExecutor(Duration.ofMillis(100));

        Demo slow = demoRunning(() -> {
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            return emptyTrace();
        });

        assertThatThrownBy(() -> executor.run(slow, DemoParams.of(Map.of(), List.of())))
                .isInstanceOf(DemoTimeoutException.class)
                .hasMessage("error.executionTimeout");
    }

    @Test
    void rethrowsDemoExceptionsUnchanged() {
        DemoExecutor executor = new DemoExecutor(Duration.ofSeconds(5));

        Demo failing = demoRunning(() -> {
            throw new DemoInputException("array", "error.arrayNotSorted", Map.of("index", 1));
        });

        assertThatThrownBy(() -> executor.run(failing, DemoParams.of(Map.of(), List.of())))
                .isInstanceOf(DemoInputException.class)
                .hasMessage("error.arrayNotSorted");
    }
}
