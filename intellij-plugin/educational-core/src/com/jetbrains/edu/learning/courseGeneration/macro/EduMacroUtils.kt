package com.jetbrains.edu.learning.courseGeneration.macro

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.macro.EduMacroMap.SubstitutionMode
import com.jetbrains.edu.learning.courseGeneration.macro.EduMacroMap.SubstitutionMode.COLLAPSE
import com.jetbrains.edu.learning.courseGeneration.macro.EduMacroMap.SubstitutionMode.EXPAND

object EduMacroUtils {

  private val LOG: Logger = logger<EduMacroUtils>()

  fun expandMacrosForFile(holder: CourseInfoHolder<out Course?>, file: VirtualFile, fileText: String): String {
    return substitute(holder, file, fileText, EXPAND)
  }

  fun collapseMacrosForFile(holder: CourseInfoHolder<out Course?>, file: VirtualFile, fileText: String): String {
    return substitute(holder, file, fileText, COLLAPSE)
  }

  private fun substitute(holder: CourseInfoHolder<out Course?>, file: VirtualFile, fileText: String, mode: SubstitutionMode): String {
    val macros = allMacrosForFile(holder, file)
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

  private fun allMacrosForFile(holder: CourseInfoHolder<out Course?>, file: VirtualFile): List<EduMacro> {
    return EduMacroProvider.EP_NAME.extensionList.mapNotNull { it.provideMacro(holder, file) }
  }
}
