package com.jetbrains.edu.ai.debugger.core.log

import com.intellij.idea.LoggerFactory
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.JulLogger
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.RollingFileHandler
import com.intellij.openapi.util.io.findOrCreateFile
import java.io.IOException
import java.nio.file.Path
import java.util.logging.Formatter
import java.util.logging.Level
import java.util.logging.LogRecord
import kotlin.io.path.div

object AIDebuggerLoggerFactory : Logger.Factory {
  private const val CATEGORY: String = "AI-Debugging"
  private const val MAX_FILE_SIZE: Long = 20_000_000 // 20MB
  private const val MAX_ROTATION_INDEX: Int = 50

  private val basePath: Path = Path.of(PathManager.getPluginsPath()) / "ai-debugging" / "aiDebugging.log"

  private val LOG = Logger.getInstance(AIDebuggerLoggerFactory::class.java)

  private val fileLogger: Logger? = createFileLogger()

  override fun getLoggerInstance(category: String): Logger = fileLogger ?: LoggerFactory().getLoggerInstance(category)

  fun getInstance(): Logger = getLoggerInstance(CATEGORY)

  private fun createFileLogger(): Logger? = try {
    runWriteAction {
      basePath.findOrCreateFile()
    }
    LOG.info("$CATEGORY logs are written to $basePath")

    val formatter = object : Formatter() {
      override fun format(record: LogRecord) = String.format(
        "%1\$tF %1\$tT.%1\$tL %1\$tz | %2\$-5s %3\$s%n",
        record.millis,
        record.level,
        record.message.replace("\n", "\\n")
      )
    }

    val appender = RollingFileHandler(basePath, MAX_FILE_SIZE, MAX_ROTATION_INDEX, false).apply {
      setFormatter(formatter)
    }
    java.util.logging.Logger.getLogger(CATEGORY).apply {
      addHandler(appender)
      useParentHandlers = false
      level = Level.INFO
    }.let(::JulLogger)
  } catch (e: IOException) {
    LOG.error("Could not create a log file for $CATEGORY logs; path: $basePath", e)
    null
  }
}
