package com.jetbrains.edu.assistant.validation.test

import com.jetbrains.edu.assistant.validation.util.StudentSolutionRecord
import com.jetbrains.edu.assistant.validation.util.downloadSolution
import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.eduAssistant.core.TaskBasedAssistant
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor
import com.jetbrains.edu.learning.navigation.NavigationUtils
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.exists
import org.hamcrest.CoreMatchers

@RunWith(Parameterized::class)
class CodeHintCompilationTest(private val lessonName: String, private val taskName: String) : JdkCheckerTestBase() {

  companion object {
    @JvmStatic
    @Parameterized.Parameters
    fun data() = listOf(
      arrayOf("TheFirstDateWithProgramming", "Variables")
    )
//    fun data() = kotlinOnboardingMockCourse.lessons.flatMap { lesson ->
//      lesson.taskList.filterIsInstance<EduTask>().map { task ->
//        arrayOf(lesson.name, task.name)
//      }
//    }
  }

  private fun getStudentSolutions(): List<StudentSolutionRecord>? {
    val pathToSolutions = Path("../../solutionsForValidation/tt_data_for_tests_version1.csv")
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

  @Test
  fun testCodeHintCompilation() {
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      assertEquals("${task.name} should be failed", CheckStatus.Failed, checkResult.status)
      TestComparisonData(
        CoreMatchers.equalTo(CheckUtils.COMPILATION_FAILED_MESSAGE),
        nullValue()
      )
    }
    val course = project.course ?: error("Course was not found")
    val task = course.findTask(lessonName = lessonName, taskName = taskName)
    NavigationUtils.navigateToTask(project, task)
    val state = project.eduState ?: error("Edu state is invalid")
    val taskProcessor = TaskProcessor(task)
    val assistant = TaskBasedAssistant(taskProcessor)
    val studentSolutions = getStudentSolutions() ?: error("Student solutions was not found")
    studentSolutions.filter { it.lessonName == lessonName && it.taskName == taskName }.map { it.code }.forEach { studentCode ->
      downloadSolution(task, project, studentCode)

//      var response: AssistantResponse? = null
//      runInBackground(project, "Running Tests", true) {
//        runBlockingCancellable {
//          withContext(Dispatchers.IO) {
//            response = assistant.getHint(task, state)
//          }
//        }
//      }
//      val codeHint = response?.codeHint ?: error("Failed to generate the code hint")
      val codeHint = """
        package jetbrains.kotlin.course.first.date

        fun main() {
            sajaqksndkkiwnskksk
               djjdjkdkd
        }
      """.trimIndent()
      downloadSolution(task, project, codeHint)

      myCourse.configurator!!.courseBuilder.refreshProject(project, RefreshCause.PROJECT_CREATED)
      checkTask(task)
    }
  }

  override fun createCourse(): Course = kotlinOnboardingMockCourse
}