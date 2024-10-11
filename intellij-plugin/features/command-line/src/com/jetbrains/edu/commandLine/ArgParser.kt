package com.jetbrains.edu.commandLine

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import org.apache.commons.cli.*
import java.io.PrintWriter
import java.io.StringWriter

class ArgParser<T : Args> private constructor(
  private val commandName: String,
  private val requiresProjectPath: Boolean,
  private val options: Options,
  private val createArgs: (CommandLine) -> Result<T, String>
) {

  fun withCustomArgs(addCustomOptions: (Options) -> Unit): ArgParser<T> {
    addCustomOptions(options)
    return this
  }

  fun parseArgs(args: List<String>): Result<T, String> {
    val parser = DefaultParser()

    val cmd = try {
      parser.parse(options, args.drop(1).toTypedArray())
    }
    catch (e: ParseException) {
      LOG.error(e)
      return Err(createHelpMessage())
    }

    return createArgs(cmd)
  }

  private fun createHelpMessage(): String {
    val formatter = HelpFormatter()
    val cmdLineSyntax = if (requiresProjectPath) "$commandName /path/to/project" else commandName
    val stringWriter = StringWriter()
    formatter.printHelp(PrintWriter(stringWriter), 140, cmdLineSyntax, null, options, formatter.leftPadding, formatter.descPadding, null)
    return stringWriter.toString()
  }

  companion object {
    private val LOG: Logger = logger<ArgParser<*>>()

    private fun <T : Args> create(
      commandName: String,
      requiresProjectPath: Boolean,
      createArgs: (CommandLine) -> Result<T, String>
    ): ArgParser<T> {
      val options = Options()

      val courseSourceGroup = OptionGroup()
      courseSourceGroup.isRequired = true
      for (courseSource in CourseSource.values()) {
        courseSourceGroup.addOption(Option(null, courseSource.option, true, courseSource.description))
      }
      options.addOptionGroup(courseSourceGroup)

      return ArgParser(commandName, requiresProjectPath, options, createArgs)
    }

    fun createDefault(commandName: String): ArgParser<Args> = create(commandName, false) { Ok(Args(it)) }

    fun createWithProjectPath(commandName: String): ArgParser<ArgsWithProjectPath> = create(commandName, true) { cmd ->
      val positionalArgs = cmd.argList
      if (positionalArgs.isEmpty()) {
        Err("Path to project is missing")
      }
      else {
        Ok(ArgsWithProjectPath(positionalArgs[0], cmd))
      }
    }
  }
}
