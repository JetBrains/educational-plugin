package com.jetbrains.edu.cpp

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vcs.VcsConfiguration
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.stepik.course.StepikCourse

class CppCourseProjectGenerator(builder: CppCourseBuilder, course: Course) :
  CourseProjectGenerator<CppProjectSettings>(builder, course) {

  private fun deepRename(item: StudyItem) {
    changeItemNameAndCustomPresentableName(item)
    if (item is ItemContainer) {
      item.items.forEach { deepRename(it) }
    }
  }

  override fun beforeProjectGenerated(): Boolean {
    if (!super.beforeProjectGenerated()) {
      return false
    }

    if (myCourse is StepikCourse) {
      myCourse.items.forEach(::deepRename)
    }

    return true
  }

  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile) {
    if (baseDir.findChild(CMakeListsFileType.FILE_NAME) != null) return

    val dataProvider = { key: String ->
      when (key) {
        CppTemplates.CMAKE_MINIMUM_REQUIRED_LINE_KAY -> cMakeMinimumRequired
        CppTemplates.PROJECT_NAME_KEY -> FileUtil.sanitizeFileName(baseDir.name)
        CppTemplates.GTEST_VERSION_KEY -> CppConfigurator.GTEST_VERSION
        else -> ""
      }
    }

    val mainCMakeTemplateInfo = getCppTemplates(myCourse).mainCMakeList
    GeneratorUtils.createChildFile(baseDir, mainCMakeTemplateInfo.fileName, mainCMakeTemplateInfo.getText(dataProvider))

    getCppTemplates(myCourse).extraTopLevelFiles.forEach { templateInfo ->
      GeneratorUtils.createChildFile(baseDir, templateInfo.fileName, templateInfo.getText(dataProvider))
    }
  }

  private fun addCMakeListToTasks(item: StudyItem, project: Project, projectSettings: CppProjectSettings) {
    when (item) {
      is ItemContainer -> item.items.forEach { addCMakeListToTasks(it, project, projectSettings) }
      is Task -> {
        val cMakeFile = item.addCMakeList(getCMakeProjectUniqueName(item), projectSettings.languageStandard)
        GeneratorUtils.createChildFile(item.getTaskDir(project) ?: return, cMakeFile.name, cMakeFile.text)
      }
    }
  }

  override fun afterProjectGenerated(project: Project, projectSettings: CppProjectSettings) {
    if (myCourse is StepikCourse) {
      myCourse.items.forEach { addCMakeListToTasks(it, project, projectSettings) }
    }

    val googleTestSrc = FileUtil.join(myCourse.getDir(project).path, "googletest", "googletest-src")
    VcsConfiguration.getInstance(project).addIgnoredUnregisteredRoots(listOf(googleTestSrc))

    super.afterProjectGenerated(project, projectSettings)
  }

  private fun changeItemNameAndCustomPresentableName(item: StudyItem) {
    // We support courses, which section/lesson/task names can be in Russian,
    // which may cause problems when creating a project with non-ascii paths.
    // For example, CMake + MinGW and CLion + CMake + Cygwin does not work correctly with non-ascii symbols in project paths.
    // Therefore, we generate folder names on the disk using ascii symbols (item.name)
    // and in the course (item.customPresentableName) we show the names in the same form as in the remote course
    item.customPresentableName = item.name
    item.name = getDefaultName(item)
  }
}
