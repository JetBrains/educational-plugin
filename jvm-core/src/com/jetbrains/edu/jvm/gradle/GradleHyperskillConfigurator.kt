package com.jetbrains.edu.jvm.gradle

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiManager
import com.jetbrains.edu.jvm.MainFileProvider
import com.jetbrains.edu.jvm.stepik.isPublic
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator

abstract class GradleHyperskillConfigurator<T>(baseConfigurator: EduConfigurator<T>) : HyperskillConfigurator<T>(baseConfigurator) {
  override fun getCodeTaskFile(project: Project, task: Task): TaskFile? {
    val language = task.course.languageById ?: return super.getCodeTaskFile(project, task)
    for (file in task.taskFiles.values) {
      val virtualFile = file.getVirtualFile(project) ?: continue
      val psiFile = PsiManager.getInstance(project).findFile(virtualFile) as? PsiClassOwner ?: continue
      for (aClass in psiFile.classes) {
        if (aClass.isPublic && MainFileProvider.getMainClassName(project, virtualFile, language) != null) {
          return file
        }
      }
    }
    return super.getCodeTaskFile(project, task)
  }
}