package com.jetbrains.edu.cpp

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import java.io.IOException

class CppCourseProjectGenerator(private val builder: CppCourseBuilder, course: Course) :
  CourseProjectGenerator<CppProjectSettings>(builder, course) {

  private val mainCMakeListsTemplate = getTemplate(builder.getMainCMakeListTemplateName())

  override fun beforeProjectGenerated(): Boolean {
    if (!super.beforeProjectGenerated()) {
      return false
    }

    fun deepRename(item: StudyItem) {
      changeItemNameAndCustomPresentableName(item)
      if (item is ItemContainer) {
        item.items.forEach { deepRename(it) }
      }
    }
    myCourse.items.forEach(::deepRename)

    return true
  }

  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile) {
    if (baseDir.findChild(CMakeListsFileType.FILE_NAME) != null) return
    GeneratorUtils.createChildFile(baseDir, CMakeListsFileType.FILE_NAME,
                                   builder.generateCMakeListText(mainCMakeListsTemplate, FileUtil.sanitizeFileName(baseDir.name)))
  }

  override fun createCourseStructure(project: Project, baseDir: VirtualFile, settings: CppProjectSettings) {
    ProgressManager.getInstance().runProcessWithProgressSynchronously<Any, IOException>(
      { builder.initCMakeMinimumRequiredLine() },
      "Getting CMake Minimum Required Version",
      false,
      project)
    super.createCourseStructure(project, baseDir, settings)
  }

  override fun afterProjectGenerated(project: Project, projectSettings: CppProjectSettings) {
    fun updateTasks(item: StudyItem) {
      when (item) {
        is ItemContainer -> item.items.forEach { updateTasks(it) }
        is Task -> {
          val cMakeFile = builder.addCMakeList(item, projectSettings.languageStandard)
          GeneratorUtils.createChildFile(item.getTaskDir(project) ?: return, cMakeFile.name, cMakeFile.text)
        }
      }
    }

    myCourse.items.forEach(::updateTasks)

    super.afterProjectGenerated(project, projectSettings)

    if (!isUnitTestMode) {
      CMakeWorkspace.getInstance(project).selectProjectDir(VfsUtil.virtualToIoFile(project.courseDir))
    }
  }

  private fun changeItemNameAndCustomPresentableName(item: StudyItem) {
    // We support courses which section/lesson/task names can be in Russian,
    // which may cause problems when creating a project with non-ascii paths.
    // For example, CMake + MinGW and CLion + CMake + Cygwin does not work correctly with non-ascii symbols in project paths.
    // Therefore, we generate folder names on the disk using ascii symbols (item.name)
    // and in the course (item.customPresentableName) we show the names in the same form as in the remote course
    item.customPresentableName = item.name
    item.name = generateDefaultName(item)
  }
}
