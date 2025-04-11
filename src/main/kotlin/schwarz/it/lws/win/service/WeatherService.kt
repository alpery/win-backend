package schwarz.it.lws.win.service

import io.github.cdimascio.dotenv.dotenv
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.InitializingBean
import schwarz.it.lws.win.model.OpenWeatherMapResponse
import schwarz.it.lws.win.model.WeatherData
import schwarz.it.lws.win.repository.WeatherRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
class WeatherService @Autowired constructor(
    private val weatherRepository: WeatherRepository
) : InitializingBean {

    /**
     * Initializes the service and performs cleanup of old weather data at startup
     */
    override fun afterPropertiesSet() {
        val deletedCount = deleteOldWeatherData()
        println("Startup cleanup completed: $deletedCount old weather records deleted")
    }

    // Load environment variables from .env file
    private val dotenv = dotenv {
        directory = "./"
        ignoreIfMissing = true
    }

    // Get the OpenWeatherMap API key from .env file
    private val apiKey: String = dotenv["OPENWEATHERMAP_API_KEY"]
    private val restTemplate = RestTemplate()
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /**
     * Fetches weather forecast data from OpenWeatherMap API for the given city
     * and saves it to the database
     */
    fun fetchAndSaveWeatherForecast(city: String, lang: String = "en"): List<WeatherData> {
        val url = "https://api.openweathermap.org/data/2.5/forecast?q=$city&lang=$lang&units=metric&appid=$apiKey"
        val response = restTemplate.getForObject(url, OpenWeatherMapResponse::class.java)
            ?: throw RuntimeException("Failed to fetch weather data from OpenWeatherMap API")

        val weatherDataList = response.list.map { forecastItem ->
            val weather = forecastItem.weather.first()
            WeatherData(
                city = response.city.name,
                forecastDate = LocalDateTime.parse(forecastItem.dtTxt, dateTimeFormatter),
                temperature = forecastItem.main.temp,
                minTemperature = forecastItem.main.tempMin,
                maxTemperature = forecastItem.main.tempMax,
                humidity = forecastItem.main.humidity,
                description = weather.description,
                iconCode = weather.icon
            )
        }

        return weatherRepository.saveAll(weatherDataList)
    }

    /**
     * Gets weather forecast data for the given city from the database
     */
    fun getWeatherForecast(city: String): List<WeatherData> {
        val now = LocalDateTime.now()
        val fiveDaysLater = now.plusDays(5)
        return weatherRepository.findByCityAndForecastDateBetween(city, now, fiveDaysLater)
    }

    /**
     * Gets weather forecast data for the given city and date range from the database
     */
    fun getWeatherForecast(city: String, startDate: LocalDateTime, endDate: LocalDateTime): List<WeatherData> {
        return weatherRepository.findByCityAndForecastDateBetween(city, startDate, endDate)
    }

    /**
     * Gets weather forecast data for the given city from the database.
     * If no data is found or if there's not enough data for a full 5-day forecast,
     * fetches it from the OpenWeatherMap API and saves it to the database.
     */
    fun getOrFetchWeatherForecast(city: String, lang: String = "en"): List<WeatherData> {
        val weatherData = getWeatherForecast(city)

        // Get the number of unique forecast dates in the data
        val uniqueDates = weatherData.map { it.forecastDate.toLocalDate() }.distinct()

        // If no data is found or if there are fewer than 5 unique dates, fetch new data from the API
        if (weatherData.isEmpty() || uniqueDates.size < 5) {
            return fetchAndSaveWeatherForecast(city, lang)
        }

        return weatherData
    }

    /**
     * Gets weather forecast data for the given city and transforms it to show:
     * - 3-hourly forecasts for the first day
     * - A single summary forecast for each subsequent day
     */
    fun getTransformedWeatherForecast(city: String, lang: String = "en"): List<WeatherData> {
        val weatherData = getOrFetchWeatherForecast(city, lang)
        return transformWeatherData(weatherData)
    }

    /**
     * Transforms weather data to show:
     * - 3-hourly forecasts for the first day
     * - A single summary forecast for each subsequent day
     */
    fun transformWeatherData(weatherData: List<WeatherData>): List<WeatherData> {
        if (weatherData.isEmpty()) {
            return emptyList()
        }

        // Group forecasts by date
        val forecastsByDate = weatherData.groupBy { it.forecastDate.toLocalDate() }

        // Sort dates to ensure they're in chronological order
        val sortedDates = forecastsByDate.keys.sorted()

        if (sortedDates.isEmpty()) {
            return emptyList()
        }

        val result = mutableListOf<WeatherData>()

        // For the first day, keep all 3-hourly forecasts
        val firstDay = sortedDates.first()
        forecastsByDate[firstDay]?.let { result.addAll(it) }

        // For subsequent days, create a daily summary
        sortedDates.drop(1).forEach { date ->
            forecastsByDate[date]?.let { forecasts ->
                if (forecasts.isNotEmpty()) {
                    // Create a summary forecast for the day
                    val summary = createDailySummary(forecasts, date)
                    result.add(summary)
                }
            }
        }

        return result
    }

    /**
     * Creates a daily summary forecast from a list of forecasts for a specific date
     */
    private fun createDailySummary(forecasts: List<WeatherData>, date: LocalDate): WeatherData {
        // Calculate average temperature and humidity
        val avgTemperature = (forecasts.map { it.temperature }.average() * 100).toInt() / 100.0
        val avgHumidity = forecasts.map { it.humidity }.average().toInt()

        // Find min and max temperatures
        val minTemp = forecasts.minByOrNull { it.minTemperature }?.minTemperature?.let { (it * 100).toInt() / 100.0 } ?: 0.0
        val maxTemp = forecasts.maxByOrNull { it.maxTemperature }?.maxTemperature?.let { (it * 100).toInt() / 100.0 } ?: 0.0

        // Find the most common description and icon
        val descriptionCounts = forecasts.groupBy { it.description }.mapValues { it.value.size }
        val mostCommonDescription = descriptionCounts.maxByOrNull { it.value }?.key ?: ""

        val iconCounts = forecasts.groupBy { it.iconCode }.mapValues { it.value.size }
        val mostCommonIcon = iconCounts.maxByOrNull { it.value }?.key ?: ""

        // Create a summary forecast at noon for the day
        val noonTime = LocalDateTime.of(date, LocalTime.of(12, 0))

        return WeatherData(
            city = forecasts.first().city,
            forecastDate = noonTime,
            temperature = avgTemperature,
            minTemperature = minTemp,
            maxTemperature = maxTemp,
            humidity = avgHumidity,
            description = mostCommonDescription,
            iconCode = mostCommonIcon
        )
    }

    /**
     * Deletes all weather data with forecast date before the current date.
     * Returns the number of records deleted.
     */
    fun deleteOldWeatherData(): Int {
        val today = LocalDate.now()
        val startOfDay = LocalDateTime.of(today, LocalTime.MIN)
        return weatherRepository.deleteByForecastDateBefore(startOfDay)
    }

    /**
     * Scheduled task that runs daily at midnight to delete old weather data
     */
    @Scheduled(cron = "0 0 0 * * ?") // Run at midnight every day
    fun scheduledCleanup() {
        val deletedCount = deleteOldWeatherData()
        println("Scheduled cleanup completed: $deletedCount old weather records deleted")
    }
}
