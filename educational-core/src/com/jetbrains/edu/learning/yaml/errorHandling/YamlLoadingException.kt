package com.jetbrains.edu.learning.yaml.errorHandling

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.messages.EduCoreBundle

fun noDirForItemMessage(name: String, itemTypeName: String = EduNames.ITEM) = EduCoreBundle.message("yaml.editor.invalid.format.no.dir",
                                                                                                    name, itemTypeName)

fun unknownConfigMessage(configName: String) = EduCoreBundle.message("yaml.editor.invalid.format.unknown.config", configName)

fun unexpectedItemTypeMessage(itemType: String) = EduCoreBundle.message("yaml.editor.invalid.format.unexpected.type", itemType)