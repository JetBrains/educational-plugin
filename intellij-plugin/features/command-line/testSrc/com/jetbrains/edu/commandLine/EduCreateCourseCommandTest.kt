package com.jetbrains.edu.commandLine

import org.junit.runners.Parameterized

class EduCreateCourseCommandTest(
  commandData: EduCommandTestData<EduCreateCourseCommand>,
) : EduCommandTestBase<EduCreateCourseCommand>(commandData) {

  override fun command(): EduCreateCourseCommand = EduCreateCourseCommand()

  companion object {
    @Parameterized.Parameters(name = "{0}")
    @JvmStatic
    fun data(): List<EduCommandTestData<*>> = EduCommandTestData.courseProjectCommandBaseData("createCourse") + listOf(
      EduCommandTestData(
        emptyList(),
        emptyMap(),
        """
          Usage: createCourse [<options>] <path to course project directory>
          
          Options:
            --archive=<value>                                 Path to course archive file
            --marketplace=<value>                             Marketplace course id
            --courseStorage=<value>                           Course id from course storage
            --hyperskill=<value>                              Hyperskill project id
            --log-level=(OFF|SEVERE|WARNING|INFO|FINE|FINER)  Minimal IDE log level printing to stderr
            -h, --help                                        Show this message and exit
        """
      )
    )
  }
}
