package com.jetbrains.edu.learning.courseGeneration.macro

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Provides a substitution rule wrapped into [EduMacro] for given file
 * to collapse desired project-dependent substrings in run configuration xmls during course serialization
 * and expand them during course creation.
 */
interface EduMacroProvider {

  fun provideMacro(project: Project, file: VirtualFile): EduMacro?

  data class EduMacro(val name: String, val substitution: String)

  companion object {
    val EP_NAME: ExtensionPointName<EduMacroProvider> = ExtensionPointName.create("Educational.pathMacroProvider")
  }
}
