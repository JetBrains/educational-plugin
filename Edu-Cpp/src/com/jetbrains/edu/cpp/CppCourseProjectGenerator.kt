package com.jetbrains.edu.cpp

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class CppCourseProjectGenerator(private val builder: CppCourseBuilder, course: Course) :
  CourseProjectGenerator<CppProjectSettings>(builder, course) {

  private val mainCMakeListsTemplate = getTemplate(builder.getMainCMakeListTemplateName())

  override fun beforeProjectGenerated(): Boolean {
    if (!super.beforeProjectGenerated()) {
      return false
    }

    fun recursiveRename(item: StudyItem) {
      changeItemNameAndCustomPresentableName(item)
      if (item is ItemContainer) {
        item.items.forEach { recursiveRename(it) }
      }
    }
    myCourse.items.forEach(::recursiveRename)

    return true
  }

  override fun createCourseStructure(project: Project, baseDir: VirtualFile, settings: CppProjectSettings) {
    val updateLesson = { section: Section?, lesson: Lesson ->
      lesson.visitTasks { task ->
        builder.addCMakeListToTask(section, lesson, task, settings.languageStandard)
      }
    }

    myCourse.items.forEach { item ->
      when (item) {
        is Section -> {
          item.visitLessons { updateLesson(item, it) }
        }
        is Lesson -> updateLesson(null, item)
      }
    }
    super.createCourseStructure(project, baseDir, settings)
  }

  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile) {
    if (baseDir.findChild(CMakeListsFileType.FILE_NAME) != null) return
    GeneratorUtils.createChildFile(baseDir, CMakeListsFileType.FILE_NAME,
                                   builder.generateCMakeListText(mainCMakeListsTemplate, FileUtil.sanitizeFileName(baseDir.name)))
  }

  override fun afterProjectGenerated(project: Project, projectSettings: CppProjectSettings) {
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
