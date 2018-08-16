package com.jetbrains.edu.jbserver

import java.util.Date
import java.io.File
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task


// Read file from testResources
fun readTestRes(name: String) = File("testResources/$name").readText()


// Compare two json strings semantically
fun jsonEquals(json1: String, json2: String) = jacksonObjectMapper().run {
  readTree(json1) == readTree(json2)
}


// Print study item info
fun StudyItem.info(indent: Int = 0, indentSize: Int = 2): String = when(this) {
  is EduCourse -> buildString {
    if (courseId != 0)
      appendln(" ".repeat(indent) + "- CourseID: $courseId ")
    if (lastModified != Date(0))
      appendln(" ".repeat(indent) + "  Modified: $lastModified ")
    appendln(" ".repeat(indent) + "  Format:   $format ")
    if (name != null)
      appendln(" ".repeat(indent) + "  Title:    $name ")
    if (description != null)
      appendln(" ".repeat(indent) + "  Summary:  $description ")
    appendln(" ".repeat(indent) + "  Language: $languageCode/$languageID ")
    items.forEach { append(it.info(indent + indentSize)) }
  }
  is Section -> buildString {
    appendln(" ".repeat(indent) + "- Section:  ${name ?: "n/a"} ")
    if (id != 0)
      appendln(" ".repeat(indent) + "  ID:       $id ")
    if (updateDate != Date(0))
      appendln(" ".repeat(indent) + "  Modified: $updateDate ")
    items.forEach { append(it.info(indent + indentSize)) }
  }
  is Lesson -> buildString {
    appendln(" ".repeat(indent) + "- Lesson:   ${name ?: "n/a"} ")
    if (id != 0)
      appendln(" ".repeat(indent) + "  ID:       $id ")
    if (updateDate != Date(0))
      appendln(" ".repeat(indent) + "  Modified: $updateDate ")
    taskList.forEach { append(it.info(indent + indentSize)) }
  }
  is Task -> buildString {
    appendln(" ".repeat(indent) + "- TaskID:   $id (ver $versionId) ")
    appendln(" ".repeat(indent) + "  Type:     $taskType")
    if (name != null)
      appendln(" ".repeat(indent) + "  Name:     ${name ?: "n/a"}")
    if (updateDate != Date(0))
      appendln(" ".repeat(indent) + "  Modified: $updateDate")
    if (taskFiles.isNotEmpty())
      appendln(" ".repeat(indent) + "  Files:    ${taskFiles.size}")
  }
  else -> buildString {
    appendln(" ".repeat(indent) + "unknown item `${this.javaClass}`")
  }
}


// Course builder extension
fun Course.asEduCourse() = EduCourse(this)
