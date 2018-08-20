package com.jetbrains.edu.jbserver

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import com.jetbrains.edu.learning.courseFormat.TaskFile
import org.junit.Test
import org.junit.Assert.assertTrue as check


class JacksonSerializationTest : EduTestCase() {

  val writer = jacksonObjectMapper()
    .setupMapper()
    .writerWithDefaultPrettyPrinter()

  @Test
  fun `test answer dependency`() {
    val obj = AnswerPlaceholderDependency()
    obj.sectionName = "section-name"
    obj.lessonName = "lesson-name"
    obj.taskName = "task-name"
    obj.fileName = "file-name"
    obj.placeholderIndex = 3
    val json = writer.writeValueAsString(obj)
    val answ = readTestRes("answer_dependency.json")
    check(jsonEquals(json, answ))
  }

  @Test
  fun `test answer placeholder`() {
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
    val answ = readTestRes("placeholder.json")
    check(jsonEquals(json, answ))
  }

  @Test
  fun `test task file`() {
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
    val answ = readTestRes("taskfile.json")
    check(jsonEquals(json, answ))
  }

  // todo: use dsl to construct objects
  
  @Test
  fun `test differential update`() {

    val course = course {
      withName("Sample course")
      withDescription("This is some course description")
      lesson("First lesson") {
        withId(465)
        theoryTask ("PA #1.1") { }
        outputTask ("PA #1.2") { }
        eduTask("PA #1.3") { }
      }
      section("Part 1") {
        withId(545)
        lesson("Second lesson") {}
        lesson("Third lesson") {}
      }
      section("Part 2") {
        lesson("Fourth lesson") {
          withId(659)
        }
        lesson("Fourth lesson") {
          eduTask("PA #4.1") {
            withId(6545)
          }
          outputTask("PA #4.2") {
            withId(6546)
          }
          eduTask("PA #4.3") {
            withTaskDescription("new task 1")
          }
          outputTask("PA #4.4") {
            withTaskDescription("new task 2")
          }
        }
      }
    }.asEduCourse()

    val json = writer.writeValueAsString(course)
    check(jsonEquals(json, readTestRes("course_update.json")))
  }

}