@file:JvmName("RemoteCourseMixins")

package com.jetbrains.edu.coursecreator.actions.mixins

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.util.*

private const val UPDATE_DATE = "update_date"
private const val ID = "id"
private const val STEPIK_ID = "stepic_id"
private const val UNIT_ID = "unit_id"

@Suppress("unused", "UNUSED_PARAMETER") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
@JsonSerialize(using = CourseSerializer::class)
@JsonPropertyOrder(ID, UPDATE_DATE)
abstract class RemoteCourseMixin : LocalCourseMixin() {
  @JsonProperty(ID)
  private var id: Int = 0

  @JsonProperty(UPDATE_DATE)
  private lateinit var myUpdateDate: Date
}

@Suppress("UNUSED_PARAMETER", "unused") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
@JsonSerialize(using = SectionSerializer::class)
abstract class RemoteSectionMixin : LocalSectionMixin() {
  @JsonProperty(ID)
  private var id: Int = 0

  @JsonProperty(UPDATE_DATE)
  private lateinit var myUpdateDate: Date

}

@Suppress("UNUSED_PARAMETER", "unused") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
@JsonSerialize(using = LessonSerializer::class)
abstract class RemoteLessonMixin : LocalLessonMixin() {
  @JsonProperty(ID)
  private var myId: Int = 0

  @JsonProperty(UNIT_ID)
  private var unitId: Int = 0

  @JsonProperty(UPDATE_DATE)
  private lateinit var myUpdateDate: Date

}

@Suppress("UNUSED_PARAMETER", "unused") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
@JsonSerialize(using = TaskSerializer::class)
abstract class RemoteTaskMixin : LocalTaskMixin() {
  @JsonProperty(STEPIK_ID)
  private var myStepId: Int = 0

  @JsonProperty(UPDATE_DATE)
  private lateinit var myUpdateDate: Date

}