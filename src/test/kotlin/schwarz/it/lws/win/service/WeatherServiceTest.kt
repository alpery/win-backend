package schwarz.it.lws.win.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.client.RestTemplate
import schwarz.it.lws.win.model.*
import schwarz.it.lws.win.repository.WeatherRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeatherServiceTest {

    private lateinit var weatherService: WeatherService
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var restTemplate: RestTemplate

    private val testCity = "Berlin"
    private val testLang = "en"
    private val testDate = LocalDateTime.now()
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @BeforeEach
    fun setup() {
        weatherRepository = mockk()
        restTemplate = mockk()

        // Use reflection to set the private restTemplate field
        val weatherServiceInstance = WeatherService(weatherRepository)
        val restTemplateField = WeatherService::class.java.getDeclaredField("restTemplate")
        restTemplateField.isAccessible = true
        restTemplateField.set(weatherServiceInstance, restTemplate)

        weatherService = weatherServiceInstance
    }

    @Test
    fun `fetchAndSaveWeatherForecast should fetch data and save it to repository`() {
        // Given
        val mockResponse = createMockOpenWeatherMapResponse()
        val expectedUrl = "https://api.openweathermap.org/data/2.5/forecast?q=$testCity&lang=$testLang&units=metric&appid="

        every { 
            restTemplate.getForObject(any<String>(), eq(OpenWeatherMapResponse::class.java)) 
        } returns mockResponse

        val savedWeatherDataSlot = slot<List<WeatherData>>()
        every { weatherRepository.saveAll<WeatherData>(capture(savedWeatherDataSlot)) } answers { savedWeatherDataSlot.captured }

        // When
        val result = weatherService.fetchAndSaveWeatherForecast(testCity, testLang)

        // Then
        verify { weatherRepository.saveAll<WeatherData>(any()) }
        assertEquals(1, result.size)
        assertEquals(testCity, result[0].city)
        assertEquals(20.5, result[0].temperature)
        assertEquals("Clear sky", result[0].description)
    }

    @Test
    fun `fetchAndSaveWeatherForecast should throw exception when API call fails`() {
        // Given
        every { 
            restTemplate.getForObject(any<String>(), eq(OpenWeatherMapResponse::class.java)) 
        } returns null

        // When/Then
        assertThrows<RuntimeException> { 
            weatherService.fetchAndSaveWeatherForecast(testCity, testLang) 
        }
    }

    @Test
    fun `getWeatherForecast should return data for city`() {
        // Given
        val mockWeatherData = createMockWeatherData()
        every { 
            weatherRepository.findByCityAndForecastDateBetween(
                testCity, 
                any<LocalDateTime>(), 
                any<LocalDateTime>()
            ) 
        } returns mockWeatherData

        // When
        val result = weatherService.getWeatherForecast(testCity)

        // Then
        verify { weatherRepository.findByCityAndForecastDateBetween(testCity, any<LocalDateTime>(), any<LocalDateTime>()) }
        assertEquals(1, result.size)
        assertEquals(testCity, result[0].city)
    }

    @Test
    fun `getWeatherForecast with date range should return data for city and date range`() {
        // Given
        val startDate = LocalDateTime.now()
        val endDate = startDate.plusDays(2)
        val mockWeatherData = createMockWeatherData()

        every { 
            weatherRepository.findByCityAndForecastDateBetween(
                testCity, 
                startDate, 
                endDate
            ) 
        } returns mockWeatherData

        // When
        val result = weatherService.getWeatherForecast(testCity, startDate, endDate)

        // Then
        verify { weatherRepository.findByCityAndForecastDateBetween(testCity, startDate, endDate) }
        assertEquals(1, result.size)
        assertEquals(testCity, result[0].city)
    }

    private fun createMockOpenWeatherMapResponse(): OpenWeatherMapResponse {
        val weather = Weather(
            id = 800,
            main = "Clear",
            description = "Clear sky",
            icon = "01d"
        )

        val main = Main(
            temp = 20.5,
            feelsLike = 19.8,
            tempMin = 18.0,
            tempMax = 22.0,
            pressure = 1013,
            seaLevel = 1013,
            grndLevel = 1010,
            humidity = 65,
            tempKf = 0.0
        )

        val forecastItem = ForecastItem(
            dt = 1625097600,
            main = main,
            weather = listOf(weather),
            clouds = Clouds(all = 0),
            wind = Wind(speed = 3.5, deg = 180, gust = 5.0),
            visibility = 10000,
            pop = 0.0,
            rain = null,
            sys = Sys(pod = "d"),
            dtTxt = testDate.format(dateTimeFormatter)
        )

        val city = City(
            id = 2950159,
            name = testCity,
            coord = Coord(lat = 52.5244, lon = 13.4105),
            country = "DE",
            population = 1000000,
            timezone = 7200,
            sunrise = 1625018087,
            sunset = 1625078087
        )

        return OpenWeatherMapResponse(
            cod = "200",
            message = 0,
            cnt = 1,
            list = listOf(forecastItem),
            city = city
        )
    }

    private fun createMockWeatherData(): List<WeatherData> {
        return listOf(
            WeatherData(
                id = 1L,
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
    }
}
