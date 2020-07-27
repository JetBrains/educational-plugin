package com.jetbrains.edu.learning.messages

import java.util.function.Supplier

// On platform 193 lazy messages not supported, so we need this hack
fun String.makeLazy(): String = this

// On platform 193 lazy messages not supported, so we need this hack
fun Supplier<String>.pass(): String = this.get()