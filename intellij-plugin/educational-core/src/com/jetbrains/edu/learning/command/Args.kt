package com.jetbrains.edu.learning.command

import org.apache.commons.cli.CommandLine

open class Args(private val cmd: CommandLine) {
  fun getOptionValue(option: String): String? = cmd.getOptionValue(option)
}

class ArgsWithProjectPath(val projectPath: String, cmd: CommandLine) : Args(cmd)
