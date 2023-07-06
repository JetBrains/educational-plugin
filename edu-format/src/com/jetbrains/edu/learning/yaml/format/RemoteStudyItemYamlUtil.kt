package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ID
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.UPDATE_DATE
import java.util.*

/**
 * Mixin class is used to deserialize remote information of [com.jetbrains.edu.learning.courseFormat.StudyItem] item stored on Stepik.
 */
@Suppress("unused") // used for json serialization
@JsonPropertyOrder(ID, UPDATE_DATE)
abstract class RemoteStudyItemYamlMixin {
  @JsonProperty(ID)
  private var id: Int = 0

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "EEE, dd MMM yyyy HH:mm:ss zzz")
  @JsonProperty(UPDATE_DATE)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private lateinit var updateDate: Date
}
