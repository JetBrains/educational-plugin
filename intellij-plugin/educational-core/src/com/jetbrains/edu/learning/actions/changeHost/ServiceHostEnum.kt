package com.jetbrains.edu.learning.actions.changeHost

import com.intellij.openapi.util.NlsContexts.ListItem

interface ServiceHostEnum {
  val url: String
  fun visibleName(): @ListItem String
}
