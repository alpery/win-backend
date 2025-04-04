package schwarz.it.lws.win.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import schwarz.it.lws.win.repository.WeatherRepository

@RestController
@RequestMapping("/api/health")
class HealthController(private val weatherRepository: WeatherRepository) {

    /**
     * Health check endpoint that verifies the application is running
     * and can connect to the database.
     * 
     * @return 200 OK if the application is healthy, 503 Service Unavailable otherwise
     */
    @GetMapping
    fun healthCheck(): ResponseEntity<Map<String, Any>> {
        val status = "UP"
        val response = mutableMapOf<String, Any>(
            "status" to status,
            "timestamp" to System.currentTimeMillis()
        )
        
        try {
            // Check database connectivity by counting entities
            val count = weatherRepository.count()
            response["database"] = mapOf(
                "status" to "UP",
                "message" to "Database connection successful",
                "recordCount" to count
            )
        } catch (e: Exception) {
            response["status"] = "DOWN"
            response["database"] = mapOf(
                "status" to "DOWN",
                "message" to "Database connection failed: ${e.message}"
            )
            return ResponseEntity.status(503).body(response)
        }
        
        return ResponseEntity.ok(response)
    }
}