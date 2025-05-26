package com.jetbrains.edu.ai.validation.core.model

import org.apache.commons.csv.CSVRecord

data class StudentSolutionRecord(
  val id: Int,
  val lessonId: Int,
  val taskId: Int,
  val code: String
) {
  companion object {
    fun buildFrom(record: CSVRecord) = StudentSolutionRecord(record.get(0).toInt(), record.get(1).toInt(), record.get(2).toInt(), record.get(3))
  }
}