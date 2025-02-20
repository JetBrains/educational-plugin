package com.jetbrains.edu.learning.yaml.errorHandling

import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.message

class YamlLoadingException(override val message: String) : IllegalStateException(message)
class RemoteYamlLoadingException(val item: StudyItem, cause: Throwable) : IllegalStateException(cause)

fun loadingError(message: String): Nothing = throw YamlLoadingException(message)

fun noDirForItemMessage(name: String, itemTypeName: String = EduFormatNames.ITEM): String =
  message("yaml.editor.invalid.format.no.dir", name, itemTypeName)

fun unknownConfigMessage(configName: String): String = message("yaml.editor.invalid.format.unknown.config", configName)

fun unexpectedItemTypeMessage(itemType: String): String = message("yaml.editor.invalid.format.unexpected.type", itemType)