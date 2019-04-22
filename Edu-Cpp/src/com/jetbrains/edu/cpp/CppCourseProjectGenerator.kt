package com.jetbrains.edu.cpp

import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.cpp.cmake.projectWizard.CLionProjectWizardUtils.getCMakeMinimumRequiredLine
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import com.jetbrains.cidr.cpp.toolchains.CMake
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class CppCourseProjectGenerator(builder: CppCourseBuilder, course: Course) :
  CourseProjectGenerator<CppProjectSettings>(builder, course) {

  private val cmakeMinimumRequired = getCMakeMinimumRequiredLine(CMake.readCMakeVersion(CPPToolchains.getInstance().defaultToolchain))
  private val taskCMakeListsTemplate = getTemplate(EDU_CMAKELISTS)
  private val mainCMakeListsTemplate = getTemplate(EDU_MAIN_CMAKELISTS)

  override fun beforeProjectGenerated(): Boolean {
    val isDone = super.beforeProjectGenerated()

    if (isDone) {
      swapItemNameAndCustomPresentableName()
      addCMakeListsToEachTaskInCourse()
    }
    return isDone
  }

  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile) {
    if (baseDir.findChild(CMakeListsFileType.FILE_NAME) != null) return
    GeneratorUtils.createChildFile(baseDir, CMakeListsFileType.FILE_NAME, getText(mainCMakeListsTemplate, baseDir.name))
  }

  override fun afterProjectGenerated(project: Project, projectSettings: CppProjectSettings) {
    super.afterProjectGenerated(project, projectSettings)

    if (!isUnitTestMode) {
      CMakeWorkspace.getInstance(project).selectProjectDir(VfsUtil.virtualToIoFile(project.courseDir))
    }
  }

  private fun addCMakeListsToEachTaskInCourse() {
    for (item in myCourse.items) {
      if (item is Lesson) {
        addCMakeListsForEachTaskInLesson(null, item)
      }
      else if (item is Section) {
        item.visitLessons { lesson ->
          addCMakeListsForEachTaskInLesson(item, lesson)
          true
        }
      }
    }
  }

  private fun swapItemNameAndCustomPresentableName() {
    for (item in myCourse.items) {
      if (item is Lesson) {
        swapNames(item, EduNames.LESSON)
      }
      else if (item is Section) {
        swapNames(item, EduNames.SECTION)
        item.visitLessons { lesson ->
          swapNames(lesson, EduNames.LESSON)
          true
        }
      }
    }
  }

  private fun swapNames(item: StudyItem, prefix: String) {
    item.customPresentableName = item.name
    item.name = "${prefix}${item.index}"
  }

  private fun addCMakeListsForEachTaskInLesson(section: Section?, lesson: Lesson) {
    lesson.visitTasks { task, _ ->
      val cMakeFile = TaskFile()
      cMakeFile.name = CMakeListsFileType.FILE_NAME
      cMakeFile.setText(getText(taskCMakeListsTemplate, getCMakeProjectUniqueName(section, lesson, task)))
      task.addTaskFile(cMakeFile)
      true
    }
  }

  private fun getCMakeProjectUniqueName(section: Section?, lesson: Lesson, task: Task): String {
    val projectName = "${EduNames.LESSON}${lesson.index}-${EduNames.TASK}${task.index}"
    if (section == null) return projectName
    return "${EduNames.SECTION}${section.index}-$projectName"
  }

  private fun getText(templateName: FileTemplate, cppProjectName: String): String {
    val params = mapOf(PROJECT_NAME to cppProjectName, CMAKE_MINIMUM_REQUIRED_LINE to cmakeMinimumRequired)
    return templateName.getText(params)
  }

  private fun getTemplate(templateName: String): FileTemplate {
    return FileTemplateManager.getDefaultInstance().findInternalTemplate(templateName)
  }

  companion object {
    private const val EDU_MAIN_CMAKELISTS = "EduMainCMakeLists.txt"
    private const val EDU_CMAKELISTS = "EduCMakeLists.txt"
    private const val PROJECT_NAME = "PROJECT_NAME"
    private const val CMAKE_MINIMUM_REQUIRED_LINE = "CMAKE_MINIMUM_REQUIRED_LINE"
  }
}
