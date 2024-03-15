package com.jetbrains.edu.assistant.validation.actions.solution.steps

import com.google.gson.Gson
import com.jetbrains.edu.assistant.validation.actions.ValidationAction
import com.jetbrains.edu.assistant.validation.messages.EduAndroidAiAssistantValidationBundle
import com.jetbrains.edu.assistant.validation.processor.processValidationSteps
import com.jetbrains.edu.assistant.validation.util.ValidationOfStepsDataframeRecord
import com.jetbrains.edu.assistant.validation.util.getAuthorSolution
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.eduAssistant.core.TaskBasedAssistant
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor
import org.jetbrains.kotlinx.dataframe.api.toDataFrame

/**
 * The `Auto Validate Steps Generation` action runs an automatic validation of educational AI assistant generating solution steps.
 * The output data can be found in `educational-plugin/aiAssistantValidation/generatedValidationOfSteps.csv` by default, validation
 * output directory can be set up in `gradle.properties`.
 *
 * Example of the output data:
 * ```
 * |   taskId   |      taskName     | taskDescription |              steps                |  solution |  amount  |  specifics  |  independence  |  codingSpecific  |  direction  |  misleadingInformation  |  granularity  |  kotlinStyle  |
 * |:----------:|:-----------------:|:---------------:|:---------------------------------:|:---------:|:--------:|:-----------:|:--------------:|:----------------:|:-----------:|:-----------------------:|:-------------:|:-------------:|
 * | 1412191977 | ProgramEntryPoint |       ...       | 1. Start by declaring ... 2. ...  |    ...    |    3     |    Yes      |     Yes        |     Coding       |     Yes     |          No             |     Yes       |     Yes       |
 * | 1762576790 | BuiltinFunctions  |       ...       |              ...                  |    ...    |    5     |    Yes      |     No         |     No coding    |     No      |          Yes            |     No        |     No        |
 * |     ...    |        ...        |       ...       |              ...                  |    ...    |   ...    |    ...      |     ...        |       ...        |     ...     |          ...            |     ...       |     ...       |
 * ```
 */
@Suppress("ComponentNotRegistered")
class AutoStepsValidationAction : ValidationAction<ValidationOfStepsDataframeRecord>() {

  override val outputFilePrefixName: String = "generatedValidationOfSteps"
  override val name: String = EduAndroidAiAssistantValidationBundle.message("action.auto.step.validation.action.name")
  override val isNavigationRequired: Boolean = false

  init {
    setUpSpinnerPanel(name)
  }

  override suspend fun buildRecords(task: EduTask, lesson: Lesson): List<ValidationOfStepsDataframeRecord> {
    val project = task.project ?: error("Cannot get project")
    val authorSolution = getAuthorSolution(task, project)
    val taskProcessor = TaskProcessor(task)
    val assistant = TaskBasedAssistant(taskProcessor)
    var stepsValidation: String? = null

    try {
      val currentSteps = assistant.getTaskAnalysis(task) ?: ""
      val currentTaskDescription = taskProcessor.getTaskTextRepresentation()
      stepsValidation = processValidationSteps(currentTaskDescription, authorSolution, currentSteps)
      val dataframeRecord = Gson().fromJson(stepsValidation, ValidationOfStepsDataframeRecord::class.java)
      return listOf(dataframeRecord.apply {
        taskId = task.id
        taskName = task.name
        taskDescription = currentTaskDescription
        steps = currentSteps
        solution = authorSolution
      })
    } catch (e: Throwable) {
      return listOf(ValidationOfStepsDataframeRecord (
        taskId = task.id,
        taskName = task.name,
        taskDescription = taskProcessor.getTaskTextRepresentation(),
        steps = "Error during validation generation: ${e.message}",
        solution = stepsValidation ?: ""
      ))
    }
  }

  override fun MutableList<ValidationOfStepsDataframeRecord>.convertToDataFrame() = toDataFrame()
}
