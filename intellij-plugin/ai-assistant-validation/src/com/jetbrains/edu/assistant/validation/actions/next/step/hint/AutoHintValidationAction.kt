package com.jetbrains.edu.assistant.validation.actions.next.step.hint

import com.google.gson.Gson
import com.jetbrains.edu.assistant.validation.actions.ValidationAction
import com.jetbrains.edu.assistant.validation.messages.EduAndroidAiAssistantValidationBundle
import com.jetbrains.edu.assistant.validation.processor.processValidationHintForItsType
import com.jetbrains.edu.assistant.validation.processor.processValidationHints
import com.jetbrains.edu.assistant.validation.util.ValidationOfHintsDataframeRecord
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.eduAssistant.core.TaskBasedAssistant
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor
import com.jetbrains.edu.learning.eduState
import com.jetbrains.edu.learning.getTextFromTaskTextFile
import org.jetbrains.kotlinx.dataframe.api.toDataFrame

/**
 * The `Auto Validate Hints Generation` action runs an automatic validation of educational AI assistant generating text and code hints.
 * The output data can be found in `educational-plugin/aiAssistantValidation/generatedValidationOfHints.csv` by default, validation
 * output directory can be set up in `gradle.properties`.
 *
 * Example of the output data:
 * ```
 * |   taskId   |      taskName     | taskDescription |  solutionSteps  |  userCode  |  nextStepTextHint |  nextStepCodeHint  |  feedbackType  |  information  |  levelOfDetail  |  personalized  |  intersection  |  appropriate  |  specific  |  misleadingInformation  |  codeQuality  | kotlinStyle  |                                       length                                         |
 * |:----------:|:-----------------:|:---------------:|:---------------:|:----------:|:-----------------:|:------------------:|:--------------:|:-------------:|:---------------:|:--------------:|:--------------:|:-------------:|:----------:|:-----------------------:|:-------------:|:------------:|:------------------------------------------------------------------------------------:|
 * | 1412191977 | ProgramEntryPoint |       ...       | 1. Start by ... | package... | Replace the ...   | package ...        | KTC-TR, ...    |      No       |      BOH        |      Yes       |      No        |     Yes       |    Yes     |           No            |      Yes      |     Yes      |  the text hint consists of 1 sentence and 8 words, the code hint consists of 5 lines |
 * |     ...    |        ...        |       ...       |     ...         |     ...    |       ...        |       ...           |      ...       |     ...       |       ...       |       ...      |      ...       |     ...       |    ...     |           ...           |      ...      |     ...      |                                        ...                                           |
 * ```
 */
@Suppress("ComponentNotRegistered")
class AutoHintValidationAction : ValidationAction<ValidationOfHintsDataframeRecord>() {

  override val outputFilePrefixName: String = "generatedValidationOfHints"
  override val name: String = EduAndroidAiAssistantValidationBundle.message("action.auto.hint.validation.action.name")
  override val isNavigationRequired: Boolean = true

  init {
    setUpSpinnerPanel(name)
  }

  override fun MutableList<ValidationOfHintsDataframeRecord>.convertToDataFrame() = toDataFrame()

  override suspend fun buildRecords(task: EduTask, lesson: Lesson): List<ValidationOfHintsDataframeRecord> {
    val taskProcessor = TaskProcessor(task)
    val assistant = TaskBasedAssistant(taskProcessor)
    val project = task.project ?: error("Cannot get project")
    val eduState = project.eduState ?: error("Cannot get eduState for project ${project.name}")

    try {
      val response = assistant.getHint(task, eduState)
      val currentUserCode = eduState.taskFile.getVirtualFile(project)?.getTextFromTaskTextFile() ?: error("Cannot get a user code")
      val generatedSolutionSteps = task.generatedSolutionSteps ?: error("Cannot get the solution steps")
      val currentTaskDescription = taskProcessor.getTaskTextRepresentation()
      val textHint = response.textHint ?: error("Cannot get a text hint")
      val codeHint = response.codeHint ?: error("Cannot get a code hint")
      val hintType = processValidationHintForItsType(textHint, codeHint)
      val hintsValidation = processValidationHints(currentTaskDescription, textHint, codeHint, currentUserCode, generatedSolutionSteps)
      val dataframeRecord = Gson().fromJson(hintsValidation, ValidationOfHintsDataframeRecord::class.java)
      return listOf(dataframeRecord.apply {
        taskId = task.id
        taskName = task.name
        taskDescription = currentTaskDescription
        solutionSteps = generatedSolutionSteps
        userCode = currentUserCode
        nextStepTextHint = textHint
        nextStepCodeHint = codeHint
        feedbackType = hintType
      })
    }
    catch (e: Throwable) {
      return listOf(ValidationOfHintsDataframeRecord (
        taskId = task.id,
        taskName = task.name,
        taskDescription = taskProcessor.getTaskTextRepresentation(),
        solutionSteps = "Error during validation generation: ${e.message}"
      ))
    }
  }
}
