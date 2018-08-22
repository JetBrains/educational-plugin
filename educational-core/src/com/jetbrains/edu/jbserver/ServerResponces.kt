package com.jetbrains.edu.jbserver

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.tasks.Task


class CourseList(

  @JsonProperty("courses")
  val courses: List<EduCourse> = listOf()

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
