package com.jetbrains.edu.cognifire.log

import java.util.logging.Formatter
import java.util.logging.LogRecord

class CognifireLoggerFactory : BaseCognifireLoggerFactory("Cognifire", "cognifire.log") {
  override val formatter: Formatter = object : Formatter() {
    override fun format(record: LogRecord): String =
      String.format("%1\$10tT %2\$-5s - %3\$s%n", record.millis, record.level, record.message)
  }
}