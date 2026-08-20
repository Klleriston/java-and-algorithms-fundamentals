package dev.klleriston.fundamentals.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsDemosWithParameterSchema() throws Exception {
        mockMvc.perform(get("/api/demos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("binary-search"))
                .andExpect(jsonPath("$[0].category").value("ALGORITHMS"))
                .andExpect(jsonPath("$[0].parameters[0].name").value("array"))
                .andExpect(jsonPath("$[0].parameters[0].type").value("INT_ARRAY"))
                .andExpect(jsonPath("$[0].sourceCode").isNotEmpty())
                .andExpect(jsonPath("$[1].id").value("bubble-sort"));
    }

    @Test
    void returnsTraceForValidParameters() throws Exception {
        mockMvc.perform(post("/api/demos/binary-search/trace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"array\":[2,5,8,12,20,33],\"target\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demoId").value("binary-search"))
                .andExpect(jsonPath("$.result.returnValue").value(4))
                .andExpect(jsonPath("$.result.measured").value(false))
                .andExpect(jsonPath("$.steps[0].view.kind").value("ARRAY"))
                .andExpect(jsonPath("$.steps[0].line").isNumber())
                .andExpect(jsonPath("$.steps[0].vars.low").value(0));
    }

    @Test
    void usesDefaultsWhenBodyIsEmpty() throws Exception {
        mockMvc.perform(post("/api/demos/bubble-sort/trace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps").isNotEmpty());
    }

    @Test
    void rejectsUnsortedArrayWithFieldAndMessage() throws Exception {
        mockMvc.perform(post("/api/demos/binary-search/trace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"array\":[5,2,8],\"target\":8}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.field").value("array"))
                .andExpect(jsonPath("$.message").value("array must be sorted ascending"));
    }

    @Test
    void rejectsOversizedArray() throws Exception {
        StringBuilder body = new StringBuilder("{\"array\":[");
        for (int i = 0; i < 70; i++) {
            body.append(i).append(i < 69 ? "," : "");
        }
        body.append("]}");

        mockMvc.perform(post("/api/demos/bubble-sort/trace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("array"));
    }

    @Test
    void returnsNotFoundForUnknownDemo() throws Exception {
        mockMvc.perform(post("/api/demos/nope/trace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("UNKNOWN_DEMO"));
    }
}
