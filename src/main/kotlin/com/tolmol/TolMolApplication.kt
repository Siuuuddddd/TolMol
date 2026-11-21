package com.tolmol
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
class TolMolApplication
fun main(args: Array<String>) { runApplication<TolMolApplication>(*args) }
