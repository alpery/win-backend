package schwarz.it.lws.win.controller

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import schwarz.it.lws.win.repository.WeatherRepository
import org.mockito.Mockito

@WebMvcTest(HealthController::class)
@Import(HealthControllerTest.TestConfig::class)
@TestPropertySource(properties = ["spring.test.mockmvc.print=default"])
class HealthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var weatherRepository: WeatherRepository

    class TestConfig {
        @Bean
        fun weatherRepository(): WeatherRepository = Mockito.mock(WeatherRepository::class.java)
    }

    @Test
    fun `healthCheck should return 200 OK when database is healthy`() {
        // Given
        // Reset any previous mock behavior
        Mockito.reset(weatherRepository)
        // Set up the mock to return 5L for count()
        `when`(weatherRepository.count()).thenReturn(5L)

        // When/Then
        mockMvc.perform(
            get("/api/health")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.database.status").value("UP"))
            .andExpect(jsonPath("$.database.recordCount").value(5))
    }

    @Test
    fun `healthCheck should return 503 Service Unavailable when database connection fails`() {
        // Given
        // Reset any previous mock behavior
        Mockito.reset(weatherRepository)
        // Set up the mock to throw an exception for count()
        doThrow(RuntimeException("Database connection failed")).`when`(weatherRepository).count()

        // When/Then
        mockMvc.perform(
            get("/api/health")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.status").value("DOWN"))
            .andExpect(jsonPath("$.database.status").value("DOWN"))
            .andExpect(jsonPath("$.database.message").value("Database connection failed: Database connection failed"))
    }
}
