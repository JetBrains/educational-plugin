package com.jetbrains.edu.learning.courseFormat

import com.fasterxml.jackson.annotation.JsonInclude
import java.util.Date
import java.util.ArrayList
import com.fasterxml.jackson.annotation.JsonProperty


class EduCourse() : Course() {

  @JsonProperty("id")
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  var courseId = 0

  @JsonProperty("last_modified")
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  var lastModified = Date(0)

  @JsonProperty("format")
  var format = "1.9-2018.3-3301"

  constructor(course: Course) : this() {
    name = course.name
    description = course.description
    language = course.language
    languageCode = course.languageCode
    items = ArrayList(course.items)
  }

}
