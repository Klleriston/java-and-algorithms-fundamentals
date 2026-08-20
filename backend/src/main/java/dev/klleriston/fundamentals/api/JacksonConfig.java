package dev.klleriston.fundamentals.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import dev.klleriston.fundamentals.trace.ViewPayload;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    public JacksonConfig(ObjectMapper objectMapper) {
        SimpleModule module = new SimpleModule();
        module.setMixInAnnotation(ViewPayload.class, ViewPayloadMixin.class);
        objectMapper.registerModule(module);
    }

    public interface ViewPayloadMixin {
        @JsonGetter
        String kind();
    }
}
