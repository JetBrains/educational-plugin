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
          EduOpenCourseCommand::courseParams to emptyMap<String, Any>(),
        ),
        null
      ),
      EduCommandTestData(
        listOf("--archive", "/path/to/archive.zip"),
        mapOf(
          EduOpenCourseCommand::source to CourseSource.ARCHIVE,
          EduOpenCourseCommand::courseId to "/path/to/archive.zip",
          EduOpenCourseCommand::courseParams to emptyMap<String, Any>(),
        ),
        null
      ),
      EduCommandTestData(
        listOf("--hyperskill", "98765"),
        mapOf(
          EduOpenCourseCommand::source to CourseSource.HYPERSKILL,
          EduOpenCourseCommand::courseId to "98765",
          EduOpenCourseCommand::courseParams to emptyMap<String, Any>(),
        ),
        null
      ),
      EduCommandTestData(
        emptyList(),
        emptyMap(),
        """
          Usage: openCourse [<options>] [<course params>]...
          
          Options:
            --archive=<value>                                 Path to course archive file
            --marketplace=<value>                             Marketplace course id
            --hyperskill=<value>                              Hyperskill project id
            --log-level=(OFF|SEVERE|WARNING|INFO|FINE|FINER)  Minimal IDE log level printing to stderr
            --course-params=<value>                           Additional parameters for a course project in JSON object format. Deprecated, pass parameters in the form --param1 value1 --param2 value2 ... (default: {})
            -h, --help                                        Show this message and exit

          Arguments:
            <course params>  Additional parameters for a course project in the form --param1 value1 --param2 value2 ...
        """
      ),
      EduCommandTestData(
        listOf("/path/to/project"),
        emptyMap(),
        """
          Usage: openCourse [<options>] [<course params>]...
          
          Error: expected even number of arguments in the form `--param1 value1 --param2 value2 ...` but got `1` instead
          Error: must provide one of --archive, --marketplace, --hyperskill
        """
      ),
      EduCommandTestData(
        listOf("/path/to/project", "--marketplace", "12345"),
        emptyMap(),
        """
          Usage: openCourse [<options>] [<course params>]...

          Error: expected even number of arguments in the form `--param1 value1 --param2 value2 ...` but got `3` instead
          Error: must provide one of --archive, --marketplace, --hyperskill
        """
      ),
      EduCommandTestData(
        listOf("--marketplace"),
        emptyMap(),
        """
          Usage: openCourse [<options>] [<course params>]...

          Error: option --marketplace requires a value
        """
      ),
      EduCommandTestData(
        listOf("--marketplace", "12345", "--archive", "/path/to/archive.zip"),
        emptyMap(),
        """
          Usage: openCourse [<options>] [<course params>]...

          Error: option --archive cannot be used with --marketplace or --hyperskill
        """
      ),
      EduCommandTestData(
        listOf("--foo", "12345"),
        emptyMap(),
        """
          Usage: openCourse [<options>] [<course params>]...

          Error: must provide one of --archive, --marketplace, --hyperskill
        """
      ),
      EduCommandTestData(
        listOf("--marketplace", "12345", "--course-params", """{"qwe":"rty"}"""),
        mapOf(
          EduOpenCourseCommand::source to CourseSource.MARKETPLACE,
          EduOpenCourseCommand::courseId to "12345",
          EduOpenCourseCommand::courseParams to mapOf("qwe" to "rty"),
        ),
        null
      ),
      EduCommandTestData(
        listOf("--marketplace", "12345", "--course-params", "qwerty"),
        emptyMap(),
        """
          Usage: openCourse [<options>] [<course params>]...
          
          Error: invalid value for --course-params: JSON object expected, got `qwerty` instead
        """
      ),
      EduCommandTestData(
        listOf("--marketplace", "12345", "--course-params", """{"a":"b", "c":"d"}""", "--x", "y", "--z", "t"),
        emptyMap(),
        errorMessage =  """
          Usage: openCourse [<options>] [<course params>]...
          
          Error: invalid value for --course-params: path additional parameters for course project either in the form --param value, or with a --course-params option (deprecated), but not both
        """
      ),
      EduCommandTestData(
        // wrong order of parameters
        listOf("--marketplace", "12345", "--x", "y", "--z", "t", "--course-params", """{"a":"b", "c":"d"}"""),
        emptyMap(),
        errorMessage = """
          Usage: openCourse [<options>] [<course params>]...

          Error: path additional parameters for course project either in the form --param value, or with a --course-params option (deprecated), but not both
        """
      ),
      EduCommandTestData(
        listOf("--marketplace", "12345", "--param1", "value1", "--param2", "value2"),
        mapOf(
          EduOpenCourseCommand::courseParams to mapOf(
            "param1" to "value1",
            "param2" to "value2"
          ),
        ),
        errorMessage = null
      ),
      EduCommandTestData(
        listOf("--marketplace", "12345", "--param1", "value1", "--param2"),
        mapOf(
          EduOpenCourseCommand::courseParams to mapOf(
            "param1" to "value1",
            "param2" to "value2"
          ),
        ),
        errorMessage = """
          Usage: openCourse [<options>] [<course params>]...

          Error: expected even number of arguments in the form `--param1 value1 --param2 value2 ...` but got `3` instead
        """
      ),
      EduCommandTestData(
        listOf("--marketplace", "12345", "--param1", "value1", "param2", "value2"),
        mapOf(
          EduOpenCourseCommand::courseParams to mapOf(
            "param1" to "value1",
            "param2" to "value2"
          ),
        ),
        errorMessage = """
          Usage: openCourse [<options>] [<course params>]...

          Error: expected key in the form `--param` but got `param2` instead
        """
      ),
    )
  }
}
