package com.jetbrains.edu.cognifire.validation

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.jetbrains.edu.cognifire.GeneratedCodeParser
import com.jetbrains.edu.cognifire.codegeneration.CodeGenerator
import com.jetbrains.edu.cognifire.grammar.GrammarParser
import com.jetbrains.edu.cognifire.messages.EduCognifireBundle
import com.jetbrains.edu.cognifire.models.PromptExpression
import com.jetbrains.edu.learning.actions.ActionWithProgressIcon
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.educational.ml.cognifire.core.PromptGenerationAssistant
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
 * @see DumbAware
 */
class GeneratedCodeValidationAction : ActionWithProgressIcon(), DumbAware {
  private val name = EduCognifireBundle.message("action.Validation.GeneratedCodeValidation.text")
  private val validationOutputPath by lazy {
    Path(System.getProperty("validation.output.path", "validationOutput")).also {
      it.createDirectories()
    }
  }
  private val outputFileName: String by lazy { "ValidationOfGeneratedCode_${LocalDateTime.now()}.csv" }
  private val validationOutputFile: File by lazy { (validationOutputPath / outputFileName).toFile() }
  private val shouldGenerateBadPrompts: Boolean = true

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
    val totalTasks = course.allTasks.filter { it is EduTask }.size
    var doneTasks = 0
    for (lesson in course.lessons) {
      val tasks = lesson.taskList.filter { it is EduTask }
      if (tasks.isEmpty()) continue
      val records = mutableListOf<GeneratedCodeDataframeRecord>()
      val task = tasks.lastOrNull() ?: continue
      val functionMap = runReadAction { FileIntoFunctionsParser.parseFunctionSignaturesAndBodies (task, language) }
      for ((signature, solution) in functionMap) {
        indicator.text = "${EduCognifireBundle.message("action.validation.indicator.task")} $doneTasks"
        indicator.fraction = doneTasks.toDouble() / totalTasks
        var prompt = getGeneratedPrompt(solution) ?: continue.also {
          records.add(
            GeneratedCodeDataframeRecord(
              taskId = task.id,
              function = signature.toString(),
              modelSolution = solution
            )
          )
          doneTasks++
        }
        if (shouldGenerateBadPrompts) {
          prompt = prompt.removeRandomSentence()
        }
        val promptExpression = PromptExpression(
          functionSignature = signature,
          baseContentOffset = 0,
          baseStartOffset = 0,
          baseEndOffset = 0,
          prompt = prompt,
          code = ""
        )
        try {
          val unparsableSentences = GrammarParser.getUnparsableSentences(promptExpression)
          val codeGenerator = CodeGenerator(promptExpression)
          val generatedCode = codeGenerator.generatedCode
          val hasTODO = runReadAction { GeneratedCodeParser.hasErrors(project, generatedCode, language) }
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
        } catch (e: Throwable) {
          records.add(
            GeneratedCodeDataframeRecord(
              taskId = task.id,
              function = signature.toString(),
              modelSolution = solution,
              prompt = promptExpression.prompt,
              code = promptExpression.code,
              error = e
            )
          )
        } finally {
          doneTasks++
        }
      }
      records.toDataFrame().writeCSV()
    }
    indicator.text = EduCognifireBundle.message("action.validation.indicator.results")
    indicator.fraction = 1.0
    processFinished()
  }

  private fun getGeneratedPrompt(solution: String) = runBlockingCancellable {
    PromptGenerationAssistant.generatePrompt(solution).getOrNull()
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
