package com.jetbrains.edu.jbserver

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.*


class CourseList(

  @JsonProperty("courses")
  val courses: List<Course> = listOf()

)

class SectionList(

  @JsonProperty("sections")
  val sections: List<Section> = listOf()

)

class LessonList(

  @JsonProperty("lessons")
  val lessons: List<Lesson> = listOf()

)

class TaskList(

  @JsonProperty("tasks")
  val tasks: List<Task> = listOf()

)