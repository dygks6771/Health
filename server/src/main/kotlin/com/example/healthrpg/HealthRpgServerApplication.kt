package com.example.healthrpg

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HealthRpgServerApplication

fun main(args: Array<String>) {
    runApplication<HealthRpgServerApplication>(*args)
}
