package schwarz.it.lws.win.controller

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import schwarz.it.lws.win.model.WeatherData
import schwarz.it.lws.win.service.WeatherService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class WeatherControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun weatherService(): WeatherService = mockk(relaxed = true)
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var weatherService: WeatherService

    private val testCity = "Berlin"
    private val testLang = "en"
    private val testDate = LocalDateTime.now()
    private val testWeatherData = listOf(
        WeatherData(
            id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            city = testCity,
            forecastDate = testDate,
            temperature = 20.5,
            minTemperature = 18.0,
            maxTemperature = 22.0,
            humidity = 65,
            description = "Clear sky",
            iconCode = "01d"
        )
    )

    @BeforeEach
    fun setup() {
        // Clear all recorded calls before each test
        io.mockk.clearAllMocks()
    }

    @Test
    fun `getWeatherForCity should return weather data`() {
        // Given
        every { weatherService.getTransformedWeatherForecast(testCity, testLang) } returns testWeatherData

        // When/Then
        mockMvc.perform(
            get("/api/weather/$testCity")
                .param("lang", testLang)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].city").value(testCity))
            .andExpect(jsonPath("$[0].temperature").value(20.5))
            .andExpect(jsonPath("$[0].description").value("Clear sky"))
    }

    @Test
    fun `getWeatherForCityInRange should return weather data for date range`() {
        // Given
        val startDate = LocalDateTime.now().toLocalDate()
        val endDate = startDate.plusDays(2)
        val formatter = DateTimeFormatter.ISO_DATE
        val startDateTime = LocalDateTime.of(startDate, java.time.LocalTime.MIN)
        val endDateTime = LocalDateTime.of(endDate, java.time.LocalTime.MAX)

        every { weatherService.getWeatherForecast(testCity, startDateTime, endDateTime) } returns testWeatherData

        // When/Then
        mockMvc.perform(
            get("/api/weather/$testCity/range")
                .param("startDate", startDate.format(formatter))
                .param("endDate", endDate.format(formatter))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].city").value(testCity))
            .andExpect(jsonPath("$[0].temperature").value(20.5))
            .andExpect(jsonPath("$[0].description").value("Clear sky"))
    }

}
