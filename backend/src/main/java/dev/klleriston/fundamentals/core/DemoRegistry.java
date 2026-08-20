package dev.klleriston.fundamentals.core;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DemoRegistry {

    private final Map<String, Demo> byId = new LinkedHashMap<>();

    public DemoRegistry(List<Demo> demos) {
        demos.stream()
                .sorted(Comparator.comparing(Demo::id))
                .forEach(demo -> {
                    if (byId.putIfAbsent(demo.id(), demo) != null) {
                        throw new IllegalStateException("Duplicate demo id: " + demo.id());
                    }
                });
    }

    public List<Demo> all() {
        return List.copyOf(byId.values());
    }

    public Demo require(String id) {
        Demo demo = byId.get(id);
        if (demo == null) {
            throw new UnknownDemoException(id);
        }
        return demo;
    }
}
