package com.jetbrains.edu.learning

import kotlinx.coroutines.currentCoroutineContext
import org.jetbrains.annotations.ApiStatus


/**
 * Methods annotated with [RequiresBlockingContext] are not designed to be called in suspend context
 * (where [currentCoroutineContext] is available).
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
@ApiStatus.Experimental
annotation class RequiresBlockingContext
