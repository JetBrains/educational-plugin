package com.jetbrains.edu.commandLine

import org.junit.runners.Parameterized

class EduOpenCourseCommandTest(
  commandData: EduCommandTestData<EduOpenCourseCommand>
) : EduCommandTestBase<EduOpenCourseCommand>(commandData) {
  override fun command(): EduOpenCourseCommand = EduOpenCourseCommand()

  companion object {
    @Parameterized.Parameters(name = "args = {0}")
    @JvmStatic
    fun data(): List<EduCommandTestData<EduOpenCourseCommand>> = listOf(
      EduCommandTestData(
        listOf("--marketplace", "12345"),
        mapOf(
          EduOpenCourseCommand::source to CourseSource.MARKETPLACE,
          EduOpenCourseCommand::courseId to "12345",
        ),
        null
      ),
      EduCommandTestData(
        listOf("--archive", "/path/to/archive.zip"),
        mapOf(
          EduOpenCourseCommand::source to CourseSource.ARCHIVE,
          EduOpenCourseCommand::courseId to "/path/to/archive.zip",
        ),
        null
      ),
      EduCommandTestData(
        listOf("--hyperskill", "98765"),
        mapOf(
          EduOpenCourseCommand::source to CourseSource.HYPERSKILL,
          EduOpenCourseCommand::courseId to "98765",
        ),
        null
      ),
      EduCommandTestData(
        emptyList(),
        emptyMap(),
        """
          Usage: openCourse [<options>]
          
          Options:
            --archive=<value>      Path to course archive file
            --marketplace=<value>  Marketplace course link. Supported formats:
              - %course-id%
              - %course-id%-%plugin-name%
              - https://plugins.jetbrains.com/plugin/%course-id%
              - https://plugins.jetbrains.com/plugin/%course-id%-%plugin-name%.
              
              So, for https://plugins.jetbrains.com/plugin/16630-introduction-to-python course, you can pass:
              - 16630
              - 16630-introduction-to-python
              - https://plugins.jetbrains.com/plugin/16630
              - https://plugins.jetbrains.com/plugin/16630-introduction-to-python
            --hyperskill=<value>   Hyperskill project id
            -h, --help             Show this message and exit
        """
      ),
      EduCommandTestData(
        listOf("/path/to/project"),
        emptyMap(),
        """
          Usage: openCourse [<options>]
          
          Error: got unexpected extra argument (/path/to/project)
          Error: must provide one of --archive, --marketplace, --hyperskill
        """
      ),
      EduCommandTestData(
        listOf("/path/to/project", "--marketplace", "12345"),
        emptyMap(),
        """
          Usage: openCourse [<options>]
          
          Error: got unexpected extra argument (/path/to/project)
        """
      ),
      EduCommandTestData(
        listOf("--marketplace"),
        emptyMap(),
        """
          Usage: openCourse [<options>]

          Error: option --marketplace requires a value
        """
      ),
      EduCommandTestData(
        listOf("--marketplace", "12345", "--archive", "/path/to/archive.zip"),
        emptyMap(),
        """
          Usage: openCourse [<options>]

          Error: option --archive cannot be used with --marketplace or --hyperskill
        """
      ),
      EduCommandTestData(
        listOf("--foo", "12345"),
        emptyMap(),
        """
          Usage: openCourse [<options>]

          Error: no such option --foo
          Error: must provide one of --archive, --marketplace, --hyperskill
        """
      ),
    )
  }
}
