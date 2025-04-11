package schwarz.it.lws.win.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.GenericGenerator
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "weather_data")
data class WeatherData(
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(nullable = false)
    val city: String,

    @Column(nullable = false)
    val forecastDate: LocalDateTime,

    @Column(nullable = false)
    val temperature: Double,

    @Column(nullable = false)
    val minTemperature: Double,

    @Column(nullable = false)
    val maxTemperature: Double,

    @Column(nullable = false)
    val humidity: Int,

    @Column(nullable = false)
    val description: String,

    @Column(nullable = false)
    val iconCode: String,

    @Column(nullable = false)
    @CreationTimestamp
    val createdAt: LocalDateTime = LocalDateTime.now()
)
