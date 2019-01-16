package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

@Suppress("unused", "UNUSED_PARAMETER") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE,
                setterVisibility = JsonAutoDetect.Visibility.NONE,
                creatorVisibility = JsonAutoDetect.Visibility.NONE)
abstract class StepikEduCourseMixin {
  @JsonProperty("is_idea_compatible")
  var isCompatible = true

  @JsonProperty("course_format")
  lateinit var myType: String

  @JsonProperty("sections")
  lateinit var sectionIds: List<Int>

  @JsonProperty("instructors")
  lateinit var instructors: List<Int>

  @JsonProperty("id")
  private var id: Int = 0

  @JsonProperty("update_date")
  lateinit var myUpdateDate: Date

  @JsonProperty("is_public")
  var isPublic: Boolean = false

  @JsonProperty("summary")
  lateinit var description: String

  @JsonProperty("title")
  lateinit var name: String

  @JsonProperty("programming_language")
  lateinit var myProgrammingLanguage: String

  @JsonProperty("language")
  lateinit var myLanguageCode: String
}

@Suppress("unused", "UNUSED_PARAMETER") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE,
                setterVisibility = JsonAutoDetect.Visibility.NONE,
                creatorVisibility = JsonAutoDetect.Visibility.NONE)
class StepikSectionMixin {
  @JsonProperty("units")
  lateinit var units: List<Int>

  @JsonProperty("course")
  var courseId: Int = 0

  @JsonProperty("title")
  lateinit var name: String

  @JsonProperty("position")
  var position: Int = 0

  @JsonProperty("id")
  private var id: Int = 0

  @JsonProperty("update_date")
  lateinit var updateDate: Date
}
