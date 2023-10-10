package com.jetbrains.edu.learning.command

import com.intellij.configurationStore.saveSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.system.exitProcess

suspend fun saveAndExit(exitCode: Int) {
  if (exitCode == 0) {
    ApplicationManagerEx.getApplicationEx().exit(true, true)
  }
  else {
    withContext(Dispatchers.EDT) {
      FileDocumentManager.getInstance().saveAllDocuments()
    }
    saveSettings(ApplicationManager.getApplication())
    exitProcess(exitCode)
  }
}
