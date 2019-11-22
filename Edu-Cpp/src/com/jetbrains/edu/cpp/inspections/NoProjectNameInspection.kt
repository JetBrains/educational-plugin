package com.jetbrains.edu.cpp.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.cmake.psi.CMakeVisitor
import com.jetbrains.edu.cpp.findCMakeCommand
import com.jetbrains.edu.cpp.getCMakeProjectUniqueName
import com.jetbrains.edu.cpp.messages.EduCppBundle
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.TaskFile

class NoProjectNameInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
    object : CMakeVisitor() {
      override fun visitFile(file: PsiFile) {
        if (EduUtils.isStudentProject(file.project)) {
          return
        }

        val taskFile = EduUtils.getTaskFile(file.project, file.virtualFile) ?: return

        if (file.findCMakeCommand("project") == null) {
          holder.registerProblem(file, EduCppBundle.message("projectName.not.set.warning"), AddDefaultProjectNameFix(file, taskFile))
        }
      }
    }

  private class AddDefaultProjectNameFix(file: PsiFile, val taskFile: TaskFile) : LocalQuickFixOnPsiElement(file) {
    override fun getFamilyName(): String = "CMake"

    override fun getText(): String = EduCppBundle.message("projectName.addDefault.fix.description")

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
      val mockFile = PsiFileFactory.getInstance(project).createFileFromText(
        "mock",
        CMakeListsFileType.INSTANCE, "project(${getCMakeProjectUniqueName(taskFile.task, StudyItem::getName)})\n"
      )
      val projectCommand = mockFile.firstChild
      val cmakeMinimumRequiredCommand = file.findCMakeCommand("cmake_minimum_required")
      if (cmakeMinimumRequiredCommand != null) {
        file.addAfter(projectCommand, cmakeMinimumRequiredCommand)
      }
      else {
        file.addBefore(projectCommand, file.firstChild)
      }
    }

  }
}