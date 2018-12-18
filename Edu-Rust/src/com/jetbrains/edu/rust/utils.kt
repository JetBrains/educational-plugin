package com.jetbrains.edu.rust

private val INVALID_SYMBOLS = """[\s-]""".toRegex()

fun String.toPackageName(): String = replace(INVALID_SYMBOLS, "_").toLowerCase()
