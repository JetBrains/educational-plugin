package com.jetbrains.edu.commandLine.validation

import com.jetbrains.edu.commandLine.CourseSource
import com.jetbrains.edu.commandLine.EduCommandTestBase
import com.jetbrains.edu.commandLine.EduCommandTestData
import com.jetbrains.edu.commandLine.EduCourseProjectCommand
import com.jetbrains.edu.commandLine.validation.EduValidateCourseCommand.Companion.VALIDATE_LINK_BY_DEFAULT
import com.jetbrains.edu.commandLine.validation.EduValidateCourseCommand.Companion.VALIDATE_TEST_BY_DEFAULT
import org.junit.runners.Parameterized
import java.nio.file.Paths
import kotlin.reflect.KProperty1

class EduValidateCourseCommandTest(
  commandData: EduCommandTestData<EduValidateCourseCommand>
) : EduCommandTestBase<EduValidateCourseCommand>(commandData) {

  override fun command(): EduValidateCourseCommand = EduValidateCourseCommand()

  companion object {
    @Parameterized.Parameters(name = "{0}")
    @JvmStatic
    fun data(): List<EduCommandTestData<*>> =
      EduCommandTestData.courseProjectCommandBaseData("validateCourse").map { it.withDefaultValues() } + listOf(
        EduCommandTestData(
          emptyList(),
          emptyMap(),
          """
            Usage: validateCourse [<options>] <path to course project directory>
            
            Options:
              --archive=<value>                                 Path to course archive file
              --marketplace=<value>                             Marketplace course link. Supported formats:
                - %course-id%
                - %course-id%-%plugin-name%
                - https://plugins.jetbrains.com/plugin/%course-id%
                - https://plugins.jetbrains.com/plugin/%course-id%-%plugin-name%.
                
                So, for https://plugins.jetbrains.com/plugin/16630-introduction-to-python course, you can pass:
                - 16630
                - 16630-introduction-to-python
                - https://plugins.jetbrains.com/plugin/16630
                - https://plugins.jetbrains.com/plugin/16630-introduction-to-python
              --hyperskill=<value>                              Hyperskill project id
              --log-level=(OFF|SEVERE|WARNING|INFO|FINE|FINER)  Minimal IDE log level printing to stderr
              --tests=true|false                                Enables/disables test validation (default: false)
              --links=true|false                                Enables/disables task description link validation (default: true)
              -h, --help                                        Show this message and exit
          """
        ),
        EduCommandTestData(
          listOf("/path/to/project", "--marketplace", "12345", "--tests", "true", "--links", "false"),
          mapOf(
            EduValidateCourseCommand::source to CourseSource.MARKETPLACE,
            EduValidateCourseCommand::courseId to "12345",
            EduValidateCourseCommand::courseDir to Paths.get("/path/to/project"),
            EduValidateCourseCommand::validateTests to true,
            EduValidateCourseCommand::validateLinks to false
          ),
          null
        ),
        EduCommandTestData(
          listOf("/path/to/project", "--marketplace", "12345", "--tests", "1234", "--links", "false"),
          emptyMap(),
          """
            Usage: validateCourse [<options>] <path to course project directory>
            
            Error: invalid value for --tests: 1234 is not a valid boolean
          """
        ),
        EduCommandTestData(
          listOf("/path/to/project", "--marketplace", "12345", "--tests", "true", "--links", "123"),
          emptyMap(),
          """
            Usage: validateCourse [<options>] <path to course project directory>
            
            Error: invalid value for --links: 123 is not a valid boolean
          """
        )
      )

    private fun EduCommandTestData<EduCourseProjectCommand>.withDefaultValues(): EduCommandTestData<EduValidateCourseCommand> {
      @Suppress("UNCHECKED_CAST")
      val newExpectedValue = expectedValues as Map<KProperty1<EduValidateCourseCommand, Any?>, Any?> + mapOf(
        EduValidateCourseCommand::validateTests to VALIDATE_TEST_BY_DEFAULT,
        EduValidateCourseCommand::validateLinks to VALIDATE_LINK_BY_DEFAULT
      )
      return EduCommandTestData(args, newExpectedValue, errorMessage)
    }
  }
}
