package schwarz.it.lws.win

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class WinApplication

fun main(args: Array<String>) {
    runApplication<WinApplication>(*args)
}
