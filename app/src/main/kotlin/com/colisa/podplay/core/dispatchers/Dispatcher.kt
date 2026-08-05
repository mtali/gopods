package com.colisa.podplay.core.dispatchers

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val goDispatcher: GoDispatchers)

enum class GoDispatchers {
  IO, Default, Main
}
