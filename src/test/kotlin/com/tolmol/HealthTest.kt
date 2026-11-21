package com.tolmol
import com.tolmol.platform.api.controllers.HealthController
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class HealthTest {
    @Test fun healthEndpoint() {
        MockMvcBuilders.standaloneSetup(HealthController()).build().perform(get("/health"))
            .andExpect(status().isOk).andExpect(jsonPath("$.status").value("UP"))
    }
}
