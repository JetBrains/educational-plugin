package com.jetbrains.edu.learning.courseFormat

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.EMAIL
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.NAME
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.URL


class Vendor {

  constructor()

  constructor(vendorName: String) {
    name = vendorName
  }

  @JsonProperty(NAME)
  var name: String = ""

  @JsonProperty(EMAIL)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  var email: String? = null

  @JsonProperty(URL)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  var url: String? = null

  override fun toString(): String = name
}
