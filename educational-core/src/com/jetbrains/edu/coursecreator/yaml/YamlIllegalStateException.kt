package com.jetbrains.edu.coursecreator.yaml

class YamlIllegalStateException(override val message: String) : IllegalStateException(message)

fun yamlIllegalStateError(message: String): Nothing = throw YamlIllegalStateException(message)

fun noDirForItemMessage(name: String, itemTypeName: String = "item") = "Directory for $itemTypeName not found: '$name'"

fun unknownConfigMessage(configName: String) = "Unknown config file '$configName'"

fun unexpectedItemTypeMessage(itemType: String) = "Unexpected item type $itemType"

fun notFoundMessage(notFoundObjectName: String, itemName: String) = "${notFoundObjectName.capitalize()} not found for $itemName"