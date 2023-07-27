package com.jetbrains.edu.cpp

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilBase
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.edu.coursecreator.StudyItemType
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemInfo
import com.jetbrains.edu.cpp.messages.EduCppBundle
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class CppGTestCourseBuilder : CppCourseBuilder() {
  override fun testTemplateName(course: Course): String = TEST_TEMPLATE_NAME

  companion object {
    @VisibleForTesting
    const val TEST_TEMPLATE_NAME = "gtest.test.cpp"
  }
}

class CppCatchCourseBuilder : CppCourseBuilder() {
  override fun testTemplateName(course: Course): String = TEST_TEMPLATE_NAME

  companion object {
    @VisibleForTesting
    const val TEST_TEMPLATE_NAME = "catch.test.cpp"
  }
}

open class CppCourseBuilder : EduCourseBuilder<CppProjectSettings> {
  override fun taskTemplateName(course: Course): String = CppConfigurator.TASK_CPP
  override fun mainTemplateName(course: Course): String = CppConfigurator.MAIN_CPP
  override fun testTemplateName(course: Course): String = CppConfigurator.TEST_CPP

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<CppProjectSettings>? =
    CppCourseProjectGenerator(this, course)

  override fun getLanguageSettings(): LanguageSettings<CppProjectSettings> = CppLanguageSettings()

  override fun getDefaultSettings(): Result<CppProjectSettings, String> = Ok(CppProjectSettings())

  override fun getSupportedLanguageVersions(): List<String> = getLanguageVersions()

  override fun initNewTask(course: Course, task: Task, info: NewStudyItemInfo, withSources: Boolean) {
    super.initNewTask(course, task, info, withSources)
    if (withSources) {
      val cMakeProjectName = getCMakeProjectName(task)
      task.addCMakeList(cMakeProjectName, getLanguageSettings().getSettings().languageStandard)
    }
  }

  override fun getTextForNewTask(taskFile: TaskFile, taskDir: VirtualFile, newTask: Task): String? {
    if (taskFile.name != CMakeListsFileType.FILE_NAME) {
      return super.getTextForNewTask(taskFile, taskDir, newTask)
    }

    val project = newTask.project
    if (project == null) {
      LOG.warn("Cannot get project by the task `${newTask.name}`")
      return null
    }

    val virtualFile = taskFile.getVirtualFile(project)
    if (virtualFile == null) {
      LOG.warn("Cannot get a virtual file from the '${CMakeListsFileType.FILE_NAME}' task file")
      return null
    }

    val psiFile = PsiUtilBase.getPsiFile(project, virtualFile).copy() as PsiFile
    val projectCommand = psiFile.findCMakeCommand("project")

    if (projectCommand != null) {
      val newProjectName = getCMakeProjectName(newTask)
      run {
        val cMakeCommandArguments = projectCommand.cMakeCommandArguments ?: return@run
        val firstArgument = cMakeCommandArguments.cMakeArgumentList.firstOrNull() ?: return@run
        runWriteAction { firstArgument.setName(newProjectName) }
      }
    }

    return psiFile.text
  }

  override fun refreshProject(project: Project, cause: RefreshCause) {
    // if it is a new project it will be initialized, else it will be reloaded only.
    CMakeWorkspace.getInstance(project).selectProjectDir(VfsUtil.virtualToIoFile(project.courseDir))
  }

  override fun validateItemName(project: Project, name: String, itemType: StudyItemType): String? =
    if (name.matches(STUDY_ITEM_NAME_PATTERN)) null else EduCppBundle.message("error.invalid.name")

  companion object {
    private val LOG: Logger = Logger.getInstance(CppCourseBuilder::class.java)

    private val STUDY_ITEM_NAME_PATTERN = "[a-zA-Z0-9_ ]+".toRegex()
  }
}
