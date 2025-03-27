package schwarz.it.lws.win.repository

import org.springframework.data.jpa.repository.JpaRepository
import schwarz.it.lws.win.model.WeatherData
import java.time.LocalDateTime

interface WeatherRepository : JpaRepository<WeatherData, Long> {
    fun findByCityAndForecastDateBetween(
        city: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime): List<WeatherData>
}