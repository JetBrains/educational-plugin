package com.jetbrains.edu.cpp

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class CppCourseBuilder : EduCourseBuilder<CppProjectSettings> {
  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<CppProjectSettings>? =
    CppCourseProjectGenerator(this, course)

  override fun getTaskTemplateName(): String = CppConfigurator.TASK_CPP
  override fun getTestTemplateName(): String = CppConfigurator.TEST_CPP

  override fun getLanguageSettings(): LanguageSettings<CppProjectSettings> = CppLanguageSettings()

  override fun createTaskContent(project: Project, task: Task, parentDirectory: VirtualFile): VirtualFile? {
    addCMakeList(task, languageSettings.settings.languageStandard)
    return super.createTaskContent(project, task, parentDirectory)
  }

  override fun refreshProject(project: Project) {
    CMakeWorkspace.getInstance(project).selectProjectDir(VfsUtil.virtualToIoFile(project.courseDir))
  }
}