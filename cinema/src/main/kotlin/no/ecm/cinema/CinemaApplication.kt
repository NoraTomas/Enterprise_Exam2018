package no.ecm.cinema

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

//TODO import dto from utils module
@SpringBootApplication(scanBasePackages = ["no.ecm.cinema"])
class CinemaApplication {}

fun main(args: Array<String>) {
    SpringApplication.run(CinemaApplication::class.java, *args)
}