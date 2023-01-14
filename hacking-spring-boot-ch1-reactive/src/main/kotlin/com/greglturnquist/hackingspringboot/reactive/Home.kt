package com.greglturnquist.hackingspringboot.reactive

import reactor.core.publisher.Mono
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomeController {
  @GetMapping
  fun home(): Mono<String> = Mono.just("home")
}
