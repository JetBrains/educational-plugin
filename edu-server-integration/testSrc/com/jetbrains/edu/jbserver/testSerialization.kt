package com.jetbrains.edu.jbserver

import com.fasterxml.jackson.databind.ObjectWriter
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.*


val writer: ObjectWriter = mapper.writerWithDefaultPrettyPrinter()


fun testSerializationTaskFile() {

  testCase("Serialize answer dependency") {
    val obj = AnswerPlaceholderDependency()
    obj.sectionName = "section-name"
    obj.lessonName = "lesson-name"
    obj.taskName = "task-name"
    obj.fileName = "file-name"
    obj.placeholderIndex = 3
    val json = writer.writeValueAsString(obj)
    val answ = readResFile("answer_dependency.json")
    check(jsonEquals(json, answ))
  }

  testCase("Serialize answer placeholder") {
    val dep = AnswerPlaceholderDependency()
    dep.sectionName = "section-dep"
    dep.lessonName = "lesson-dep"
    dep.taskName = "task-dep"
    dep.fileName = "file-dep"
    dep.placeholderIndex = 1
    val obj = AnswerPlaceholder()
    obj.offset = 301
    obj.length = 7
    obj.hints = listOf("hint 1", "hint 2")
    obj.possibleAnswer = "answer"
    obj.placeholderText = "todo"
    obj.placeholderDependency = dep
    val json = writer.writeValueAsString(obj)
    val answ = readResFile("placeholder.json")
    check(jsonEquals(json, answ))
  }

  testCase("Serialize task file") {
    val dep = AnswerPlaceholderDependency()
    dep.sectionName = "section-dep-9645696"
    dep.lessonName = "lesson-dep-5654756"
    dep.taskName = "task-dep-1265799"
    dep.fileName = "file-dep-8633256"
    dep.placeholderIndex = 2
    val ph1 = AnswerPlaceholder()
    ph1.offset = 42
    ph1.length = 7
    ph1.possibleAnswer = "answer-23174611"
    ph1.placeholderText = "todo-67"
    ph1.placeholderDependency = dep
    val ph2 = AnswerPlaceholder()
    ph2.offset = 163
    ph2.length = 5
    ph2.hints = listOf("hint-13", "hint-72")
    ph2.possibleAnswer = "answer-2940345937"
    ph2.placeholderText = "todo-46"
    val obj = TaskFile()
    obj.name = "file-name-3301"
    obj.text = "file-content-08672241"
    obj.answerPlaceholders = listOf(ph1, ph2)
    val json = writer.writeValueAsString(obj)
    val answ = readResFile("taskfile.json")
    check(jsonEquals(json, answ))
  }

}


// Problem: how to test task and course serialization
// if we don't store some fields? fixme


fun testSerializationTask() {

}


fun testSerializationCourse() {

}
