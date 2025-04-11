package schwarz.it.lws.win.controller

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import schwarz.it.lws.win.model.WeatherData
import schwarz.it.lws.win.service.WeatherService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@RestController
@RequestMapping("/api/weather")
class WeatherController(private val weatherService: WeatherService) {

    /**
     * Gets weather forecast data for the given city from the database.
     * If no data is found, fetches it from the OpenWeatherMap API and saves it to the database.
     * Returns 3-hourly forecasts for the first day and a single summary forecast for each subsequent day.
     */
    @GetMapping("/{city}")
    fun getWeatherForCity(
        @PathVariable city: String,
        @RequestParam(required = false, defaultValue = "de") lang: String
    ): ResponseEntity<List<WeatherData>> {
        val weatherData = weatherService.getTransformedWeatherForecast(city, lang)
        return ResponseEntity.ok(weatherData)
    }

    /**
     * Gets weather forecast data for the given city and date range from the database
     */
    @GetMapping("/{city}/range")
    fun getWeatherForCityInRange(
        @PathVariable city: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<List<WeatherData>> {
        val startDateTime = LocalDateTime.of(startDate, LocalTime.MIN)
        val endDateTime = LocalDateTime.of(endDate, LocalTime.MAX)
        val weatherData = weatherService.getWeatherForecast(city, startDateTime, endDateTime)
        return ResponseEntity.ok(weatherData)
    }

}
