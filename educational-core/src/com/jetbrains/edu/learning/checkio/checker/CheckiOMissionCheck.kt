package com.jetbrains.edu.learning.checkio.checker

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.io.IOException
import java.util.concurrent.Callable
import javax.swing.JComponent

abstract class CheckiOMissionCheck(val project: Project, val task: Task) : Callable<CheckResult> {

  abstract override fun call(): CheckResult

  abstract fun getPanel(): JComponent

  protected fun getCodeFromTask(): String {
    val taskFile = (task as CheckiOMission).taskFile
    val missionDir = task.getDir(project)
                     ?: throw IOException("Directory is not found for mission: ${task.id}, ${task.name}")
    val virtualFile = EduUtils.findTaskFileInDir(taskFile, missionDir)
                      ?: throw IOException("Virtual file is not found for mission: ${task.id}, ${task.name}")

    val document = ApplicationManager.getApplication().runReadAction(
      Computable {
        FileDocumentManager.getInstance().getDocument(virtualFile)
      }
    ) ?: throw IOException("Document isn't provided for VirtualFile: ${virtualFile.name}")

    return document.text
  }
}
