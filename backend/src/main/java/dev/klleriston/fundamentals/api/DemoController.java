package dev.klleriston.fundamentals.api;

import dev.klleriston.fundamentals.core.Demo;
import dev.klleriston.fundamentals.core.DemoParams;
import dev.klleriston.fundamentals.core.DemoRegistry;
import dev.klleriston.fundamentals.trace.Trace;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demos")
public class DemoController {

    private final DemoRegistry registry;

    public DemoController(DemoRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<DemoSummary> list() {
        return registry.all().stream().map(DemoSummary::from).toList();
    }

    @PostMapping("/{id}/trace")
    public Trace trace(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        Demo demo = registry.require(id);
        DemoParams params = DemoParams.of(body == null ? Map.of() : body, demo.parameters());
        return demo.run(params);
    }
}
