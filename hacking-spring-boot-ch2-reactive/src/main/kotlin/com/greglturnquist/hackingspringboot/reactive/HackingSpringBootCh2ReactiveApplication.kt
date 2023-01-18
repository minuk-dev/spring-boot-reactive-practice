package com.greglturnquist.hackingspringboot.reactive

import kotlinx.coroutines.reactive.awaitFirstOrElse
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.core.MongoOperations

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.reactive.result.view.Rendering
import reactor.core.publisher.Mono

@SpringBootApplication
class HackingSpringBootCh2ReactiveApplication

fun main(args: Array<String>) {
  runApplication<HackingSpringBootCh2ReactiveApplication>(*args)
}

data class Item(
  val name: String,
  val price: Double,
) {
  var id: String? = null
}

data class CartItem(
  val item: Item,
  var quantity: Int = 1,
) {
  constructor(item: Item): this(item, 1) {}

  fun increment() {
    this.quantity += 1
  }
}

data class Cart (
  val id: String?,
  val cartItems: MutableList<CartItem>,
) {
  constructor(id: String): this(id, mutableListOf()) {}
}

@Repository
interface ItemRepository: ReactiveCrudRepository<Item, String> {}
@Repository
interface CartRepository: ReactiveCrudRepository<Cart, String> {}

@Component
class RepositoryDatabaseLoader {
  @Bean
  fun initialize(repository: MongoOperations): CommandLineRunner {
    return CommandLineRunner {
      repository.save(Item("Alf alarm clock", 19.99))
      repository.save(Item("Smurf TV tray", 24.99))
    }
  }
}

@Controller
class HomeController(
  private val itemRepository: ItemRepository,
  private val cartRepository: CartRepository,
  private val cartService: CartService,
) {
  @GetMapping
  fun home(): Mono<Rendering> = Mono.just(
    Rendering.view("home.html")
      .modelAttribute("items", itemRepository.findAll())
      .modelAttribute("cart", cartRepository.findById("My Cart").defaultIfEmpty(Cart("My Cart")))
      .build()
  )

  @PostMapping("/add/{id}")
  fun addToCart(@PathVariable("id") id: String): Mono<String> =
    cartService.addToCart("My Cart", id)
      .thenReturn("redirect:/")
}

@Service
class CartService (
  private val itemRepository: ItemRepository,
  private val cartRepository: CartRepository,
) {
  fun addToCartOriginal(cartId: String, id: String) =
    cartRepository.findById(cartId)
      .defaultIfEmpty(Cart(cartId))
      .flatMap { cart:Cart -> cart.cartItems.stream()
        .filter { it.item.id == id }
        .findAny()
        .map {
          it.increment()
          Mono.just(cart)
        }.orElse(
          itemRepository.findById(id)
            .map{ CartItem(it) }
            .map{
              cart.cartItems.add(it)
              cart
            }
        )
      }.flatMap { cartRepository.save(it) }

  fun addToCart(cartId: String, id: String) =
    cartRepository.findById(cartId)
      .defaultIfEmpty(Cart(cartId))
      .flatMap { cart ->
        cart.cartItems
          .firstOrNull { it.item.id == id }
          ?.let {
            it.increment()
            Mono.just(cart)
          } ?: run {
          itemRepository.findById(id)
            .map { CartItem(it) }
            .map {
              cart.cartItems.add(it)
              cart
            }
        }
      }.flatMap{ cartRepository.save(it) }
}