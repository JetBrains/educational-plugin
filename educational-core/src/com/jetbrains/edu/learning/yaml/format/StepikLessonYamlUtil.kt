@file:JvmName("StepikLessonYamlUtil")
@file:Suppress("unused")

package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.jetbrains.edu.learning.stepik.course.StepikLesson
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CONTENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ID
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.UNIT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.UPDATE_DATE

@JsonDeserialize(builder = StepikLessonBuilder::class)
abstract class StepikLessonYamlMixin : LessonYamlMixin() {
  val itemType: String
    @JsonProperty(YamlMixinNames.TYPE)
    get() = throw NotImplementedInMixin()
}

@JsonPOJOBuilder(withPrefix = "")
open class StepikLessonBuilder(
  @JsonProperty(CONTENT) content: List<String?> = emptyList(),
  @JsonProperty(CUSTOM_NAME) customName: String? = null
) : LessonBuilder(content, customName) {
  override fun createLesson(): StepikLesson = StepikLesson()
}

@JsonPropertyOrder(ID, UPDATE_DATE, UNIT)
abstract class StepikLessonRemoteYamlMixin : RemoteStudyItemYamlMixin() {
  @JsonProperty(UNIT)
  private var unitId: Int = 0
}

class StepikLessonChangeApplier : RemoteInfoChangeApplierBase<StepikLesson>() {
  override fun applyChanges(existingItem: StepikLesson, deserializedItem: StepikLesson) {
    super.applyChanges(existingItem, deserializedItem)
    existingItem.unitId = deserializedItem.unitId
  }
}