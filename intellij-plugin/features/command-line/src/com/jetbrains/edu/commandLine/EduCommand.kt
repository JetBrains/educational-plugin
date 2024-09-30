package com.jetbrains.edu.commandLine

import com.github.ajalt.clikt.command.CoreSuspendingCliktCommand
import com.github.ajalt.clikt.core.MessageEchoer
import com.github.ajalt.clikt.output.PlaintextHelpFormatter
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.OptionDelegate
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.onError
import kotlin.system.exitProcess

abstract class EduCommand(name: String) : CoreSuspendingCliktCommand(name) {

  private val sourceWithCourseId by mutuallyExclusiveOptions(
    // Intentionally doesn't use concrete variants here to properly handle any change in `CourseSource` enum
    CourseSource.values()[0].toOption(),
    CourseSource.values()[1].toOption(),
    *CourseSource.values().drop(2).map { it.toOption() }.toTypedArray()
  ).single().required()

  val source: CourseSource get() = sourceWithCourseId.source
  val courseId: String get() = sourceWithCourseId.courseId

  override val printHelpOnEmptyArgs: Boolean get() = true

  init {
    configureContext {
      helpFormatter = { ctx -> PlaintextHelpFormatter(ctx, showDefaultValues = true) }
    }
  }

  protected open suspend fun doRun(course: Course): CommandResult = CommandResult.Ok

  override suspend fun run() {
    val course = source.loadCourse(courseId).onError { logErrorAndExit(it) }

    val result = doRun(course)
    if (result is CommandResult.Error) {
      LOG.error(result.message, result.throwable)
    }
    ApplicationManagerEx.getApplicationEx().exit(true, true, result.exitCode)
  }

  fun logErrorAndExit(message: String): Nothing {
    echo(message, err = true)
    exitProcess(1)
  }

  private fun CourseSource.toOption(): OptionDelegate<CourseSourceWithId?> = option("--${option}", help = description)
    .convert { CourseSourceWithId(this@toOption, it) }

  companion object {
    @JvmStatic
    protected val LOG = logger<EduCommand>()

    @JvmStatic
    protected fun Course.incompatibleCourseMessage(): String {
      return buildString {
        append("""Can't open `${course.name}` course (type="${course.itemType}", language="${course.languageId}", """)
        if (!course.languageVersion.isNullOrEmpty()) {
          append("""language version="${course.languageVersion}", """)
        }
        append("""environment="${course.environment}") with current IDE setup""")
      }
    }
  }

  private data class CourseSourceWithId(val source: CourseSource, val courseId: String)
}

internal fun Logger.toMessageEchoer(): MessageEchoer = { _, message, _, err ->
  val log: (String) -> Unit = if (err) ::error else ::warn
  log(message.toString())
}
