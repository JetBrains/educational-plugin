package com.jetbrains.edu.cpp

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.cmake.psi.CMakeArgument
import com.jetbrains.cmake.psi.CMakeCommand
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.ui.AdditionalPanel
import com.jetbrains.edu.coursecreator.ui.showNewStudyItemDialog
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class CppCourseBuilder : EduCourseBuilder<CppProjectSettings> {
  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<CppProjectSettings>? =
    CppCourseProjectGenerator(this, course)

  override fun getTaskTemplateName(): String = CppConfigurator.TASK_CPP
  override fun getTestTemplateName(): String = CppConfigurator.TEST_CPP

  override fun getLanguageSettings(): LanguageSettings<CppProjectSettings> = CppLanguageSettings()

  override fun initNewTask(project: Project, lesson: Lesson, task: Task, info: NewStudyItemInfo) {
    super.initNewTask(project, lesson, task, info)

    val cMakeProjectName = getCMakeProjectUniqueName(task) { FileUtil.sanitizeFileName(it.name, true) }
    addCMakeList(task, cMakeProjectName, languageSettings.settings.languageStandard)

    val mainName = GeneratorUtils.joinPaths(task.sourceDir, EDU_RUN_CPP)
    val mainText = GeneratorUtils.getInternalTemplateText(EDU_RUN_CPP)
    task.addTaskFile(TaskFile(mainName, mainText))
  }

  override fun afterFrameworkTaskCopy(project: Project, sourceTask: Task, newTask: Task) {
    val sourceCMake = sourceTask.taskFiles[CMakeListsFileType.FILE_NAME] ?: return
    val virtualFile = sourceCMake.getVirtualFile(project)
    if (virtualFile == null) {
      LOG.warn("Cannot get a virtual file from the '${CMakeListsFileType.FILE_NAME}' task file")
      return
    }

    val psiFile = PsiUtilBase.getPsiFile(project, virtualFile).copy() as PsiFile
    val cMakeCommands = PsiTreeUtil.findChildrenOfType(psiFile, CMakeCommand::class.java)

    val projectCommand = cMakeCommands.first { it.name == "project" }
    val newProjectName = getCMakeProjectUniqueName(newTask) { FileUtil.sanitizeFileName(it.name, true) }
    replaceFirstArgument(psiFile, projectCommand, false) { it }

    val targets = cMakeCommands.filter { it.name == "add_executable" }
    targets.forEachIndexed { index, cMakeCommand ->
      replaceFirstArgument(psiFile, cMakeCommand, true) { targetName ->
        val suffix = when {
          targetName.endsWith("-src") -> "src"
          targetName.endsWith("-test") -> "test"
          else -> "target${index + 1}"
        }
        "$newProjectName-$suffix"
      }
    }

    newTask.taskFiles[CMakeListsFileType.FILE_NAME]?.setText(psiFile.text)
  }

  private fun replaceFirstArgument(
    psiFile: PsiFile,
    command: CMakeCommand,
    needReplaceOccurrences: Boolean,
    newNameGenerator: (String) -> String
  ) {
    val cMakeCommandArguments = command.cMakeCommandArguments ?: return
    val argument = cMakeCommandArguments.cMakeArgumentList.firstOrNull() ?: return
    val argumentName = argument.name ?: return

    val replaceTo = newNameGenerator(argumentName)

    if (needReplaceOccurrences) {
      PsiTreeUtil.findChildrenOfType(psiFile, CMakeArgument::class.java)
        .filter { it.name == argument.name }
        .forEach { it.setName(replaceTo) }
    }
    else {
      argument.setName(replaceTo)
    }
  }

  override fun showNewStudyItemUi(
    project: Project,
    model: NewStudyItemUiModel,
    additionalPanels: MutableList<AdditionalPanel>
  ): NewStudyItemInfo? {
    return showNewStudyItemDialog(project, model, additionalPanels, ::CppNewStudyItemDialog)
  }

  override fun refreshProject(project: Project) {
    // if it is a new project it will be initialized, else it will be reloaded only.
    CMakeWorkspace.getInstance(project).selectProjectDir(VfsUtil.virtualToIoFile(project.courseDir))
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(CppCourseBuilder::class.java)

    private const val EDU_RUN_CPP = "run.cpp"
  }
}