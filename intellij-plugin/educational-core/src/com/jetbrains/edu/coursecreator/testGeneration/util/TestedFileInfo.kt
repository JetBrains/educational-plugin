package com.jetbrains.edu.coursecreator.testGeneration.util

import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.edu.learning.courseFormat.TaskFile

data class TestedFileInfo(
  val project: Project,
  val psiFile: PsiFile,
  val caret: Int,
  val language: Language,
  val selectedTaskFile: TaskFile
)
