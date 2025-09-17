package com.jetbrains.edu.commandLine.validation

import kotlinx.serialization.StringFormat
import kotlinx.serialization.encodeToString as platformEncodeToString

inline fun <reified T> StringFormat.encodeToString(value: T): String = platformEncodeToString(value)
