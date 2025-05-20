package com.jetbrains.edu.ai.debugger.core.service

import com.jetbrains.educational.ml.debugger.dto.FileContentMap
import com.jetbrains.educational.ml.debugger.request.TestInfoBase

data class TestInfo(
  override val name: String,
  override val errorMessage: String,
  override val expectedOutput: String,
  override val testFiles: FileContentMap,
  override val text: String
) : TestInfoBase()