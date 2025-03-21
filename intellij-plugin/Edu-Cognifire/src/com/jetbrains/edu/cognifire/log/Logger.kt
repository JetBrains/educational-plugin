package com.jetbrains.edu.cognifire.log

import com.intellij.idea.LoggerFactory
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.lazyPub
import java.nio.file.Path
import java.util.logging.Formatter
import java.util.logging.LogRecord
import kotlin.io.path.div

object Logger {
  private val default: Logger = LoggerFactory().getLoggerInstance("Cognifire")

  val cognifireLogger: Logger by lazyPub {
    val basePath: Path = Path.of(PathManager.getPluginsPath()) / "cognifire.log"
    val formatter = object : Formatter() {
      override fun format(record: LogRecord) = String.format("%1\$10tT %2\$-5s - %3\$s%n", record.millis, record.level, record.message)
    }
    BaseCognifireLoggerFactory("Cognifire", basePath, formatter).getLoggerInstanceOrNull() ?: default
  }

  val cognifireStudyLogger: Logger by lazyPub {
    val basePath: Path = Path.of(PathManager.getPluginsPath()) / "cognifire-study" / "cognifire-study.log"
    val formatter: Formatter = object : Formatter() {
      override fun format(record: LogRecord) = String.format(
        "%1\$tF %1\$tT.%1\$tL %1\$tz | %2\$-5s %3\$s%n",
        record.millis,
        record.level,
        record.message.replace("\n", "\\n")
      )
    }
    BaseCognifireLoggerFactory("CognifireStudyLogger", basePath, formatter).getLoggerInstanceOrNull() ?: default
  }
}
