package com.jetbrains.edu.cpp

import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.cpp.cmake.projectWizard.CLionProjectWizardUtils.getCMakeMinimumRequiredLine
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import com.jetbrains.cidr.cpp.toolchains.CMake.readCMakeVersion
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduNames.PROJECT_NAME
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class CppCourseProjectGenerator(builder: CppCourseBuilder, course: Course) :
  CourseProjectGenerator<CppProjectSettings>(builder, course) {

  private var cmakeMinimumRequired: String? = null
  private var cmakeMinimumRequiredFuture = ApplicationManager.getApplication().executeOnPooledThread {
    cmakeMinimumRequired = getCMakeMinimumRequiredLine(readCMakeVersion(CPPToolchains.getInstance().defaultToolchain))
  }
  private val taskCMakeListsTemplate = getTemplate(EDU_CMAKELISTS)
  private val mainCMakeListsTemplate = getTemplate(EDU_MAIN_CMAKELISTS)

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

  override fun createProject(locationString: String, projectSettings: CppProjectSettings): Project? {
    val updateLesson = { section: Section?, lesson: Lesson ->
      lesson.visitTasks { task ->
        addCMakeListsForTask(section, lesson, task, projectSettings.languageStandard)
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

    return super.createProject(locationString, projectSettings)
  }

  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile) {
    if (baseDir.findChild(CMakeListsFileType.FILE_NAME) != null) return
    GeneratorUtils.createChildFile(baseDir, CMakeListsFileType.FILE_NAME,
                                   getText(mainCMakeListsTemplate, FileUtil.sanitizeFileName(baseDir.name)))
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

  private fun generateDefaultName(item: StudyItem) = when (item) {
    is Section -> "${EduNames.SECTION}${item.index}"
    is Lesson -> "${EduNames.LESSON}${item.index}"
    is Task -> "${EduNames.TASK}${item.index}"
    else -> "NonCommonStudyItem"
  }

  private fun addCMakeListsForTask(section: Section?, lesson: Lesson, task: Task, standard: String) {
    val cMakeFile = TaskFile()
    cMakeFile.apply {
      name = CMakeListsFileType.FILE_NAME
      setText(getText(taskCMakeListsTemplate, getCMakeProjectUniqueName(section, lesson, task), standard))
      isVisible = false
    }
    task.addTaskFile(cMakeFile)
  }

  private fun getCMakeProjectUniqueName(section: Section?, lesson: Lesson, task: Task): String {
    val sectionPart = section?.let { generateDefaultName(it) } ?: "global"
    val lessonPart = generateDefaultName(lesson)
    val taskPart = generateDefaultName(task)

    return "$sectionPart-$lessonPart-$taskPart"
  }

  private fun getText(templateName: FileTemplate, cppProjectName: String, cppStandard: String? = null): String {
    cmakeMinimumRequiredFuture.get()
    val params = mapOf(PROJECT_NAME to cppProjectName,
                       CMAKE_MINIMUM_REQUIRED_LINE to cmakeMinimumRequired,
                       CPP_STANDARD to cppStandard).filterValues { it != null }
    return templateName.getText(params)
  }

  private fun getTemplate(templateName: String): FileTemplate {
    return FileTemplateManager.getDefaultInstance().findInternalTemplate(templateName)
  }

  companion object {
    private const val EDU_MAIN_CMAKELISTS = "EduMainCMakeLists.txt"
    private const val EDU_CMAKELISTS = "EduCMakeLists.txt"
    private const val CMAKE_MINIMUM_REQUIRED_LINE = "CMAKE_MINIMUM_REQUIRED_LINE"
    private const val CPP_STANDARD = "CPP_STANDARD"
  }
}
