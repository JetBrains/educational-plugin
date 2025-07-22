package com.jetbrains.edu.learning.checker

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.macro.EduMacro
import com.jetbrains.edu.learning.courseGeneration.macro.EduMacroProvider
import com.jetbrains.edu.learning.getTaskDir
import com.jetbrains.edu.learning.isTaskRunConfigurationFile
import org.jetbrains.jps.model.serialization.PathMacroUtil

class TaskRunConfigurationPathMacroProvider : EduMacroProvider {
  override fun provideMacro(holder: CourseInfoHolder<out Course?>, file: VirtualFile): EduMacro? {
    return if (file.isTaskRunConfigurationFile(holder)) {
      val taskRelativePath = taskRelativePath(holder, file)
      EduMacro(TASK_DIR_MACRO_NAME, "$${PathMacroUtil.PROJECT_DIR_MACRO_NAME}$/$taskRelativePath")
    } else {
      null
    }
  }

  private fun taskRelativePath(holder: CourseInfoHolder<out Course?>, file: VirtualFile): String {
    val taskDir = file.getTaskDir(holder) ?: error("Can't find task directory for `$file` file")
    return FileUtil.getRelativePath(holder.courseDir.path, taskDir.path, VfsUtilCore.VFS_SEPARATOR_CHAR)!!
  }

  companion object {
    private const val TASK_DIR_MACRO_NAME = "TASK_DIR"
  }
}
