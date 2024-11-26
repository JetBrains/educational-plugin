package com.jetbrains.edu.cpp

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vcs.VcsConfiguration
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.stepik.StepikCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class CppCourseProjectGenerator(builder: CppCourseBuilder, course: Course) :
  CourseProjectGenerator<CppProjectSettings>(builder, course) {

  override fun autoCreatedAdditionalFiles(holder: CourseInfoHolder<Course>): List<EduFile> {
    if (holder.courseDir.findChild(CMakeListsFileType.FILE_NAME) != null) return emptyList()

    val mainCMakeTemplateInfo = getCppTemplates(course).mainCMakeList
    val sanitizedProjectName = FileUtil.sanitizeFileName(holder.courseDir.name)

    return listOf(
      EduFile(
        mainCMakeTemplateInfo.generatedFileName,
        mainCMakeTemplateInfo.getText(sanitizedProjectName, course.languageVersion ?: "")
      )
    ) +
    getCppTemplates(course).extraTopLevelFiles.map { templateInfo ->
      EduFile(templateInfo.generatedFileName, templateInfo.getText(sanitizedProjectName))
    }
  }

  private fun addCMakeListToStepikTasks(item: StudyItem, project: Project, projectSettings: CppProjectSettings) {
    when (item) {
      is ItemContainer -> item.items.forEach { addCMakeListToStepikTasks(it, project, projectSettings) }
      is Task -> {
        val cMakeFile = item.addCMakeList(getCMakeProjectName(item), projectSettings.languageStandard)
        GeneratorUtils.createChildFile(project, item.getDir(project.courseDir) ?: return, cMakeFile.name, cMakeFile.contents)
      }
    }
  }

  override fun afterProjectGenerated(project: Project, projectSettings: CppProjectSettings, onConfigurationFinished: () -> Unit) {
    if (course is StepikCourse) {
      course.items.forEach { addCMakeListToStepikTasks(it, project, projectSettings) }
    }

    val googleTestSrc = FileUtil.join(project.courseDir.path, TEST_FRAMEWORKS_BASE_DIR_VALUE, GTEST_SOURCE_DIR_VALUE)
    VcsConfiguration.getInstance(project).addIgnoredUnregisteredRoots(listOf(googleTestSrc))

    super.afterProjectGenerated(project, projectSettings, onConfigurationFinished)
  }
}
