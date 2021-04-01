package com.jetbrains.edu.learning.checker

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseGeneration.macro.EduMacro
import com.jetbrains.edu.learning.courseGeneration.macro.EduMacroProvider
import com.jetbrains.edu.learning.getTaskDir
import com.jetbrains.edu.learning.isTaskRunConfigurationFile
import org.jetbrains.jps.model.serialization.PathMacroUtil

class TaskRunConfigurationPathMacroProvider : EduMacroProvider {
  override fun provideMacro(project: Project, file: VirtualFile): EduMacro? {
    return if (file.isTaskRunConfigurationFile(project)) {
      val taskRelativePath = taskRelativePath(project, file)
      EduMacro(TASK_DIR_MACRO_NAME, "\$${PathMacroUtil.PROJECT_DIR_MACRO_NAME}\$/$taskRelativePath")
    } else {
      null
    }
  }

  private fun taskRelativePath(project: Project, file: VirtualFile): String {
    val taskDir = file.getTaskDir(project) ?: error("Can't find task directory for `$file` file")
    val courseDir = project.courseDir
    return FileUtil.getRelativePath(courseDir.path, taskDir.path, VfsUtilCore.VFS_SEPARATOR_CHAR)!!
  }

  companion object {
    private const val TASK_DIR_MACRO_NAME = "TASK_DIR"
  }
}
