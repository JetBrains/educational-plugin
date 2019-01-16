package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.EduNames
import java.util.*

@Suppress("unused", "UNUSED_PARAMETER") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE,
                setterVisibility = JsonAutoDetect.Visibility.NONE,
                creatorVisibility = JsonAutoDetect.Visibility.NONE)
abstract class StepikEduCourseMixin {
  @JsonProperty("is_idea_compatible")
  private var isCompatible = true

  @JsonProperty("course_format")
  private lateinit var myType: String

  @JsonProperty("sections")
  var sectionIds: List<Int> = ArrayList()

  @JsonProperty("instructors")
  var instructors: List<Int> = ArrayList()

  @JsonProperty("id")
  private var id: Int = 0

  @JsonProperty("update_date")
  private var myUpdateDate = Date(0)

  @JsonProperty("is_public")
  var isPublic: Boolean = false

  @JsonProperty("summary")
  private var description: String? = null

  @JsonProperty("title")
  private var name: String? = null

  @JsonProperty("programming_language")
  private var myProgrammingLanguage = EduNames.PYTHON

  @JsonProperty("language")
  private var myLanguageCode = "en"

}
