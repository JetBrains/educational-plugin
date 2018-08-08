package com.jetbrains.edu.jbserver

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.*


class CourseList(

  @JsonProperty("courses")
  val courses: List<Course> = listOf()

)
