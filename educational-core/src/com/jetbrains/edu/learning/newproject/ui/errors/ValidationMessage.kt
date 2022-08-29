package com.jetbrains.edu.learning.newproject.ui.errors

import org.jetbrains.annotations.Nls

data class ValidationMessage @JvmOverloads constructor(
  @Nls val message: String,
  val hyperlinkAddress: String? = null,
  val type: ValidationMessageType = ValidationMessageType.ERROR
)