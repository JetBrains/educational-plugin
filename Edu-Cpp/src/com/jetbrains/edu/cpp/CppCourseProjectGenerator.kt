package com.jetbrains.edu.cpp

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
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

    myCourse.items.forEach(::deepRename)

    return true
  }

  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile) {
    if (baseDir.findChild(CMakeListsFileType.FILE_NAME) != null) return

    val mainCMakeText = GeneratorUtils.getInternalTemplateText(getCppParameters(myCourse).mainCMakeList,
                                                               getCMakeTemplateVariables(FileUtil.sanitizeFileName(baseDir.name)))

    GeneratorUtils.createChildFile(baseDir, CMakeListsFileType.FILE_NAME, mainCMakeText)

    if (myCourse !is StepikCourse) {
      val initCMakeText = GeneratorUtils.getInternalTemplateText(getCppParameters(myCourse).initCMakeList,
                                                                 getCMakeTemplateVariables(gtestVersion = CppConfigurator.GTEST_VERSION))

      GeneratorUtils.createChildFile(baseDir, "${CMakeListsFileType.FILE_NAME}.in", initCMakeText)
    }
  }

  private fun updateTasks(item: StudyItem, project: Project, projectSettings: CppProjectSettings) {
    when (item) {
      is ItemContainer -> item.items.forEach { updateTasks(it, project, projectSettings) }
      is Task -> {
        val cMakeFile = addCMakeList(item, projectSettings.languageStandard)
        GeneratorUtils.createChildFile(item.getTaskDir(project) ?: return, cMakeFile.name, cMakeFile.text)
      }
    }
  }

  override fun afterProjectGenerated(project: Project, projectSettings: CppProjectSettings) {
    myCourse.items.forEach { updateTasks(it, project, projectSettings) }

    super.afterProjectGenerated(project, projectSettings)
  }

  private fun changeItemNameAndCustomPresentableName(item: StudyItem) {
    // We support courses which section/lesson/task names can be in Russian,
    // which may cause problems when creating a project with non-ascii paths.
    // For example, CMake + MinGW and CLion + CMake + Cygwin does not work correctly with non-ascii symbols in project paths.
    // Therefore, we generate folder names on the disk using ascii symbols (item.name)
    // and in the course (item.customPresentableName) we show the names in the same form as in the remote course
    item.customPresentableName = item.name
    item.name = getDefaultName(item)
  }
}
