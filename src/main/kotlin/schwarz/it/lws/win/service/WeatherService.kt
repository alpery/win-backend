package schwarz.it.lws.win.service

import io.github.cdimascio.dotenv.dotenv
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.beans.factory.annotation.Autowired
import schwarz.it.lws.win.model.OpenWeatherMapResponse
import schwarz.it.lws.win.model.WeatherData
import schwarz.it.lws.win.repository.WeatherRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class WeatherService @Autowired constructor(
    private val weatherRepository: WeatherRepository
) {

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
     * If no data is found, fetches it from the OpenWeatherMap API and saves it to the database.
     */
    fun getOrFetchWeatherForecast(city: String, lang: String = "en"): List<WeatherData> {
        val weatherData = getWeatherForecast(city)

        // If no data is found in the database, fetch it from the API
        if (weatherData.isEmpty()) {
            return fetchAndSaveWeatherForecast(city, lang)
        }

        return weatherData
    }
}
