package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.stepik.hyperskill.api.*
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.HYPERSKILL_PROJECT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.STAGES
import java.util.*

@JsonPropertyOrder(HYPERSKILL_PROJECT, STAGES)
abstract class HyperskillCourseMixin {
  @JsonProperty(HYPERSKILL_PROJECT)
  lateinit var hyperskillProject: HyperskillProject

  @JsonProperty(STAGES)
  var stages: List<HyperskillStage> = mutableListOf()
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

@JsonPropertyOrder(ID, STEP_ID)
class HyperskillStageMixin {
  @JsonProperty(ID)
  var id: Int = -1

  @JsonIgnore
  var title: String = ""

  @JsonProperty(STEP_ID)
  var stepId: Int = -1
}