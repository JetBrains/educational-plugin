package com.jetbrains.edu.ai.validation.core.model

import org.apache.commons.csv.CSVRecord

data class StudentSolutionRecord(
  val id: Int,
  val lessonId: Int,
  val taskId: Int,
  val code: String
) {
  companion object {
    fun buildFrom(record: CSVRecord) =
      StudentSolutionRecord(record.get("id").toInt(), record.get("taskId").toInt(), record.get("lessonId").toInt(), record.get("code"))
  }
}