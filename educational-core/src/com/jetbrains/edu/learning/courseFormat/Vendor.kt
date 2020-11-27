package com.jetbrains.edu.learning.courseFormat

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.EMAIL
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.NAME
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.URL


class Vendor {

  constructor()

  constructor(organization: String) {
    name = organization
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