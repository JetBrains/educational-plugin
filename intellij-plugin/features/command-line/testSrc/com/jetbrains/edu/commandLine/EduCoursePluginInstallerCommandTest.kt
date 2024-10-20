package com.jetbrains.edu.commandLine

import org.junit.runners.Parameterized
import java.nio.file.Paths

class EduCoursePluginInstallerCommandTest(
  data: EduCommandTestData<EduCoursePluginInstallerCommand>
) : EduCommandTestBase<EduCoursePluginInstallerCommand>(data) {

  override fun command(): EduCoursePluginInstallerCommand = EduCoursePluginInstallerCommand()

  companion object {
    @Parameterized.Parameters(name = "args = {0}")
    @JvmStatic
    fun data(): List<EduCommandTestData<EduCoursePluginInstallerCommand>> = listOf(
      EduCommandTestData(
        listOf("/path/to/project", "--marketplace", "12345"),
        mapOf(
          EduCoursePluginInstallerCommand::source to CourseSource.MARKETPLACE,
          EduCoursePluginInstallerCommand::courseId to "12345",
          EduCoursePluginInstallerCommand::projectPath to Paths.get("/path/to/project")
        ),
        null
      ),
      EduCommandTestData(
        listOf("--marketplace", "12345"),
        mapOf(
          EduCoursePluginInstallerCommand::source to CourseSource.MARKETPLACE,
          EduCoursePluginInstallerCommand::courseId to "12345",
          EduCoursePluginInstallerCommand::projectPath to null
        ),
        null
      ),
      EduCommandTestData(
        listOf("--archive", "/path/to/archive.zip"),
        mapOf(
          EduCoursePluginInstallerCommand::source to CourseSource.ARCHIVE,
          EduCoursePluginInstallerCommand::courseId to "/path/to/archive.zip",
          EduCoursePluginInstallerCommand::projectPath to null
        ),
        null
      ),
      EduCommandTestData(
        listOf("--hyperskill", "98765"),
        mapOf(
          EduCoursePluginInstallerCommand::source to CourseSource.HYPERSKILL,
          EduCoursePluginInstallerCommand::courseId to "98765",
          EduCoursePluginInstallerCommand::projectPath to null
        ),
        null
      ),
      EduCommandTestData(
        emptyList(),
        emptyMap(),
        """
          Usage: installCoursePlugins [<options>] [<path to course project directory>]

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
            -h, --help                                        Show this message and exit
        """
      ),
      EduCommandTestData(
        listOf("/path/to/project"),
        emptyMap(),
        """
          Usage: installCoursePlugins [<options>] [<path to course project directory>]

          Error: must provide one of --archive, --marketplace, --hyperskill
        """
      ),
      EduCommandTestData(
        listOf("/path/to/project1", "/path/to/project2"),
        emptyMap(),
        """
          Usage: installCoursePlugins [<options>] [<path to course project directory>]

          Error: got unexpected extra argument (/path/to/project2)
          Error: must provide one of --archive, --marketplace, --hyperskill
        """
      ),
      EduCommandTestData(
        listOf("--marketplace"),
        emptyMap(),
        """
          Usage: installCoursePlugins [<options>] [<path to course project directory>]

          Error: option --marketplace requires a value
        """
      ),
      EduCommandTestData(
        listOf("--marketplace", "12345", "--archive", "/path/to/archive.zip"),
        emptyMap(),
        """
          Usage: installCoursePlugins [<options>] [<path to course project directory>]

          Error: option --archive cannot be used with --marketplace or --hyperskill
        """
      ),
      EduCommandTestData(
        listOf("--foo", "12345"),
        emptyMap(),
        """
          Usage: installCoursePlugins [<options>] [<path to course project directory>]

          Error: no such option --foo
          Error: must provide one of --archive, --marketplace, --hyperskill
        """
      ),
    )
  }
}
