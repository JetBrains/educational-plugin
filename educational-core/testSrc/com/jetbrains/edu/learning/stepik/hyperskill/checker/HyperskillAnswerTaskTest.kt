package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckersTestBase
import com.jetbrains.edu.learning.checker.EduCheckerFixture
import com.jetbrains.edu.learning.checker.PlaintTextCheckerFixture
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import com.jetbrains.edu.learning.courseFormat.tasks.Task

abstract class HyperskillAnswerTaskTest : CheckersTestBase<Unit>() {
  override fun createCheckerFixture(): EduCheckerFixture<Unit> = PlaintTextCheckerFixture()

  protected fun getSavedTextInFile(task: Task, fileName: String, savedText: String, project: Project): String {
    val taskFile = task.getTaskFile(fileName) ?: error("Task file with name: $fileName is absent")
    val document = taskFile.getDocument(project) ?: error(
      "Document from task file is null. File name is $fileName, task file is ${task.name}")
    runWriteAction {
      document.setText(savedText)
      FileDocumentManager.getInstance().saveDocument(document)
    }
    return document.text
  }

  protected fun testWithEnabledEnsureNewLineAtEOFSetting(testFunction: () -> Unit) {
    val initialValue = EditorSettingsExternalizable.getInstance().isEnsureNewLineAtEOF
    try {
      EditorSettingsExternalizable.getInstance().isEnsureNewLineAtEOF = true
      testFunction()
    }
    finally {
      EditorSettingsExternalizable.getInstance().isEnsureNewLineAtEOF = initialValue
    }
  }
}