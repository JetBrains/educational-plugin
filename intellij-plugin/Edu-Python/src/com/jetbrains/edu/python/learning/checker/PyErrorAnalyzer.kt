package com.jetbrains.edu.python.learning.checker

import com.jetbrains.edu.ai.error.explanation.ErrorAnalyzer

class PyErrorAnalyzer : ErrorAnalyzer {
  override fun getStackTrace(stderr: String): List<Pair<String, Int>> {
    val regex = Regex("""File "(.+)", line (\d+)""")
    val matches = regex.findAll(stderr).map { it.destructured }.toList()
    return matches.map { (fileName, lineNumber) -> fileName to lineNumber.toInt() }
  }
}