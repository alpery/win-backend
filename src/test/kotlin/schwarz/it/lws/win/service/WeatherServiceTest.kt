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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeatherServiceTest {

    // Helper function to generate deterministic UUIDs for testing
    private fun createTestUUID(number: Int): UUID {
        return UUID.fromString("00000000-0000-0000-0000-${String.format("%012d", number)}")
    }

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

    @Test
    fun `getOrFetchWeatherForecast should return data from database when it exists with at least 5 unique dates`() {
        // Given
        // Create weather data for 5 unique dates
        val today = LocalDate.now()
        val weatherDataList = listOf(
            WeatherData(
                id = createTestUUID(1),
                city = testCity,
                forecastDate = LocalDateTime.of(today, LocalTime.of(9, 0)),
                temperature = 20.5,
                minTemperature = 18.0,
                maxTemperature = 22.0,
                humidity = 65,
                description = "Clear sky",
                iconCode = "01d"
            ),
            WeatherData(
                id = createTestUUID(2),
                city = testCity,
                forecastDate = LocalDateTime.of(today.plusDays(1), LocalTime.of(9, 0)),
                temperature = 21.0,
                minTemperature = 18.5,
                maxTemperature = 23.0,
                humidity = 62,
                description = "Clear sky",
                iconCode = "01d"
            ),
            WeatherData(
                id = createTestUUID(3),
                city = testCity,
                forecastDate = LocalDateTime.of(today.plusDays(2), LocalTime.of(9, 0)),
                temperature = 22.0,
                minTemperature = 19.0,
                maxTemperature = 24.0,
                humidity = 60,
                description = "Clear sky",
                iconCode = "01d"
            ),
            WeatherData(
                id = createTestUUID(4),
                city = testCity,
                forecastDate = LocalDateTime.of(today.plusDays(3), LocalTime.of(9, 0)),
                temperature = 21.5,
                minTemperature = 18.5,
                maxTemperature = 23.5,
                humidity = 63,
                description = "Clear sky",
                iconCode = "01d"
            ),
            WeatherData(
                id = createTestUUID(5),
                city = testCity,
                forecastDate = LocalDateTime.of(today.plusDays(4), LocalTime.of(9, 0)),
                temperature = 20.0,
                minTemperature = 17.5,
                maxTemperature = 22.5,
                humidity = 67,
                description = "Clear sky",
                iconCode = "01d"
            )
        )

        every { 
            weatherRepository.findByCityAndForecastDateBetween(
                testCity, 
                any<LocalDateTime>(), 
                any<LocalDateTime>()
            ) 
        } returns weatherDataList

        // When
        val result = weatherService.getOrFetchWeatherForecast(testCity, testLang)

        // Then
        verify { weatherRepository.findByCityAndForecastDateBetween(testCity, any<LocalDateTime>(), any<LocalDateTime>()) }
        // Verify that fetchAndSaveWeatherForecast was not called because there are 5 unique dates
        verify(exactly = 0) { restTemplate.getForObject(any<String>(), eq(OpenWeatherMapResponse::class.java)) }
        assertEquals(5, result.size)
        assertEquals(testCity, result[0].city)
    }

    @Test
    fun `getOrFetchWeatherForecast should fetch data from API when it does not exist in database`() {
        // Given
        val mockResponse = createMockOpenWeatherMapResponse()
        val emptyList = emptyList<WeatherData>()

        every { 
            weatherRepository.findByCityAndForecastDateBetween(
                testCity, 
                any<LocalDateTime>(), 
                any<LocalDateTime>()
            ) 
        } returns emptyList

        every { 
            restTemplate.getForObject(any<String>(), eq(OpenWeatherMapResponse::class.java)) 
        } returns mockResponse

        val savedWeatherDataSlot = slot<List<WeatherData>>()
        every { weatherRepository.saveAll<WeatherData>(capture(savedWeatherDataSlot)) } answers { savedWeatherDataSlot.captured }

        // When
        val result = weatherService.getOrFetchWeatherForecast(testCity, testLang)

        // Then
        verify { weatherRepository.findByCityAndForecastDateBetween(testCity, any<LocalDateTime>(), any<LocalDateTime>()) }
        verify { restTemplate.getForObject(any<String>(), eq(OpenWeatherMapResponse::class.java)) }
        verify { weatherRepository.saveAll<WeatherData>(any()) }
        assertEquals(1, result.size)
        assertEquals(testCity, result[0].city)
    }

    @Test
    fun `getOrFetchWeatherForecast should fetch data from API when there are fewer than 5 unique dates`() {
        // Given
        val mockResponse = createMockOpenWeatherMapResponse()

        // Create weather data for only 2 unique dates
        val today = LocalDate.now()
        val weatherDataList = listOf(
            WeatherData(
                id = createTestUUID(1),
                city = testCity,
                forecastDate = LocalDateTime.of(today, LocalTime.of(9, 0)),
                temperature = 20.5,
                minTemperature = 18.0,
                maxTemperature = 22.0,
                humidity = 65,
                description = "Clear sky",
                iconCode = "01d"
            ),
            WeatherData(
                id = createTestUUID(2),
                city = testCity,
                forecastDate = LocalDateTime.of(today, LocalTime.of(15, 0)),
                temperature = 22.5,
                minTemperature = 19.0,
                maxTemperature = 24.0,
                humidity = 60,
                description = "Clear sky",
                iconCode = "01d"
            ),
            WeatherData(
                id = createTestUUID(3),
                city = testCity,
                forecastDate = LocalDateTime.of(today.plusDays(1), LocalTime.of(9, 0)),
                temperature = 21.0,
                minTemperature = 18.5,
                maxTemperature = 23.0,
                humidity = 62,
                description = "Clear sky",
                iconCode = "01d"
            )
        )

        every { 
            weatherRepository.findByCityAndForecastDateBetween(
                testCity, 
                any<LocalDateTime>(), 
                any<LocalDateTime>()
            ) 
        } returns weatherDataList

        every { 
            restTemplate.getForObject(any<String>(), eq(OpenWeatherMapResponse::class.java)) 
        } returns mockResponse

        val savedWeatherDataSlot = slot<List<WeatherData>>()
        every { weatherRepository.saveAll<WeatherData>(capture(savedWeatherDataSlot)) } answers { savedWeatherDataSlot.captured }

        // When
        val result = weatherService.getOrFetchWeatherForecast(testCity, testLang)

        // Then
        verify { weatherRepository.findByCityAndForecastDateBetween(testCity, any<LocalDateTime>(), any<LocalDateTime>()) }
        // Verify that fetchAndSaveWeatherForecast was called because there are fewer than 5 unique dates
        verify { restTemplate.getForObject(any<String>(), eq(OpenWeatherMapResponse::class.java)) }
        verify { weatherRepository.saveAll<WeatherData>(any()) }
        assertEquals(1, result.size)
        assertEquals(testCity, result[0].city)
    }

    @Test
    fun `createDailySummary should round temperature values to 2 decimal places`() {
        // Given
        val today = LocalDate.now()

        // Create weather data with temperature values that have many decimal places
        val weatherDataList = listOf(
            WeatherData(
                id = createTestUUID(1),
                city = testCity,
                forecastDate = LocalDateTime.of(today, LocalTime.of(9, 0)),
                temperature = 14.194999999999999,
                minTemperature = 13.765432109876543,
                maxTemperature = 14.987654321098765,
                humidity = 65,
                description = "Clear sky",
                iconCode = "01d"
            ),
            WeatherData(
                id = createTestUUID(2),
                city = testCity,
                forecastDate = LocalDateTime.of(today, LocalTime.of(12, 0)),
                temperature = 15.555555555555555,
                minTemperature = 14.222222222222222,
                maxTemperature = 16.888888888888888,
                humidity = 60,
                description = "Clear sky",
                iconCode = "01d"
            ),
            WeatherData(
                id = createTestUUID(3),
                city = testCity,
                forecastDate = LocalDateTime.of(today, LocalTime.of(15, 0)),
                temperature = 16.333333333333333,
                minTemperature = 15.111111111111111,
                maxTemperature = 17.555555555555555,
                humidity = 55,
                description = "Clear sky",
                iconCode = "01d"
            )
        )

        // Use reflection to access the private createDailySummary method
        val createDailySummaryMethod = WeatherService::class.java.getDeclaredMethod(
            "createDailySummary",
            List::class.java,
            LocalDate::class.java
        )
        createDailySummaryMethod.isAccessible = true

        // When
        val summary = createDailySummaryMethod.invoke(weatherService, weatherDataList, today) as WeatherData

        // Then
        // The average temperature should be (14.194999999999999 + 15.555555555555555 + 16.333333333333333) / 3 = 15.36
        // But rounded to 2 decimal places, it should be 15.36
        assertEquals(15.36, summary.temperature, 0.001, "Average temperature should be rounded to 2 decimal places")

        // The min temperature should be 13.765432109876543, but rounded to 2 decimal places, it should be 13.76
        assertEquals(13.76, summary.minTemperature, 0.001, "Min temperature should be rounded to 2 decimal places")

        // The max temperature should be 17.555555555555555, but rounded to 2 decimal places, it should be 17.55
        assertEquals(17.55, summary.maxTemperature, 0.001, "Max temperature should be rounded to 2 decimal places")
    }

    @Test
    fun `deleteOldWeatherData should delete data older than current date`() {
        // Given
        val deletedCount = 5
        val dateSlot = slot<LocalDateTime>()

        every { 
            weatherRepository.deleteByForecastDateBefore(capture(dateSlot)) 
        } returns deletedCount

        // When
        val result = weatherService.deleteOldWeatherData()

        // Then
        verify { weatherRepository.deleteByForecastDateBefore(any()) }
        assertEquals(deletedCount, result)

        // Verify that the date passed to the repository method is the start of the current day
        val today = LocalDate.now()
        val startOfDay = LocalDateTime.of(today, LocalTime.MIN)
        assertEquals(startOfDay, dateSlot.captured)
    }

    @Test
    fun `afterPropertiesSet should call deleteOldWeatherData`() {
        // Given
        val deletedCount = 3
        every { weatherRepository.deleteByForecastDateBefore(any()) } returns deletedCount

        // When
        weatherService.afterPropertiesSet()

        // Then
        verify { weatherRepository.deleteByForecastDateBefore(any()) }
    }

    @Test
    fun `getTransformedWeatherForecast should return 3-hourly forecasts for first day and daily summaries for subsequent days`() {
        // Given
        val today = LocalDate.now()

        // Create weather data with multiple forecasts per day for 3 days
        val weatherDataList = mutableListOf<WeatherData>()

        // Mock the RestTemplate in case getOrFetchWeatherForecast decides to fetch new data
        val mockResponse = createMockOpenWeatherMapResponse()
        every { 
            restTemplate.getForObject(any<String>(), eq(OpenWeatherMapResponse::class.java)) 
        } returns mockResponse

        val savedWeatherDataSlot = slot<List<WeatherData>>()
        every { weatherRepository.saveAll<WeatherData>(capture(savedWeatherDataSlot)) } answers { savedWeatherDataSlot.captured }

        // First day - 3 forecasts
        weatherDataList.add(
            WeatherData(
                id = createTestUUID(1),
                city = testCity,
                forecastDate = LocalDateTime.of(today, LocalTime.of(9, 0)),
                temperature = 20.0,
                minTemperature = 18.0,
                maxTemperature = 22.0,
                humidity = 65,
                description = "Clear sky",
                iconCode = "01d"
            )
        )

        weatherDataList.add(
            WeatherData(
                id = createTestUUID(2),
                city = testCity,
                forecastDate = LocalDateTime.of(today, LocalTime.of(12, 0)),
                temperature = 22.0,
                minTemperature = 20.0,
                maxTemperature = 24.0,
                humidity = 60,
                description = "Clear sky",
                iconCode = "01d"
            )
        )

        weatherDataList.add(
            WeatherData(
                id = createTestUUID(3),
                city = testCity,
                forecastDate = LocalDateTime.of(today, LocalTime.of(15, 0)),
                temperature = 24.0,
                minTemperature = 22.0,
                maxTemperature = 26.0,
                humidity = 55,
                description = "Clear sky",
                iconCode = "01d"
            )
        )

        // Second day - 3 forecasts
        weatherDataList.add(
            WeatherData(
                id = createTestUUID(4),
                city = testCity,
                forecastDate = LocalDateTime.of(today.plusDays(1), LocalTime.of(9, 0)),
                temperature = 19.0,
                minTemperature = 17.0,
                maxTemperature = 21.0,
                humidity = 70,
                description = "Partly cloudy",
                iconCode = "02d"
            )
        )

        weatherDataList.add(
            WeatherData(
                id = createTestUUID(5),
                city = testCity,
                forecastDate = LocalDateTime.of(today.plusDays(1), LocalTime.of(12, 0)),
                temperature = 21.0,
                minTemperature = 19.0,
                maxTemperature = 23.0,
                humidity = 65,
                description = "Partly cloudy",
                iconCode = "02d"
            )
        )

        weatherDataList.add(
            WeatherData(
                id = createTestUUID(6),
                city = testCity,
                forecastDate = LocalDateTime.of(today.plusDays(1), LocalTime.of(15, 0)),
                temperature = 23.0,
                minTemperature = 21.0,
                maxTemperature = 25.0,
                humidity = 60,
                description = "Clear sky",
                iconCode = "01d"
            )
        )

        // Third day - 3 forecasts
        weatherDataList.add(
            WeatherData(
                id = createTestUUID(7),
                city = testCity,
                forecastDate = LocalDateTime.of(today.plusDays(2), LocalTime.of(9, 0)),
                temperature = 18.0,
                minTemperature = 16.0,
                maxTemperature = 20.0,
                humidity = 75,
                description = "Cloudy",
                iconCode = "03d"
            )
        )

        weatherDataList.add(
            WeatherData(
                id = createTestUUID(8),
                city = testCity,
                forecastDate = LocalDateTime.of(today.plusDays(2), LocalTime.of(12, 0)),
                temperature = 20.0,
                minTemperature = 18.0,
                maxTemperature = 22.0,
                humidity = 70,
                description = "Cloudy",
                iconCode = "03d"
            )
        )

        weatherDataList.add(
            WeatherData(
                id = createTestUUID(9),
                city = testCity,
                forecastDate = LocalDateTime.of(today.plusDays(2), LocalTime.of(15, 0)),
                temperature = 22.0,
                minTemperature = 20.0,
                maxTemperature = 24.0,
                humidity = 65,
                description = "Cloudy",
                iconCode = "03d"
            )
        )

        // Mock the repository to return our test data when queried
        every { 
            weatherRepository.findByCityAndForecastDateBetween(
                testCity, 
                any<LocalDateTime>(), 
                any<LocalDateTime>()
            ) 
        } returns weatherDataList

        // When - directly test the transformWeatherData method
        val result = weatherService.transformWeatherData(weatherDataList)

        // Debug logging
        println("[DEBUG_LOG] Weather data list size: ${weatherDataList.size}")
        println("[DEBUG_LOG] Result size: ${result.size}")
        println("[DEBUG_LOG] Weather data dates: ${weatherDataList.map { it.forecastDate.toLocalDate() }.distinct()}")
        println("[DEBUG_LOG] Result dates: ${result.map { it.forecastDate.toLocalDate() }.distinct()}")
        println("[DEBUG_LOG] Today: ${today}")

        // Then
        // Verify that the result contains all 3 forecasts for the first day
        assertEquals(5, result.size, "Result should contain 3 forecasts for first day and 1 summary for each of the 2 subsequent days")

        // First day should have 3 forecasts
        val firstDayForecasts = result.filter { it.forecastDate.toLocalDate() == today }
        assertEquals(3, firstDayForecasts.size, "First day should have 3 forecasts")

        // Verify the first day forecasts are the original ones
        assertEquals(20.0, firstDayForecasts[0].temperature)
        assertEquals(22.0, firstDayForecasts[1].temperature)
        assertEquals(24.0, firstDayForecasts[2].temperature)

        // Second day should have 1 summary forecast
        val secondDayForecasts = result.filter { it.forecastDate.toLocalDate() == today.plusDays(1) }
        assertEquals(1, secondDayForecasts.size, "Second day should have 1 summary forecast")

        // Verify the second day summary has the correct values
        val secondDaySummary = secondDayForecasts[0]
        assertEquals(21.0, secondDaySummary.temperature, 0.01, "Second day summary should have average temperature")
        assertEquals(17.0, secondDaySummary.minTemperature, "Second day summary should have minimum temperature")
        assertEquals(25.0, secondDaySummary.maxTemperature, "Second day summary should have maximum temperature")
        assertEquals(65, secondDaySummary.humidity, "Second day summary should have average humidity")
        assertEquals("Partly cloudy", secondDaySummary.description, "Second day summary should have most common description")
        assertEquals("02d", secondDaySummary.iconCode, "Second day summary should have most common icon code")

        // Third day should have 1 summary forecast
        val thirdDayForecasts = result.filter { it.forecastDate.toLocalDate() == today.plusDays(2) }
        assertEquals(1, thirdDayForecasts.size, "Third day should have 1 summary forecast")

        // Verify the third day summary has the correct values
        val thirdDaySummary = thirdDayForecasts[0]
        assertEquals(20.0, thirdDaySummary.temperature, 0.01, "Third day summary should have average temperature")
        assertEquals(16.0, thirdDaySummary.minTemperature, "Third day summary should have minimum temperature")
        assertEquals(24.0, thirdDaySummary.maxTemperature, "Third day summary should have maximum temperature")
        assertEquals(70, thirdDaySummary.humidity, "Third day summary should have average humidity")
        assertEquals("Cloudy", thirdDaySummary.description, "Third day summary should have most common description")
        assertEquals("03d", thirdDaySummary.iconCode, "Third day summary should have most common icon code")
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
                id = createTestUUID(1),
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
