package com.jetbrains.edu.coursecreator.yaml

class YamlLoadingException(override val message: String) : IllegalStateException(message)

fun loadingError(message: String): Nothing = throw YamlLoadingException(message)

fun noDirForItemMessage(name: String, itemTypeName: String = "item") = "Directory for $itemTypeName not found '$name'"

fun unknownConfigMessage(configName: String) = "Unknown config file '$configName'"

fun unexpectedItemTypeMessage(itemType: String) = "Unexpected item type '$itemType'"

fun notFoundMessage(notFoundObjectName: String, itemName: String) = "${notFoundObjectName} not found for '$itemName'"