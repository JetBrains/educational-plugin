package com.jetbrains.edu.cognifire.validation

import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.jetbrains.edu.cognifire.GeneratedCodeParser
import com.jetbrains.edu.cognifire.codegeneration.CodeGenerator
import com.jetbrains.edu.cognifire.grammar.GrammarParser
import com.jetbrains.edu.cognifire.messages.EduCognifireBundle
import com.jetbrains.edu.cognifire.models.FunctionSignature
import com.jetbrains.edu.cognifire.models.PromptExpression
import com.jetbrains.edu.learning.actions.ActionWithProgressIcon
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.educational.ml.cognifire.core.CodeToPromptAssistant
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import org.apache.commons.csv.CSVFormat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.forEach
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import kotlin.random.Random

/**
 * A class that represents an action to validate generated code.
 * The validation results are saved to a CSV file in the [validationOutputPath].
 * The validation can check how the code is generated from both good and bad prompts,
 * this behavior can be configured using the [shouldGenerateBadPrompts] property.
 *
 * @see ActionWithProgressIcon
 */
open class GeneratedCodeValidationAction(private val shouldGenerateBadPrompts: Boolean = false) : ActionWithProgressIcon() {
  private val name = EduCognifireBundle.message("action.Validation.GeneratedCodeValidation.text")
  private val validationOutputPath by lazy {
    Path(System.getProperty("validation.output.path", "validationOutput")).also {
      it.createDirectories()
    }
  }
  private val outputFileName: String by lazy { "ValidationOfGeneratedCode_${LocalDateTime.now()}.csv" }
  private val validationOutputFile: File by lazy { (validationOutputPath / outputFileName).toFile() }

  init {
    setUpSpinnerPanel(name)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = project.course ?: return
    executeAction(project, course)
  }

  private fun executeAction(project: Project, course: Course) = runBackgroundableTask(
    EduCognifireBundle.message("action.progress.bar.message"),
    project
  ) { indicator ->
    processStarted()
    indicator.isIndeterminate = false
    indicator.fraction = 0.0
    val language = course.languageById ?: return@runBackgroundableTask
    val totalTasks = course.allTasks.filterIsInstance<EduTask>().size
    var doneTasks = 0
    course.lessons.forEach { lesson ->
      doneTasks += processLesson(lesson, project, language, indicator, totalTasks, doneTasks)
    }
    indicator.text = EduCognifireBundle.message("action.validation.indicator.results")
    indicator.fraction = 1.0
    processFinished()
  }

  private fun processLesson(
    lesson: Lesson,
    project: Project,
    language: Language,
    indicator: ProgressIndicator,
    totalTasks: Int,
    doneTasks: Int
  ): Int {
    val tasks = lesson.taskList.filterIsInstance<EduTask>()
    if (tasks.isEmpty()) return 0
    val records = mutableListOf<GeneratedCodeDataframeRecord>()
    val task = tasks.lastOrNull() ?: return 0
    return processTask(task, language, project, records, indicator, totalTasks, doneTasks).also {
      records.toDataFrame().writeCSV()
    }
  }

  private fun processTask(
    task: Task,
    language: Language,
    project: Project,
    records: MutableList<GeneratedCodeDataframeRecord>,
    indicator: ProgressIndicator,
    totalTasks: Int,
    doneTasks: Int
  ): Int {
    var completedTasksNumber = doneTasks
    val functionMap = runReadAction { FileIntoFunctionsParser.parseFunctionSignaturesAndBodies (task, language) }
    for ((signature, solution) in functionMap) {
      indicator.text = "${EduCognifireBundle.message("action.validation.indicator.task")} $completedTasksNumber"
      indicator.fraction = completedTasksNumber.toDouble() / totalTasks
      var prompt = getGeneratedPrompt(solution)
      if (prompt == null) {
        records.addRecordForMissingPrompt(task, signature, solution)
        completedTasksNumber++
        continue
      }
      if (shouldGenerateBadPrompts) {
        prompt = prompt.removeRandomSentence()
      }
      val promptExpression = createPromptExpression(signature, prompt)
      try {
        processPromptExpression(project, promptExpression, language, task, signature, solution, records)
      } catch (e: Throwable) {
        records.addExceptionRecord(task, signature, solution, promptExpression, e)
      } finally {
        completedTasksNumber++
      }
    }
    return completedTasksNumber
  }

  private fun createPromptExpression(signature: FunctionSignature, prompt: String) =
    PromptExpression(
      functionSignature = signature,
      baseContentOffset = 0,
      baseStartOffset = 0,
      baseEndOffset = 0,
      prompt = prompt,
      code = ""
    )

  private fun processPromptExpression(
    project: Project,
    promptExpression: PromptExpression,
    language: Language,
    task: Task,
    signature: FunctionSignature,
    solution: String,
    records: MutableList<GeneratedCodeDataframeRecord>
  ) {
    val unparsableSentences = GrammarParser.getUnparsableSentences(promptExpression)
    val codeGenerator = CodeGenerator(promptExpression)
    val generatedCode = codeGenerator.generatedCode
    val hasTODO = runReadAction { GeneratedCodeParser.hasErrors(project, generatedCode, signature, language) }
    records.add(
      GeneratedCodeDataframeRecord(
        taskId = task.id,
        function = signature.toString(),
        modelSolution = solution,
        prompt = promptExpression.prompt,
        code = promptExpression.code,
        unparsableSentences = unparsableSentences.joinToString(System.lineSeparator()) { it.sentence },
        generatedCode = generatedCode,
        hasErrors = hasTODO
      )
    )
  }

  private fun MutableList<GeneratedCodeDataframeRecord>.addRecordForMissingPrompt(
    task: Task,
    signature: FunctionSignature,
    solution: String
  ) {
    add(GeneratedCodeDataframeRecord(
      taskId = task.id,
      function = signature.toString(),
      modelSolution = solution
    ))
  }

  private fun MutableList<GeneratedCodeDataframeRecord>.addExceptionRecord(
    task: Task,
    signature: FunctionSignature,
    solution: String,
    promptExpression: PromptExpression,
    e: Throwable
  ) {
    add(GeneratedCodeDataframeRecord(
      taskId = task.id,
      function = signature.toString(),
      modelSolution = solution,
      prompt = promptExpression.prompt,
      code = promptExpression.code,
      error = e
    ))
  }

  private fun getGeneratedPrompt(solution: String) = runBlockingCancellable {
    CodeToPromptAssistant.generatePrompt(solution).getOrNull()
  }

  private fun DataFrame<GeneratedCodeDataframeRecord>.writeCSV() {
    val format = CSVFormat.DEFAULT
    val shouldWriteHeaders = !validationOutputFile.exists()
    val writer = FileWriter(validationOutputFile, true)
    format.print(writer).use { printer ->
      if (shouldWriteHeaders) {
        printer.printRecord(columnNames())
      }
      forEach {
        printer.printRecord(it.values())
      }
    }
  }

  private fun String.removeRandomSentence(): String {
    val sentences = split(DOT).map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
    val index = Random.nextInt(sentences.size)
    sentences.removeAt(index)
    return sentences.joinToString(DOT) + if (sentences.isNotEmpty()) DOT else ""
  }

  companion object {
    private const val DOT = "."
  }
}
