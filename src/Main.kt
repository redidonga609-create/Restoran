import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
data class Order(
    val id: String,
    val dishName: String,
    var cookingProgress: Int,
    var status: String
)

data class DishResult(
    val orderId: String,
    val dishName: String,
    val qualityScore: Double
)
suspend fun prepareIngredient(ingredientName: String): String {
    return withContext(Dispatchers.Default) {
        delay(1500L)
        val state = when (ingredientName.lowercase()) {
            "лук" -> "Нарезанный лук"
            "масло" -> "Растопленное масло"
            "сливки" -> "Взбитые сливки"
            "чеснок" -> "Измельчённый чеснок"
            "сыр" -> "Тёртый сыр"
            else -> "Подготовленный $ingredientName"
        }
        state
    }
}
suspend fun cookOrder(order: Order) = coroutineScope {
    launch { while (order.cookingProgress != 100){
            delay(500L)
            order.cookingProgress +=10;
            println("Процент приготовления блюда: ${order.cookingProgress}")
        }
    }
    launch {
        delay(2000)
        println("Идеальная температура")
    }
    launch {
        if (order.cookingProgress == 80){
            println("Блюдо почти готово, пора на финишную обработку")
        }
    }
}

suspend fun checkDishQuality(orderId: String): Deferred<DishResult> = coroutineScope {
    async{ delay(3000)
        DishResult(orderId, "Салат Цезарь", 4.9)
    }
}

suspend fun cancelOrder(order: Order, job: Job){
    delay(2000)
    job.cancel()
    job.join()
    println("Заказ ${order.id} отменён, блюдо ${order.dishName} снято с приготовления")
    println()
}
suspend fun cookWithExceptions() = supervisorScope {
    // Обычная задача
    supervisorScope {
        launch {
            println("Задача 1: Начинаем готовить салат.")
            delay(1000)
            println("Задача 1: Салат готов.")
        }

        // Задача с исключением (подгорание)
        launch {
            println("Задача 2: Начинаем жарить стейк.")
            delay(1500)
            throw RuntimeException("🔥 Блюде подгорело! Исключение в задаче 2.")
        }

        // Еще одна обычная задача
        launch {
            println("Задача 3: Начинаем варить суп.")
            delay(2000)
            println("Задача 3: Суп готов.")
        }
    }
}
suspend fun chenel() = coroutineScope {
    val dishChannel = Channel<DishResult>()


    val producer = launch{
        val dishes = listOf(
            DishResult("ORD-101", "Салат Цезарь", 4.8),
            DishResult("ORD-102", "Стейк Рибай", 5.0),
            DishResult("ORD-103", "Суп Том Ям", 4.5),
            DishResult("ORD-104", "Паста Карбонара", 4.9),
            DishResult("ORD-105", "Тирамису", 4.7)
        )

        for (dish in dishes) {
            delay(5000)
            println("[Кухня] Отправляет на раздачу: ${dish.dishName} (Заказ ${dish.orderId})")


            dishChannel.send(dish)
        }
        println("[Кухня] Все 5 блюда готовы. Закрываем канал раздачи.")
        dishChannel.close()
    }
    val consumer = launch{
        println("[Метрдотель] Готов принимать блюда с кухни...")

        // Цикл for автоматически читает из канала, пока он не закроется
        for (result in dishChannel) {
            println("[Метрдотель] Выдал заказ ${result.orderId}: ${result.dishName} | Оценка: ${result.qualityScore}")
        }

        // Этот код выполнится только после того, как в канале вызовут close()
        println("[Метрдотель] Раздача пуста, канал закрыт. Сводка по выдаче завершена.")
    }
}
suspend fun main(): Unit = coroutineScope{
    val onion = prepareIngredient("лук")
    println("Ингредиент готов: $onion\n")

    println("Готовка и Отмена заказа ---")
    val order1 = Order("001", "Стейк Рибай", 0, "cooking")

    // Запускаем готовку и отмену параллельно
    val cookingJob = cookOrder(order1)
    val cancelJob = launch { cancelOrder(order1, cookingJob) }

    // Ждем отмены
    cancelJob.join()
    println("Статус заказа после отмены: ${order1.status}\n")


    println("Проверка качества шефом ---")
    val deferredResult = checkDishQuality("002")
    println("Шеф начал дегустацию... (ждем 3 сек)")
    val result = deferredResult.await()
    println("Оценка качества от шефа для ${result.dishName}: ${result.qualityScore}\n")

    chenel()

    cookWithExceptions()
}
