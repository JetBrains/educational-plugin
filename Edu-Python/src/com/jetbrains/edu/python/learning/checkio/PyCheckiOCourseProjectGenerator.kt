package com.jetbrains.edu.python.learning.checkio

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.python.learning.PyCourseBuilder
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGenerator
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyCheckiOCourseProjectGenerator(builder: PyCourseBuilder, course: Course) : PyCourseProjectGenerator(builder, course) {

  override fun afterProjectGenerated(project: Project, projectSettings: PyNewProjectSettings) {
    super.afterProjectGenerated(project, projectSettings)
    reformatCodeInAllTaskFiles(project, course)
  }

  private fun reformatCodeInAllTaskFiles(project: Project, course: Course) {
    course.visitTasks {
      for ((_, file) in it.taskFiles) {
        val virtualFile = file.getVirtualFile(project) ?: continue
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: continue
        runInEdt {
          WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(psiFile)
          }
        }
      }
    }
  }

}
