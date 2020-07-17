@file:JvmName("CodeTaskHelper")
package com.jetbrains.edu.jvm.stepik

import com.intellij.lang.Language
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.jetbrains.annotations.NotNull

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
        if ((isPublic(aClass) || fileName == className) && className != null) {
          fileName = className
          break
        }
      }
    }
  }

  return "$fileName.${fileType.defaultExtension}"
}

fun findCodeTaskFile(project: Project, task: Task, mainClassForFile: (Project, VirtualFile) -> String?): TaskFile? {
  for ((_, file) in task.taskFiles) {
    val virtualFile = file.getVirtualFile(project) ?: continue
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile) as? PsiClassOwner ?: continue
    for (aClass in psiFile.classes) {
      if (isPublic(aClass) && mainClassForFile(project, virtualFile) != null) {
        return file
      }
    }
  }
  return null
}

private fun isPublic(aClass: @NotNull PsiClass) = aClass.hasModifierProperty(PsiModifier.PUBLIC)
