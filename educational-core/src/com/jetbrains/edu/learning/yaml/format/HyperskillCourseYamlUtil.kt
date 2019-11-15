package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.ID
import com.jetbrains.edu.learning.stepik.hyperskill.api.IDE_FILES
import com.jetbrains.edu.learning.stepik.hyperskill.api.IS_TEMPLATE_BASED
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.HYPERSKILL_PROJECT
import java.util.*


abstract class HyperskillCourseMixin {
  @JsonProperty(HYPERSKILL_PROJECT)
  lateinit var hyperskillProject: HyperskillProject
}

@Suppress("unused")
@JsonPropertyOrder(ID, IDE_FILES, IS_TEMPLATE_BASED)
abstract class HyperskillProjectMixin {
  @JsonIgnore
  private var myId: Int = 0

  @JsonIgnore
  private lateinit var myUpdateDate: Date

  @JsonProperty(ID)
  var id: Int = -1

  @JsonIgnore
  var title: String = ""

  @JsonIgnore
  var description: String = ""

  @JsonProperty(IDE_FILES)
  var ideFiles: String = ""

  @JsonIgnore
  var useIde: Boolean = false

  @JsonIgnore
  var language: String = ""

  @JsonProperty(IS_TEMPLATE_BASED)
  var isTemplateBased: Boolean = false
}