package com.jetbrains.edu.learning.yaml.format.codeforces

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.jetbrains.edu.learning.json.mixins.NotImplementedInMixin
import com.jetbrains.edu.learning.yaml.format.CourseYamlMixin
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CONTENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.END_DATE_TIME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ENVIRONMENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LANGUAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE_VERSION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAM_TYPE_ID
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SUMMARY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TITLE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE
import com.jetbrains.edu.learning.yaml.format.remote.RemoteCourseBuilder
import com.jetbrains.edu.learning.yaml.format.remote.RemoteStudyItemYamlMixin
import java.time.ZonedDateTime

@Suppress("unused", "LateinitVarOverridesLateinitVar") // used for yaml serialization
@JsonPropertyOrder(TYPE, TITLE, LANGUAGE, SUMMARY, PROGRAMMING_LANGUAGE, PROGRAMMING_LANGUAGE_VERSION, ENVIRONMENT, CONTENT, END_DATE_TIME, PROGRAM_TYPE_ID)
@JsonDeserialize(builder = RemoteCourseBuilder::class)
abstract class CodeforcesCourseYamlMixin : CourseYamlMixin() {
  @JsonProperty(END_DATE_TIME)
  private var endDateTime: ZonedDateTime? = null

  @JsonProperty(PROGRAM_TYPE_ID)
  private var programTypeId: String? = null

  @JsonIgnore
  override lateinit var feedbackLink: String

  @JsonIgnore
  override lateinit var contentTags: List<String>
}

/**
 * Mixin class is used to deserialize remote information of [com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesCourse] item.
 */
@Suppress("unused") // used for json serialization
@JsonPropertyOrder(TYPE, YamlMixinNames.ID, YamlMixinNames.UPDATE_DATE)
abstract class CodeforcesCourseRemoteInfoYamlMixin : RemoteStudyItemYamlMixin() {

  val itemType: String
    @JsonProperty(TYPE)
    get() = throw NotImplementedInMixin()
}