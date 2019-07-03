package com.jetbrains.edu.cpp

import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.cpp.cmake.projectWizard.CLionProjectWizardUtils.getCMakeMinimumRequiredLine
import com.jetbrains.cidr.cpp.toolchains.CMake.readCMakeVersion
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class CppCourseBuilder : EduCourseBuilder<CppProjectSettings> {

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<CppProjectSettings>? =
    CppCourseProjectGenerator(this, course)

  override fun getTaskTemplateName(): String = CppConfigurator.TASK_CPP
  override fun getTestTemplateName(): String = CppConfigurator.TEST_CPP
  fun getMainCMakeListTemplateName(): String = EDU_MAIN_CMAKE_LIST

  override fun getLanguageSettings(): LanguageSettings<CppProjectSettings> = CppLanguageSettings()

  override fun createTaskContent(project: Project, task: Task, parentDirectory: VirtualFile): VirtualFile? {
    addCMakeList(task, languageSettings.settings.languageStandard)
    return super.createTaskContent(project, task, parentDirectory)
  }

  fun addCMakeList(task: Task, cppStandard: String?): TaskFile {
    val lesson = task.lesson
    val section = lesson.section
    val cMakeListFile = TaskFile()

    cMakeListFile.apply {
      name = CMakeListsFileType.FILE_NAME
      isVisible = false
      setText(generateCMakeListText(
        taskCMakeListsTemplate,
        generateCMakeProjectUniqueName(section, lesson, task),
        cppStandard
      ))
    }
    task.addTaskFile(cMakeListFile)

    return cMakeListFile
  }

  private fun generateCMakeProjectUniqueName(section: Section?, lesson: Lesson, task: Task): String {
    val sectionPart = section?.let { generateDefaultName(it) } ?: "global"
    val lessonPart = generateDefaultName(lesson)
    val taskPart = generateDefaultName(task)

    return "$sectionPart-$lessonPart-$taskPart"
  }

  fun generateCMakeListText(templateName: FileTemplate, cppProjectName: String, cppStandard: String? = null): String {
    val params = mapOf(EduNames.PROJECT_NAME to cppProjectName,
                       CMAKE_MINIMUM_REQUIRED_LINE to cMakeMinimumRequiredLine,
                       CPP_STANDARD to cppStandard).filterValues { it != null }
    return templateName.getText(params)
  }

  private val cMakeMinimumRequiredLine: String by lazy {
    getCMakeMinimumRequiredLine(readCMakeVersion(CPPToolchains.getInstance().defaultToolchain))
  }

  fun initCMakeMinimumRequiredLine() {
    cMakeMinimumRequiredLine
  }

  private val taskCMakeListsTemplate = getTemplate(EDU_TASK_CMAKE_LIST)

  companion object {
    private const val EDU_MAIN_CMAKE_LIST = "EduMainCMakeList.txt"
    private const val EDU_TASK_CMAKE_LIST = "EduTaskCMakeList.txt"

    private const val CMAKE_MINIMUM_REQUIRED_LINE = "CMAKE_MINIMUM_REQUIRED_LINE"
    private const val CPP_STANDARD = "CPP_STANDARD"
  }
}