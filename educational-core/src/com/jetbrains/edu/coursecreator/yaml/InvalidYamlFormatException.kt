package com.jetbrains.edu.coursecreator.yaml


class InvalidYamlFormatException(override val message: String) : IllegalStateException(message)

fun formatError(message: String): Nothing = throw InvalidYamlFormatException(message)

fun unsupportedItemTypeMessage(itemType: String, itemName: String = "item") = "Unsupported $itemName type '$itemType'"

fun unnamedItemAtMessage(position: Int) = "Unnamed item at position $position"

fun unknownFieldValueMessage(fieldName: String, value: String) = "Unknown $fieldName '$value'"

fun negativeParamNotAllowedMessage(paramName: String) = "Answer placeholders with negative $paramName not allowed"