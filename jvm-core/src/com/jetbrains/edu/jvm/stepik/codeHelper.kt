@file:JvmName("CodeTaskHelper")
package com.jetbrains.edu.jvm.stepik

import com.intellij.lang.Language
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiModifier

private const val MAIN_NAME = "Main"

fun fileName(language: Language, fileText: String): String {

  var fileName = MAIN_NAME
  val fileType = language.associatedFileType ?: return fileName

  runReadAction {
    val file = PsiFileFactory.getInstance(ProjectManager.getInstance().defaultProject).createFileFromText("Tmp", fileType, fileText)
    if (file is PsiClassOwner) {
      val classes = file.classes
      for (aClass in classes) {
        val className = aClass.nameIdentifier?.text ?: aClass.name
        val isPublic = aClass.hasModifierProperty(PsiModifier.PUBLIC) || fileName == className
        if (isPublic && className != null) {
          fileName = className
          break
        }
      }
    }
  }

  return fileName
}
