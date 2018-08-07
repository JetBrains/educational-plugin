package com.jetbrains.edu.jbserver

import java.io.File
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.*


val resPath = "edu-server-integration/testResources"


val mapper = jacksonObjectMapper()
  .addMixIn(StudyItem::class.java, StudyItemMixin::class.java)
  .addMixIn(Course::class.java, CourseMixin::class.java)
  .addMixIn(Section::class.java, SectionMixin::class.java)
  .addMixIn(Lesson::class.java, LessonMixin::class.java)
  .addMixIn(Task::class.java, TaskMixIn::class.java)
  .addMixIn(TheoryTask::class.java, TheoryTaskMixIn::class.java)
  .addMixIn(IdeTask::class.java, IdeTaskMixIn::class.java)
  .addMixIn(OutputTask::class.java, OutputTaskMixIn::class.java)
  .addMixIn(EduTask::class.java, EduTaskMixIn::class.java)
  .addMixIn(CodeTask::class.java, CodeTaskMixIn::class.java)
  .addMixIn(ChoiceTask::class.java, ChoiceTaskMixIn::class.java)
  .addMixIn(TaskFile::class.java, TaskFileMixin::class.java)
  .addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderMixin::class.java)
  .addMixIn(AnswerPlaceholderDependency::class.java, AnswerPlaceholderDependencyMixin::class.java)
  .disable(MapperFeature.AUTO_DETECT_CREATORS)
  .disable(MapperFeature.AUTO_DETECT_FIELDS)
  .disable(MapperFeature.AUTO_DETECT_GETTERS)
  .disable(MapperFeature.AUTO_DETECT_SETTERS)
  .disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
  .setSerializationInclusion(JsonInclude.Include.NON_NULL)


fun testCase(name: String, test: () -> Unit) {
  try {
    test()
  } catch (e: IllegalStateException) {
    println("Test `$name` failed (check).")
  } catch (e: Exception) {
    println("Test `$name` failed (exception):")
    e.printStackTrace()
  }
}


fun jsonEquals(json1: String, json2: String): Boolean {
  val compareMapper = jacksonObjectMapper()
  val tree1 = compareMapper.readTree(json1)
  val tree2 = compareMapper.readTree(json2)
  return tree1 == tree2
}


fun readResFile(filename: String) =
  File("$resPath/$filename").readText()


fun main(args: Array<String>) {

  testDeserializationCourse()
  testDeserializationTaskFile()
  testDeserializationTask()

  testSerializationTaskFile()
  testSerializationTask()
  testSerializationCourse()

}
