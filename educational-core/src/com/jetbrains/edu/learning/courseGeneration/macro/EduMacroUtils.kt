package com.jetbrains.edu.learning.courseGeneration.macro

import com.intellij.application.options.ReplacePathToMacroMap
import com.intellij.openapi.components.ExpandMacroToPathMap
import com.intellij.openapi.components.PathMacroMap
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.vfs.VirtualFile

object EduMacroUtils {

  private val LOG: Logger = logger<EduMacroUtils>()

  fun expandPathsForFile(project: Project, file: VirtualFile, fileText: String): String {
    val macros = allMacrosForFile(project, file)
    return if (macros.isEmpty()) fileText else expandPaths(fileText, macros)
  }

  fun collapsePathsForFile(project: Project, file: VirtualFile, fileText: String): String {
    val macros = allMacrosForFile(project, file)
    return if (macros.isEmpty()) fileText else collapsePaths(fileText, macros)
  }

  private fun expandPaths(fileText: String, macros: List<EduMacroProvider.EduMacro>): String {
    val map = ExpandMacroToPathMap()
    for (macro in macros) {
      map.addMacroExpand(macro.name, macro.substitution)
    }

    return substitutePaths(fileText, map)
  }

  private fun collapsePaths(fileText: String, macros: List<EduMacroProvider.EduMacro>): String {
    val map = ReplacePathToMacroMap()
    for (macro in macros) {
      map.addMacroReplacement(macro.substitution, macro.name)
    }
    return substitutePaths(fileText, map)
  }

  private fun substitutePaths(fileText: String, map: PathMacroMap): String {
    return try {
      val element = JDOMUtil.load(fileText)
      map.substitute(element, SystemInfoRt.isFileSystemCaseSensitive)
      JDOMUtil.write(element)
    }
    catch (e: Exception) {
      LOG.error(e)
      fileText
    }
  }

  private fun allMacrosForFile(project: Project, file: VirtualFile): List<EduMacroProvider.EduMacro> {
    return EduMacroProvider.EP_NAME.extensionList.mapNotNull { it.provideMacro(project, file) }
  }
}
