package com.jetbrains.edu.learning.courseGeneration.macro

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseGeneration.macro.EduMacroMap.SubstitutionMode
import com.jetbrains.edu.learning.courseGeneration.macro.EduMacroMap.SubstitutionMode.COLLAPSE
import com.jetbrains.edu.learning.courseGeneration.macro.EduMacroMap.SubstitutionMode.EXPAND

object EduMacroUtils {

  private val LOG: Logger = logger<EduMacroUtils>()

  fun expandMacrosForFile(project: Project, file: VirtualFile, fileText: String): String {
    return substitute(project, file, fileText, EXPAND)
  }

  fun collapseMacrosForFile(project: Project, file: VirtualFile, fileText: String): String {
    return substitute(project, file, fileText, COLLAPSE)
  }

  private fun substitute(project: Project, file: VirtualFile, fileText: String, mode: SubstitutionMode): String {
    val macros = allMacrosForFile(project, file)
    if (macros.isEmpty()) return fileText

    val map = EduMacroMap(mode, macros)
    return try {
      val element = JDOMUtil.load(fileText)
      map.substitute(element, SystemInfoRt.isFileSystemCaseSensitive, true)
      JDOMUtil.write(element)
    }
    catch (e: Exception) {
      LOG.error(e)
      fileText
    }
  }

  private fun allMacrosForFile(project: Project, file: VirtualFile): List<EduMacro> {
    return EduMacroProvider.EP_NAME.extensionList.mapNotNull { it.provideMacro(project, file) }
  }
}
