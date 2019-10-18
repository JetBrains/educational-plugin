package com.jetbrains.edu.cpp

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.cmake.psi.CMakeArgument
import com.jetbrains.cmake.psi.CMakeCommand
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.StudyItemType
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class CppCourseBuilder(
  override val taskTemplateName: String,
  override val testTemplateName: String
) : EduCourseBuilder<CppProjectSettings> {
  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<CppProjectSettings>? =
    CppCourseProjectGenerator(this, course)

  override fun createDefaultTestFile(task: Task): TaskFile? =
    super.createDefaultTestFile(task)?.apply { name = GeneratorUtils.joinPaths(EduNames.TEST, CppBaseConfigurator.TEST_CPP) }

  override fun getLanguageSettings(): LanguageSettings<CppProjectSettings> = CppLanguageSettings()

  override fun initNewTask(project: Project, lesson: Lesson, task: Task, info: NewStudyItemInfo) {
    super.initNewTask(project, lesson, task, info)

    val cMakeProjectName = getCMakeProjectUniqueName(task) { FileUtil.sanitizeFileName(it.name, true) }
    task.addCMakeList(cMakeProjectName, getLanguageSettings().settings.languageStandard)
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
    val cMakeCommands = PsiTreeUtil.findChildrenOfType(psiFile, CMakeCommand::class.java)

    val projectCommand = cMakeCommands.first { it.name == "project" }
    val newProjectName = getCMakeProjectUniqueName(newTask) { FileUtil.sanitizeFileName(it.name, true) }
    replaceFirstArgument(psiFile, projectCommand, false) { newProjectName }

    val targets = cMakeCommands.filter { it.name == "add_executable" }

    targets.forEachIndexed { index, cMakeCommand ->
      replaceFirstArgument(psiFile, cMakeCommand, true) { targetName ->
        val suffix = when {
          targetName.endsWith("-${RUN_SUFFIX}") -> RUN_SUFFIX
          targetName.endsWith("-${TEST_SUFFIX}") -> TEST_SUFFIX
          else -> "target${index + 1}"
        }
        "$newProjectName-$suffix"
      }
    }

    return psiFile.text
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

  override fun refreshProject(project: Project) {
    // if it is a new project it will be initialized, else it will be reloaded only.
    CMakeWorkspace.getInstance(project).selectProjectDir(VfsUtil.virtualToIoFile(project.courseDir))
  }

  override fun validateItemName(name: String, itemType: StudyItemType): String? =
    if (name.matches(STUDY_ITEM_NAME_PATTERN)) null else "Name should contain only latin letters, digits, spaces or '_' symbols."

  companion object {
    private val LOG: Logger = Logger.getInstance(CppCourseBuilder::class.java)

    private const val RUN_SUFFIX = "run"
    private const val TEST_SUFFIX = "test"

    private val STUDY_ITEM_NAME_PATTERN = "[a-zA-Z0-9_ ]+".toRegex()
  }
}