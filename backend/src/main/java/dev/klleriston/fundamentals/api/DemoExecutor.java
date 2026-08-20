package dev.klleriston.fundamentals.api;

import dev.klleriston.fundamentals.core.Demo;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.trace.Trace;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class DemoExecutor {

    private final Duration limit;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public DemoExecutor() {
        this(Duration.ofSeconds(5));
    }

    public DemoExecutor(Duration limit) {
        this.limit = limit;
    }

    public Trace run(Demo demo, DemoParams params) {
        Future<Trace> future = executor.submit(() -> demo.run(params));
        try {
            return future.get(limit.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeout) {
            future.cancel(true);
            throw new DemoTimeoutException(limit);
        } catch (ExecutionException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException(cause);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }
}
