package com.jetbrains.edu.commandLine.validation

import java.io.PrintWriter
import java.nio.file.Path
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createParentDirectories

class FileValidationOutputConsumer(path: Path) : ValidationOutputConsumer {

  private val writer: PrintWriter = PrintWriter(path.createParentDirectories().bufferedWriter())

  override fun consume(output: String) {
    writer.println(output)
  }

  override fun close() {
    writer.close()
  }
}
