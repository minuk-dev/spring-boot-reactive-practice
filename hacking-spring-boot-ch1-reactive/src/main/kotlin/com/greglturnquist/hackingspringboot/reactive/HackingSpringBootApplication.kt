package com.greglturnquist.hackingspringboot.reactive

import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toFlux

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.http.MediaType
import org.springframework.boot.runApplication
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.GetMapping
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility

import java.util.Random
import java.time.Duration
import java.util.stream.Stream

@SpringBootApplication
class HackingSpringBootApplication

fun main(args: Array<String>) {
  runApplication<HackingSpringBootApplication>(*args)
}

@RestController
class ServerController(
  final val kitchen: KitchenService
) {
  @GetMapping(value = ["/server"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
  fun serveDishes(): Flux<Dish> {
    return kitchen.dishes
  }
}

@Service
class KitchenService {
  val dishes: Flux<Dish>
    get(): Flux<Dish>{
      return Flux.generate<Dish>{ it.next(randomDish()) }
        .delayElements(Duration.ofMillis(250))
    }

  fun randomDish(): Dish {
    return menu[picker.nextInt(menu.size)]
  }

  val menu = listOf(
    Dish("Sesame chicken"),
    Dish("Lo mein noodles, plain"),
    Dish("Sweet & sour beef"),
  )

  val picker = Random()
}

@JsonAutoDetect(fieldVisibility = Visibility.ANY)
class Dish {
  constructor(desc: String) {
    description = desc
  }

  private var delivered: Boolean = false
    get() = field
  private var description: String
    get() = field
    set(desc: String) {
      field = desc
    }

  override fun toString(): String = "Dish{description=\'$description\', delivered=$delivered}"

  companion object {
    fun deliver(dish: Dish): Dish {
      val deliveredDish = Dish(dish.description)
      deliveredDish.delivered = true
      return deliveredDish
    }
  }
}
