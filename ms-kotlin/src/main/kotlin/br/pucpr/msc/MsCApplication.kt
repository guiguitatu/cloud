package br.pucpr.msc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MsCApplication

fun main(args: Array<String>) {
	runApplication<MsCApplication>(*args)
}
