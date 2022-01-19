package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.codeInsight.documentation.DocumentationManagerProtocol
import com.intellij.openapi.vfs.StandardFileSystems

enum class TaskDescriptionLinkProtocol(val protocol: String) {
  COURSE("course://"),
  FILE(StandardFileSystems.FILE_PROTOCOL_PREFIX),
  PSI_ELEMENT(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL);

  override fun toString(): String = protocol
}
