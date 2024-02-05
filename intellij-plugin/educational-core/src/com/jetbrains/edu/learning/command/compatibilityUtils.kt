package com.jetbrains.edu.learning.command

import com.intellij.openapi.application.ex.ApplicationManagerEx

// BACKCOMPAT: 2023.1. Inline it
suspend fun saveAndExit(exitCode: Int) {
  ApplicationManagerEx.getApplicationEx().exit(true, true, exitCode)
}
