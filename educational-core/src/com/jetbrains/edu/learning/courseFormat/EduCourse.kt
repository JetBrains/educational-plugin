package com.jetbrains.edu.learning.courseFormat

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Date



class EduCourse : Course() {

  @JsonProperty("id")
  var courseId = -1

  @JsonProperty("last_modified")
  var lastModified = Date(0)

  @JsonProperty("format")
  var format = ""

}



