package com.jetbrains.edu.cognifire.log

import java.util.logging.Formatter
import java.util.logging.LogRecord

class CognifireStudyLoggerFactory : BaseCognifireLoggerFactory("CognifireStudyLogger", "cognifire-study.log") {
  override val formatter: Formatter = object : Formatter() {
    override fun format(record: LogRecord): String =
      String.format(
        "%1\$tF %1\$tT.%1\$tL %1\$tz | %2\$-5s %3\$s%n",
        record.millis,
        record.level,
        record.message.replace("\n", "\\n")
      )
  }
}