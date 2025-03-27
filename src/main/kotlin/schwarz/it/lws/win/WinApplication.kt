package schwarz.it.lws.win

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WinApplication

fun main(args: Array<String>) {
    runApplication<WinApplication>(*args)
}
