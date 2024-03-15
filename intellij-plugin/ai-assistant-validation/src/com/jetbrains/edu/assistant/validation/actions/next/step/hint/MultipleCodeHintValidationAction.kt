package com.jetbrains.edu.assistant.validation.actions.next.step.hint

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.psi.PsiFileFactory
import com.jetbrains.edu.assistant.validation.actions.ValidationAction
import com.jetbrains.edu.assistant.validation.messages.EduAndroidAiAssistantValidationBundle
import com.jetbrains.edu.assistant.validation.util.MultipleCodeHintDataframeRecord
import com.jetbrains.edu.kotlin.checker.KtTaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getText
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.eduAssistant.core.TaskBasedAssistant
import com.jetbrains.edu.learning.eduAssistant.inspection.InspectionProvider
import com.jetbrains.edu.learning.eduAssistant.inspection.getInspectionsWithIssues
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor
import com.jetbrains.edu.learning.eduState
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.navigation.NavigationUtils
import org.jetbrains.kotlinx.dataframe.api.toDataFrame


@Suppress("ComponentNotRegistered")
class MultipleCodeHintValidationAction : ValidationAction<MultipleCodeHintDataframeRecord>() {

  override val outputFilePrefixName: String = "multipleCodeHints"
  override val name: String = EduAndroidAiAssistantValidationBundle.message("action.multiple.code.hint.validation.action.name")

  init {
    setUpSpinnerPanel(name)
  }

  override suspend fun buildRecords(task: EduTask, lesson: Lesson): List<MultipleCodeHintDataframeRecord> {
    return getCodeFromTaskFiles(task, lesson).map {
      runBlockingCancellable {
        buildCodeHintRecords(task, it)
      }
    }.flatten()
  }

  override fun MutableList<MultipleCodeHintDataframeRecord>.convertToDataFrame() = toDataFrame()

  private suspend fun buildCodeHintRecords(task: EduTask, userCode: String): List<MultipleCodeHintDataframeRecord> {
    val taskProcessor = TaskProcessor(task)
    val assistant = TaskBasedAssistant(taskProcessor)
    val language = task.course.languageById ?: error("Language could not be determined")
    val project = task.project ?: error("Cannot get project")

    NavigationUtils.navigateToTask(project, task)
    val eduState = project.eduState ?: error("Cannot get eduState for project ${project.name}")
    val records = mutableListOf<MultipleCodeHintDataframeRecord>()

    assistant.getTaskAnalysis(task)
    task.generatedSolutionSteps ?: error("Cannot get generate solution steps for task ${task.name}")
    var currentUserCode = userCode
    val maxHintSteps = assistant.parseSteps(task.generatedSolutionSteps!!).size * 2
    for (hintIndex in 1..maxHintSteps) {
      try {
        val response = assistant.getHint(task, eduState, currentUserCode)
        if (response.codeHint == null) {
          error("Code hint is empty")
        }
        currentUserCode = taskProcessor.applyCodeHint(response.codeHint!!, eduState.taskFile) ?: error("Can not apply code hint")
        val psiFile = PsiFileFactory.getInstance(project).createFileFromText("file", language, currentUserCode)
        val inspections = InspectionProvider.getInspections(language)
        val issues = psiFile.getInspectionsWithIssues(inspections).map { it.id }

        val taskChecker = KtTaskCheckerProvider().getEduTaskChecker(task, project)
        val indicator = ProgressManager.getInstance().progressIndicator
        taskChecker.check(indicator)
        val testResults = task.status

        records.add(
          MultipleCodeHintDataframeRecord(
            hintIndex = hintIndex,
            taskId = task.id,
            taskName = task.name,
            taskDescription = taskProcessor.getTaskTextRepresentation(),
            taskAnalysisPrompt = assistant.taskAnalysisPrompt,
            steps = task.generatedSolutionSteps,
            codeHintPrompt = response.prompts.getOrDefault("nextStepCodeHintPrompt", ""),
            userCode = userCode,
            generatedCode = response.codeHint,
            numberOfIssues = issues.size,
            issues = issues.joinToString(","),
            testStatus = task.status.name
          )
        )

        if (testResults == CheckStatus.Solved) {
          break
        }
      }
      catch (e: Throwable) {
        records.add(
          MultipleCodeHintDataframeRecord(
            hintIndex = hintIndex,
            taskId = task.id,
            taskName = task.name,
            taskDescription = taskProcessor.getTaskTextRepresentation(),
            userCode = userCode,
            error = e
          )
        )
      }
    }
    return records
  }

  private fun checkTaskFile(taskFile: TaskFile) = (taskFile.isBinary == false) && taskFile.isVisible

  private fun getCodeFromTaskFiles(task: EduTask, lesson: Lesson): List<String> {
    val project = task.project ?: error("Cannot get project")
    if (lesson is FrameworkLesson) {
      val state: Map<String, String> = FrameworkLessonManager.getInstance(project).getTaskState(lesson, task)
      return state.filterKeys { task.taskFiles[it] != null && checkTaskFile(task.taskFiles[it]!!) }.values.toList().take(5)
    }
    return task.taskFiles.values.filter { checkTaskFile(it) }.mapNotNull { it.getText(project) }
  }
}
