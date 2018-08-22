package com.jetbrains.edu.learning.courseFormat

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.pluginVersion
import java.util.*


class EduCourse() : Course() {

  @JsonProperty("id")
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  var courseId = 0

  @JsonProperty("last_modified")
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  var lastModified = Date(0)

  @JsonProperty("format")
  var format = pluginVersion(EduNames.PLUGIN_ID) ?: "n/a"

  constructor(course: Course) : this() {
    name = course.name
    description = course.description
    language = course.language
    languageCode = course.languageCode
    items = ArrayList(course.items)
  }

}
