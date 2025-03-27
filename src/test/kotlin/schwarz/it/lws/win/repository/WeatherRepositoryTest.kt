package schwarz.it.lws.win.repository

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import schwarz.it.lws.win.model.WeatherData
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DataJpaTest
@ActiveProfiles("test")
class WeatherRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var weatherRepository: WeatherRepository

    @Test
    fun `findByCityAndForecastDateBetween should return weather data for city and date range`() {
        // Given
        val city = "Berlin"
        val now = LocalDateTime.now()
        val startDate = now.minusDays(1)
        val endDate = now.plusDays(1)

        // Create test data
        val weatherData1 = WeatherData(
            city = city,
            forecastDate = now,
            temperature = 20.5,
            minTemperature = 18.0,
            maxTemperature = 22.0,
            humidity = 65,
            description = "Clear sky",
            iconCode = "01d"
        )

        val weatherData2 = WeatherData(
            city = city,
            forecastDate = now.plusDays(2), // Outside the range
            temperature = 22.0,
            minTemperature = 19.0,
            maxTemperature = 24.0,
            humidity = 60,
            description = "Partly cloudy",
            iconCode = "02d"
        )

        val weatherData3 = WeatherData(
            city = "Munich", // Different city
            forecastDate = now,
            temperature = 18.0,
            minTemperature = 15.0,
            maxTemperature = 20.0,
            humidity = 70,
            description = "Rainy",
            iconCode = "09d"
        )

        // Save test data
        entityManager.persist(weatherData1)
        entityManager.persist(weatherData2)
        entityManager.persist(weatherData3)
        entityManager.flush()

        // When
        val result = weatherRepository.findByCityAndForecastDateBetween(city, startDate, endDate)

        // Then
        assertEquals(1, result.size)
        assertEquals(city, result[0].city)
        assertEquals(now, result[0].forecastDate)
        assertEquals(20.5, result[0].temperature)
        assertEquals("Clear sky", result[0].description)
    }

    @Test
    fun `findByCityAndForecastDateBetween should return empty list when no data matches`() {
        // Given
        val city = "Berlin"
        val now = LocalDateTime.now()
        val startDate = now.minusDays(1)
        val endDate = now.plusDays(1)

        // Create test data for a different city
        val weatherData = WeatherData(
            city = "Munich",
            forecastDate = now,
            temperature = 18.0,
            minTemperature = 15.0,
            maxTemperature = 20.0,
            humidity = 70,
            description = "Rainy",
            iconCode = "09d"
        )

        // Save test data
        entityManager.persist(weatherData)
        entityManager.flush()

        // When
        val result = weatherRepository.findByCityAndForecastDateBetween(city, startDate, endDate)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findByCityAndForecastDateBetween should return multiple results when multiple data matches`() {
        // Given
        val city = "Berlin"
        val now = LocalDateTime.now()
        val startDate = now.minusDays(1)
        val endDate = now.plusDays(1)

        // Create multiple test data entries for the same city and date range
        val weatherData1 = WeatherData(
            city = city,
            forecastDate = now.minusHours(6),
            temperature = 19.0,
            minTemperature = 17.0,
            maxTemperature = 21.0,
            humidity = 68,
            description = "Clear sky",
            iconCode = "01d"
        )

        val weatherData2 = WeatherData(
            city = city,
            forecastDate = now,
            temperature = 20.5,
            minTemperature = 18.0,
            maxTemperature = 22.0,
            humidity = 65,
            description = "Clear sky",
            iconCode = "01d"
        )

        // Save test data
        entityManager.persist(weatherData1)
        entityManager.persist(weatherData2)
        entityManager.flush()

        // When
        val result = weatherRepository.findByCityAndForecastDateBetween(city, startDate, endDate)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.city == city })
        assertTrue(result.all { it.forecastDate >= startDate && it.forecastDate <= endDate })
    }
}
