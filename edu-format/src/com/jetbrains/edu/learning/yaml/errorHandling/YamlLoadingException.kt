package com.jetbrains.edu.learning.yaml.errorHandling

import com.jetbrains.edu.learning.courseFormat.message

class YamlLoadingException(override val message: String) : IllegalStateException(message)

fun loadingError(message: String): Nothing = throw YamlLoadingException(message)

fun unknownConfigMessage(configName: String) = message("yaml.editor.invalid.format.unknown.config", configName)
