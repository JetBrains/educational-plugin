package com.jetbrains.edu.cpp

import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.cpp.cmake.projectWizard.CLionProjectWizardUtils.getCMakeMinimumRequiredLine
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import com.jetbrains.cidr.cpp.toolchains.CMake.readCMakeVersion
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains
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

  fun initCMakeMinimumRequired() {
    CMAKE_MINIMUM_REQUIRED
  }

  companion object {
    const val EDU_MAIN_CMAKE_LIST = "EduMainCMakeList.txt"
    private const val EDU_TASK_CMAKE_LIST = "EduTaskCMakeList.txt"

    const val CMAKE_MINIMUM_REQUIRED_LINE = "CMAKE_MINIMUM_REQUIRED_LINE"
    const val CPP_STANDARD = "CPP_STANDARD"

    val CMAKE_MINIMUM_REQUIRED: String by lazy {
      getCMakeMinimumRequiredLine(readCMakeVersion(CPPToolchains.getInstance().defaultToolchain))
    }

    val EDU_TASK_CMAKE_LIST_TEMP: FileTemplate by lazy {
      getTemplate(EDU_TASK_CMAKE_LIST)
    }
  }
}