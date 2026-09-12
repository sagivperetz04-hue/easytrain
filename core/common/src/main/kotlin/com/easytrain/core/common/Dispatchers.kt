package com.easytrain.core.common

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(
    val dispatcher: EasyTrainDispatcher,
)

enum class EasyTrainDispatcher {
    IO,
    Default,
}
