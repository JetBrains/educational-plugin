package com.jetbrains.edu.jbserver

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.io.File


// Read file from testResources
fun readTestRes(name: String) =
  File("testResources/$name").readText()


// Compare two json strings semantically
fun jsonEquals(json1: String, json2: String): Boolean {
  val compareMapper = jacksonObjectMapper()
  val tree1 = compareMapper.readTree(json1)
  val tree2 = compareMapper.readTree(json2)
  return tree1 == tree2
}


// Print study item
fun StudyItem.print(indent: Int = 0, indentSize: Int = 2): Unit = when (this) {
  is EduCourse -> {
    println(" ".repeat(indent) + "ID: $courseId")
    println(" ".repeat(indent) + "Name: $name")
    println(" ".repeat(indent) + "Desc: $description")
    println(" ".repeat(indent) + "Lang: $languageCode/$languageID")
    items.forEach {
      it.print(indent + indentSize)
    }
  }
  is Section -> {
    println(" ".repeat(indent) + "Type: section")
    println(" ".repeat(indent) + "ID: $id")
    println(" ".repeat(indent) + "Name: $name")
    println(" ".repeat(indent) + "LMT: $updateDate")
    items.forEach {
      it.print(indent + indentSize)
    }
  }
  is Lesson -> {
    println(" ".repeat(indent) + "Type: lesson")
    println(" ".repeat(indent) + "ID: $id")
    println(" ".repeat(indent) + "Name: $name")
    println(" ".repeat(indent) + "LMT: $updateDate")
    taskList.forEach {
      it.print(indent + indentSize)
    }
  }
  is Task -> {
    println(" ".repeat(indent) + "ID: $id")
    println(" ".repeat(indent) + "VID: $versionId")
    println(" ".repeat(indent) + "Task type: $taskType")
    println(" ".repeat(indent) + "Name: $name")
    println(" ".repeat(indent) + "LMT: $updateDate")
  }
  else -> {
    println(" ".repeat(indent) + "unknown item `${this.javaClass}`")
  }
}


// Course builder extension
// fun Course.asEduCourse() = EduCourse().apply { fromCourse(this) }
fun Course.asEduCourse(): EduCourse {
  val course = EduCourse()
  course.fromCourse(this)
  return course
}
