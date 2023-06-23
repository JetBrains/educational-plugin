package com.jetbrains.edu.learning.yaml.errorHandling

import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.message


class InvalidYamlFormatException(override val message: String) : IllegalStateException(message)

fun formatError(message: String): Nothing = throw InvalidYamlFormatException(message)

fun unsupportedItemTypeMessage(itemType: String, itemName: String = EduFormatNames.ITEM) = message(
  "yaml.editor.invalid.format.unsupported.type", itemName, itemType)

fun unnamedItemAtMessage(position: Int) = message("yaml.editor.invalid.format.unnamed.item", position)

fun negativeLengthNotAllowedMessage() = message("yaml.editor.invalid.format.placeholders.negative.length")

fun negativeOffsetNotAllowedMessage() = message("yaml.editor.invalid.format.placeholders.negative.offset")