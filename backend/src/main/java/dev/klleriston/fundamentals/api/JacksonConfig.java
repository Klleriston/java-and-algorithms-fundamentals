package dev.klleriston.fundamentals.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import dev.klleriston.fundamentals.trace.ViewPayload;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes ViewPayload.kind() as the JSON property "kind". Jackson only auto-detects getX()/isX()
 * accessors, and the trace package deliberately carries no Jackson annotations, so the mapping
 * lives here as a mix-in instead.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Module viewPayloadModule() {
        SimpleModule module = new SimpleModule("view-payload");
        module.setMixInAnnotation(ViewPayload.class, ViewPayloadMixin.class);
        return module;
    }

    interface ViewPayloadMixin {

        @JsonGetter
        String kind();
    }
}
