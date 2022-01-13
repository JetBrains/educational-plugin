package com.jetbrains.edu.learning.yaml.errorHandling

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.messages.EduCoreBundle


class InvalidYamlFormatException(override val message: String) : IllegalStateException(message)

fun formatError(message: String): Nothing = throw InvalidYamlFormatException(message)

fun unsupportedItemTypeMessage(itemType: String, itemName: String = EduNames.ITEM) = EduCoreBundle.message(
  "yaml.editor.invalid.format.unsupported.type", itemName, itemType)

fun unnamedItemAtMessage(position: Int) = EduCoreBundle.message("yaml.editor.invalid.format.unnamed.item", position)

fun negativeLengthNotAllowedMessage() = EduCoreBundle.message("yaml.editor.invalid.format.placeholders.negative.length")

fun negativeOffsetNotAllowedMessage() = EduCoreBundle.message("yaml.editor.invalid.format.placeholders.negative.offset")