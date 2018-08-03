package com.jetbrains.edu.jbserver

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


fun main(args: Array<String>) {

  println("Running tests...")

  // todo: call tests here
  // serialization : course structure, task, task files
  // deserialization : course structure, task, task files

}
