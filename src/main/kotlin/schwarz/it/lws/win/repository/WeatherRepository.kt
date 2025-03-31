package schwarz.it.lws.win.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import schwarz.it.lws.win.model.WeatherData
import java.time.LocalDateTime

interface WeatherRepository : JpaRepository<WeatherData, Long> {
    fun findByCityAndForecastDateBetween(
        city: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime): List<WeatherData>

    /**
     * Deletes all weather data with forecast date before the given date
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM WeatherData w WHERE w.forecastDate < :date")
    fun deleteByForecastDateBefore(date: LocalDateTime): Int
}
