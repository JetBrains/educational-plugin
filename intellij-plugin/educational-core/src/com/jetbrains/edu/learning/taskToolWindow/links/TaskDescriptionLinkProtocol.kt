package com.jetbrains.edu.learning.taskToolWindow.links

import com.intellij.codeInsight.documentation.DocumentationManagerProtocol
import com.intellij.openapi.vfs.StandardFileSystems

enum class TaskDescriptionLinkProtocol(val protocol: String) {
  COURSE("course://"),
  FILE(StandardFileSystems.FILE_PROTOCOL_PREFIX),
  PSI_ELEMENT(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL),
  TOOL_WINDOW("tool_window://"),
  SETTINGS("settings://"),
  FEATURE_TRAINER("ift://");

  override fun toString(): String = protocol
}
