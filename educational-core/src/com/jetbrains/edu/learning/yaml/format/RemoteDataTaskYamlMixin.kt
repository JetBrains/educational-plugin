@file:Suppress("unused") // used for yaml serialization
package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.courseFormat.attempts.DataTaskAttempt
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask
import com.jetbrains.edu.learning.json.mixins.NotImplementedInMixin
import com.jetbrains.edu.learning.stepik.api.ATTEMPT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.END_DATE_TIME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ID
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.UPDATE_DATE
import java.util.*

@JsonPropertyOrder(TYPE, ID, UPDATE_DATE, ATTEMPT)
abstract class RemoteDataTaskYamlMixin : RemoteStudyItemYamlMixin() {
  val itemType: String
    @JsonProperty(TYPE)
    get() = throw NotImplementedInMixin()

  @JsonProperty(ATTEMPT)
  private var attempt: DataTaskAttempt? = null
}

@JsonPropertyOrder(ID, END_DATE_TIME)
abstract class DataTaskAttemptYamlMixin {
  @JsonProperty(ID)
  private var id: Int = -1

  @JsonIgnore
  private lateinit var time: Date

  @JsonIgnore
  private var timeLeft: Long = 0

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "EEE, dd MMM yyyy HH:mm:ss zzz")
  @JsonProperty(END_DATE_TIME)
  private lateinit var endDateTime: Date
}

class RemoteDataTaskChangeApplier : RemoteInfoChangeApplierBase<DataTask>() {
  override fun applyChanges(existingItem: DataTask, deserializedItem: DataTask) {
    super.applyChanges(existingItem, deserializedItem)
    existingItem.attempt = deserializedItem.attempt
  }
}