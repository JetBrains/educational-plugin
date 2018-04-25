package com.jetbrains.edu.coursecreator.handlers

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileSystemItem
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import java.io.IOException

class CCDescriptionFileRenameHandler : CCRenameHandler() {

  override fun isAvailable(project: Project, file: VirtualFile): Boolean {
    if (!EduUtils.isTaskDescriptionFile(file.name)) return false
    val task = EduUtils.getTaskForFile(project, file) ?: return false
    return file.parent == task.getTaskDir(project)
  }

  override fun rename(project: Project, course: Course, item: PsiFileSystemItem) {
    val task = EduUtils.getTaskForFile(project, item.virtualFile) ?: return
    val name = item.name
    val text = "Rename description file"
    val newName = Messages.showInputDialog(project, "$text '$name' to", text, null, name, object : InputValidatorEx {
      override fun checkInput(inputString: String?): Boolean = getErrorText(inputString) == null

      override fun getErrorText(inputString: String?): String? {
        return if (inputString == null || !EduUtils.isTaskDescriptionFile(inputString)) {
          "Description file should be named '${EduNames.TASK_HTML}' or '${EduNames.TASK_MD}'."
        } else {
          null
        }
      }

      override fun canClose(inputString: String?): Boolean = checkInput(inputString)
    })
    if (newName != null) {
      val format = DescriptionFormat.values().find { it.descriptionFileName == newName } ?: error("Unexpected new name: `$newName`")
      task.descriptionFormat = format

      runWriteAction {
        try {
          item.virtualFile.rename(CCRenameHandler::class.java, newName)
        } catch (e: IOException) {
          LOG.error(e)
        }
      }
    }
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(CCDescriptionFileRenameHandler::class.java)
  }
}
