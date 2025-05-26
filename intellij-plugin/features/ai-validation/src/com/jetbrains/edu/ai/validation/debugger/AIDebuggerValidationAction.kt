package com.jetbrains.edu.ai.validation.debugger

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.ai.debugger.core.service.DebuggerHintRequest
import com.jetbrains.edu.ai.debugger.core.service.TaskDescription
import com.jetbrains.edu.ai.debugger.core.utils.AIDebugUtils.getLanguage
import com.jetbrains.edu.ai.debugger.core.utils.AIDebugUtils.getTaskDescriptionText
import com.jetbrains.edu.ai.debugger.core.utils.AIDebugUtils.toTaskDescriptionType
import com.jetbrains.edu.ai.validation.core.CourseRunner
import com.jetbrains.edu.ai.validation.core.model.StudentSolutionRecord
import com.jetbrains.edu.ai.validation.core.model.UserResult
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.getSolution
import com.jetbrains.edu.learning.courseFormat.ext.isTestFile
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.io.FileWriter
import java.io.File
import java.time.LocalDateTime

@Suppress("ComponentNotRegistered")
class AIDebuggerValidationAction : AnAction() {

  private val pathToSolution = "/datasets/validation_dataset.csv"
  private val validationOutputPath = "/datasets/output_${LocalDateTime.now()}.csv"

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = project.course ?: return

    val solutions = parseCsvFile { record ->
      StudentSolutionRecord.buildFrom(record)
    } ?: return

    CourseRunner(project, course, solutions).collectUserResultInfo { results ->
      results
        .map { it.toDebuggerHint(project) }
        .writeCSV()
    }
  }

  private fun UserResult.toDebuggerHint(project: Project) = DebuggerHintRequest(
    authorSolution =  task.taskFiles.values.filter { !it.isTestFile }.associate { it.name to it.getSolution() },
    courseId = task.course.id,
    programmingLanguage = project.getLanguage(),
    taskDescription = TaskDescription(
      descriptionFormat = task.descriptionFormat.toTaskDescriptionType(),
      text = task.getTaskDescriptionText(project)
    ),
    taskId = task.id,
    testInfo = testInfo,
    updateVersion = project.course?.marketplaceCourseVersion,
    userSolution = userSolution
  )

  private fun <K> parseCsvFile(recordConverter: (CSVRecord) -> K): List<K>? =
    javaClass.getResourceAsStream(pathToSolution)?.use { inputStream ->
      val format = CSVFormat.Builder.create().setHeader().setSkipHeaderRecord(true).build()
      val csvParser = CSVParser(inputStream.reader(), format)
      return csvParser.records.map(recordConverter)
    }

  private fun List<DebuggerHintRequest>.writeCSV() {
    val validationOutputFile = File("${PathManager.getConfigPath()}/$validationOutputPath")
    validationOutputFile.parentFile?.mkdirs()
    val format = CSVFormat.Builder.create().setHeader().setSkipHeaderRecord(true).build()
    val writer = FileWriter(validationOutputFile, true)
    format.print(writer).use { printer ->
      printer.printRecord(AUTHOR_SOLUTION, COURSE_ID, PROGRAMMING_LANGUAGE, TASK_DESCRIPTION_TEXT, TASK_DESCRIPTION_FORMAT, TASK_ID,
        TEST_TEXT, TEST_NAME, TEST_EXPECTED_OUTPUT, TEST_ERROR_MESSAGE, TEST_FILES, UPDATE_VERSION, USER_SOLUTION)
      forEach {
        printer.printRecord(
          it.authorSolution,
          it.courseId,
          it.programmingLanguage,
          it.taskDescription.text,
          it.taskDescription.descriptionFormat,
          it.taskId,
          it.testInfo.text,
          it.testInfo.name,
          it.testInfo.expectedOutput,
          it.testInfo.errorMessage,
          it.testInfo.testFiles,
          it.updateVersion,
          it.userSolution
        )
      }
    }
  }

  companion object {
    private const val AUTHOR_SOLUTION = "authorSolution"
    private const val COURSE_ID = "courseId"
    private const val PROGRAMMING_LANGUAGE = "programmingLanguage"
    private const val TASK_DESCRIPTION_TEXT = "taskDescriptionText"
    private const val TASK_DESCRIPTION_FORMAT = "taskDescriptionFormat"
    private const val TASK_ID = "taskId"
    private const val TEST_TEXT = "testText"
    private const val TEST_NAME = "testName"
    private const val TEST_EXPECTED_OUTPUT = "testExpectedOutput"
    private const val TEST_ERROR_MESSAGE = "testErrorMessage"
    private const val TEST_FILES = "testFiles"
    private const val UPDATE_VERSION = "updateVersion"
    private const val USER_SOLUTION = "userSolution"
  }
}