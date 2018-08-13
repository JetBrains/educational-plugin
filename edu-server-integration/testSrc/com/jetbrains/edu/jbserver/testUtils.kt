package com.jetbrains.edu.jbserver

import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.*


val indentSize = 2


fun StudyItem.print(indent: Int = 0) {
  if (this is Course) {
    println(" ".repeat(indent) + "Name: $name")
    println(" ".repeat(indent) + "Desc: $description")
    println(" ".repeat(indent) + "Lang: $languageCode/$languageID")
    items.forEach {
      it.print(indent + indentSize)
    }
  }
  if (this is Section) {
    println(" ".repeat(indent) + "ID: $id")
    println(" ".repeat(indent) + "Type: section")
    println(" ".repeat(indent) + "Name: $name")
    println(" ".repeat(indent) + "LMT: $updateDate")
    items.forEach {
      it.print(indent + indentSize)
    }
  }
  if (this is Lesson) {
    println(" ".repeat(indent) + "ID: $id")
    println(" ".repeat(indent) + "Type: lesson")
    println(" ".repeat(indent) + "Name: $name")
    println(" ".repeat(indent) + "LMT: $updateDate")
    taskList.forEach {
      it.print(indent + indentSize)
    }

  }
  if (this is Task) {
    println(" ".repeat(indent) + "ID: $id")
    println(" ".repeat(indent) + "Task type: $taskType")
    println(" ".repeat(indent) + "Name: $name")
    println(" ".repeat(indent) + "LMT: $updateDate")
  }
}
