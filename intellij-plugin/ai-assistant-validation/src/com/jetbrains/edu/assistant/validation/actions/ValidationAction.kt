package com.jetbrains.edu.assistant.validation.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.jetbrains.edu.assistant.validation.messages.EduAndroidAiAssistantValidationBundle
import com.jetbrains.edu.assistant.validation.util.propagateAuthorSolution
import com.jetbrains.edu.learning.actions.ActionWithProgressIcon
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.intellij.openapi.progress.Task.Backgroundable
import com.jetbrains.edu.assistant.validation.util.StudentSolutionRecord
import com.jetbrains.edu.assistant.validation.util.downloadSolution
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.navigation.NavigationUtils
import java.time.LocalDateTime
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.forEach
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import kotlin.io.path.*

/**
 * Abstract class for performing validation actions in the educational AI assistant.
 * It provides a framework for subclasses to define and implement specific validation tasks, such as automatic validation or
 * preparing datasets for manual validation.
 *
 * @param T the type of records generated during validation
 *
 * @property outputFilePrefixName the prefix name of the file where the validation results will be saved
 * @property name the name of the validation action
 *
 * @see ActionWithProgressIcon
 * @see DumbAware
 */
abstract class ValidationAction<T> : ActionWithProgressIcon(), DumbAware {
  protected abstract val outputFilePrefixName: String
  protected abstract val name: String
  protected abstract val isNavigationRequired: Boolean
  private val validationOutputPath by lazy {
    Path(System.getProperty("validation.output.path", "validationOutput")).also {
      it.createDirectories()
    }
  }
  private val outputFileName: String by lazy { "${outputFilePrefixName}_${LocalDateTime.now()}.csv" }
  private val validationOutputFile: File by lazy { (validationOutputPath / outputFileName).toFile() }
  private val pathToSolutions by lazy { Path(System.getProperty("validation.solution.path")) }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  protected abstract suspend fun buildRecords(task: EduTask, lesson: Lesson): List<T>

  protected abstract fun MutableList<T>.convertToDataFrame(): DataFrame<T>

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = project.course ?: return
    ProgressManager.getInstance().run(ValidationTask(project, course))
  }

  inner class ValidationTask(
    project: Project,
    private val course: Course
  ) : Backgroundable(project, name) {
    override fun run(indicator: ProgressIndicator) {
      processStarted()

      indicator.isIndeterminate = false
      indicator.fraction = 0.0
      val totalTasks = course.allTasks.size
      var doneTasks = 0
      var previousTask: Task? = null

      val studentSolutions = getStudentSolutions()

      for (lesson in course.lessons) {
        val records = mutableListOf<T>()
        for (task in lesson.taskList) {
          if (task is EduTask) {
            if (isNavigationRequired) {
              ApplicationManager.getApplication().invokeAndWait {
                ApplicationManager.getApplication().runWriteAction {
                  NavigationUtils.navigateToTask(project, task)
                }
              }
            }

            indicator.text = "${EduAndroidAiAssistantValidationBundle.message("action.validation.indicator.task")} ${task.name}"
            indicator.fraction = doneTasks.toDouble() / totalTasks

            studentSolutions?.let {
              it.getSolutionListForTask(lesson.name, task.name).forEach { studentCode ->
                downloadSolution(task, project, studentCode)
                runBlockingCancellable {
                  records.addAll(buildRecords(task, lesson))
                }
              }
            } ?: run {
              previousTask?.let {
                propagateAuthorSolution(it, task, project)
              }
              runBlockingCancellable {
                records.addAll(buildRecords(task, lesson))
              }
            }

            doneTasks++
          }
          previousTask = task
        }
        records.convertToDataFrame().writeCSV()
      }

      indicator.fraction = 1.0
      indicator.text = EduAndroidAiAssistantValidationBundle.message("action.validation.indicator.results")

      processFinished()
    }

    private fun getStudentSolutions(): List<StudentSolutionRecord>? {
      if (pathToSolutions.exists()) {
        Files.newBufferedReader(pathToSolutions).use { reader ->
          val csvParser = CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())
          return csvParser.records.map { record ->
            StudentSolutionRecord(record.get(0).toInt(), record.get(1), record.get(2), record.get(3))
          }
        }
      }
      return null
    }

    private fun List<StudentSolutionRecord>.getSolutionListForTask(lessonName: String, taskName: String) =
      filter { it.lessonName == lessonName && it.taskName == taskName }.map { it.code }

    private fun DataFrame<T>.writeCSV() {
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

    override fun onThrowable(error: Throwable) {
      NotificationGroupManager.getInstance().getNotificationGroup("AiEduAssistantValidation")
        .createNotification(error.message ?: "Error during validation", NotificationType.ERROR)
        .notify(project)
      super.onThrowable(error)
    }

    override fun onFinished() {
      NotificationGroupManager.getInstance().getNotificationGroup("AiEduAssistantValidation")
        .createNotification(EduAndroidAiAssistantValidationBundle.message(
          "notification.content.finished.validation.task",
          name,
          validationOutputFile
        ), NotificationType.INFORMATION)
        .notify(project)
      super.onFinished()
    }
  }
}
