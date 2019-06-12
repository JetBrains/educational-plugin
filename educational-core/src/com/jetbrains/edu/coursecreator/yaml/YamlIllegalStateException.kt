package com.jetbrains.edu.coursecreator.yaml

class YamlIllegalStateException(override val message: String) : IllegalStateException(message)

fun yamlIllegalStateError(message: String): Nothing = throw YamlIllegalStateException(message)

fun noDirForItemMessage(name: String, itemTypeName: String = "item") = "Cannot find directory for $itemTypeName: '$name'"

fun unknownConfigMessage(configName: String) = "Unknown config file '$configName'"

fun unexpectedItemTypeMessage(itemType: String) = "Unexpected item type $itemType"

fun notFoundMessage(notFoundObjectName: String, itemName: String) = "Cannot find $notFoundObjectName for $itemName"

fun noItemDirMessage(itemTypeName: String, itemName: String) = "Cannot find directory for a $itemTypeName '$itemName'"