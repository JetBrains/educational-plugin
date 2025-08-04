package com.jetbrains.edu.commandLine

import java.nio.file.Paths
import kotlin.reflect.KProperty1

data class EduCommandTestData<T : EduCommand>(
  val args: List<String>,
  val expectedValues: Map<KProperty1<T, Any?>, Any?>,
  val errorMessage: String?
) {
  override fun toString(): String = args.toString()

  companion object {
    fun courseProjectCommandBaseData(commandName: String): List<EduCommandTestData<EduCourseProjectCommand>> {
      return listOf(
        EduCommandTestData(
          listOf("/path/to/project", "--marketplace", "12345"),
          mapOf(
            EduCourseProjectCommand::source to CourseSource.MARKETPLACE,
            EduCourseProjectCommand::courseId to "12345",
            EduCourseProjectCommand::courseDir to Paths.get("/path/to/project")
          ),
          null
        ),
        EduCommandTestData(
          listOf("/path/to/project", "--archive", "/path/to/archive.zip"),
          mapOf(
            EduCourseProjectCommand::source to CourseSource.ARCHIVE,
            EduCourseProjectCommand::courseId to "/path/to/archive.zip",
            EduCourseProjectCommand::courseDir to Paths.get("/path/to/project")
          ),
          null
        ),
        EduCommandTestData(
          listOf("/path/to/project", "--hyperskill", "98765"),
          mapOf(
            EduCourseProjectCommand::source to CourseSource.HYPERSKILL,
            EduCourseProjectCommand::courseId to "98765",
            EduCourseProjectCommand::courseDir to Paths.get("/path/to/project")
          ),
          null
        ),
        EduCommandTestData(
          listOf("/path/to/project"),
          empty(),
          """
            Usage: $commandName [<options>] <path to course project directory>
  
            Error: must provide one of --archive, --marketplace, --courseStorage, --hyperskill
          """
        ),
        EduCommandTestData(
          listOf("--marketplace", "12345"),
          empty(),
          """
            Usage: $commandName [<options>] <path to course project directory>
            
            Error: missing argument <path to course project directory>
          """
        ),
        EduCommandTestData(
          listOf("/path/to/project1", "/path/to/project2"),
          empty(),
          """
            Usage: $commandName [<options>] <path to course project directory>
  
            Error: got unexpected extra argument (/path/to/project2)
            Error: must provide one of --archive, --marketplace, --courseStorage, --hyperskill
          """
        ),
        EduCommandTestData(
          listOf("/path/to/project", "--marketplace"),
          empty(),
          """
            Usage: $commandName [<options>] <path to course project directory>
            
            Error: option --marketplace requires a value
          """
        ),
        EduCommandTestData(
          listOf("/path/to/project", "--marketplace", "12345", "--archive", "/path/to/archive.zip"),
          empty(),
          """
            Usage: $commandName [<options>] <path to course project directory>
  
            Error: option --archive cannot be used with --marketplace or --courseStorage or --hyperskill
          """
        ),
        EduCommandTestData(
          listOf("--foo", "12345"),
          empty(),
          """
            Usage: $commandName [<options>] <path to course project directory>
  
            Error: no such option --foo
            Error: must provide one of --archive, --marketplace, --courseStorage, --hyperskill
          """
        ),
      )
    }
  }
}

fun <T : EduCommand> empty(): Map<KProperty1<T, Any?>, Any?> = emptyMap()

