package com.jetbrains.edu.learning.courseGeneration

import com.intellij.application.options.ReplacePathToMacroMap
import com.intellij.openapi.components.ExpandMacroToPathMap
import com.intellij.openapi.components.PathMacroMap
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.getTaskDir
import com.jetbrains.edu.learning.isTaskRunConfigurationFile
import org.jetbrains.jps.model.serialization.PathMacroUtil

object EduPathMacroUtils {

  private val LOG: Logger = logger<EduPathMacroUtils>()

  private const val TASK_DIR_MACRO_NAME = "TASK_DIR"

  fun expandPathsForFile(project: Project, file: VirtualFile, text: String): String {
    return if (file.needToSubstitutePaths(project)) {
      expandPaths(text, taskRelativePath(project, file))
    }
    else {
      text
    }
  }

  fun collapsePathsForFile(project: Project, file: VirtualFile, text: String): String {
    return if (file.needToSubstitutePaths(project)) {
      collapsePaths(text, taskRelativePath(project, file))
    }
    else {
      text
    }
  }

  private fun VirtualFile.needToSubstitutePaths(project: Project): Boolean = isTaskRunConfigurationFile(project)

  private fun taskRelativePath(project: Project, file: VirtualFile): String {
    val taskDir = file.getTaskDir(project)!!
    val courseDir = project.courseDir
    return FileUtil.getRelativePath(courseDir.path, taskDir.path, VfsUtilCore.VFS_SEPARATOR_CHAR)!!
  }

  private fun expandPaths(text: String, taskRelativePath: String): String {
    val map = ExpandMacroToPathMap()
    map.addMacroExpand(TASK_DIR_MACRO_NAME, "\$${PathMacroUtil.PROJECT_DIR_MACRO_NAME}\$/$taskRelativePath")
    return substitutePaths(text, map)
  }

  private fun collapsePaths(text: String, taskRelativePath: String): String {
    val map = ReplacePathToMacroMap()
    map.addMacroReplacement("\$${PathMacroUtil.PROJECT_DIR_MACRO_NAME}\$/$taskRelativePath", TASK_DIR_MACRO_NAME)
    return substitutePaths(text, map)
  }

  private fun substitutePaths(text: String, map: PathMacroMap): String {
    return try {
      val element = JDOMUtil.load(text)
      map.substitute(element, SystemInfoRt.isFileSystemCaseSensitive)
      JDOMUtil.write(element)
    }
    catch (e: Exception) {
      LOG.error(e)
      text
    }
  }
}
