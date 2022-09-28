@file:JvmName("RemoteEduCourseMixins")
@file:Suppress("unused")

package com.jetbrains.edu.learning.json.mixins

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.courseFormat.Vendor
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.ADDITIONAL_FILES
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.AUTHORS
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.COURSE_TYPE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.CUSTOM_NAME
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.DESCRIPTION_FORMAT
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.DESCRIPTION_TEXT
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.ENVIRONMENT
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.FILES
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.ID
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.IS_PRIVATE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.IS_TEMPLATE_BASED
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.ITEMS
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.LANGUAGE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.MARKETPLACE_COURSE_VERSION
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.NAME
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PLUGINS
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PLUGIN_VERSION
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PROGRAMMING_LANGUAGE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.SOLUTIONS_HIDDEN
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.SOLUTION_HIDDEN
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.SUMMARY
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.TAGS
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.TASK_LIST
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.TASK_TYPE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.TITLE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.TYPE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.UPDATE_DATE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.VENDOR
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.VERSION
import java.util.*

/**
 * If you need to change something in the marketplace course archive format, you should do the following:
 * - Add description to the `educational-core/resources/marketplace/format_description.md`
 * - Create a pull request to the `https://github.com/JetBrains/intellij-plugin-verifier/tree/master/intellij-plugin-structure/structure-edu`
 * and wait for it to be accepted and deployed.
 */

@JsonAutoDetect(setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder(MARKETPLACE_COURSE_VERSION, ENVIRONMENT, SUMMARY, TITLE, PROGRAMMING_LANGUAGE, LANGUAGE, COURSE_TYPE,
                   PLUGIN_VERSION, VENDOR, FEEDBACK_LINK, IS_PRIVATE, SOLUTIONS_HIDDEN, PLUGINS, ITEMS, AUTHORS, TAGS, ID, UPDATE_DATE,
                   ADDITIONAL_FILES, PLUGIN_VERSION, VERSION)
abstract class RemoteEduCourseMixin : LocalEduCourseMixin() {

  @JsonProperty(VENDOR)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private val vendor: Vendor? = null

  @JsonProperty(IS_PRIVATE)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private val isMarketplacePrivate: Boolean = false

  @JsonProperty(MARKETPLACE_COURSE_VERSION)
  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = IntValueFilter::class)
  private val marketplaceCourseVersion: Int = -1

  @JsonProperty(ID)
  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = IntValueFilter::class)
  private var id: Int = 0

  @JsonProperty(UPDATE_DATE)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private lateinit var updateDate: Date

  @JsonProperty(FEEDBACK_LINK)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private lateinit var feedbackLink: String
}

@JsonPropertyOrder(TITLE, CUSTOM_NAME, TAGS, TASK_LIST, IS_TEMPLATE_BASED, TYPE)
abstract class RemoteFrameworkLessonMixin : RemoteLessonMixin() {
  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = TrueValueFilter::class)
  @JsonProperty(IS_TEMPLATE_BASED)
  private var isTemplateBased: Boolean = true
}

@JsonPropertyOrder(ID, TITLE, CUSTOM_NAME, TAGS, TASK_LIST, TYPE)
abstract class RemoteLessonMixin : LocalLessonMixin() {
  @JsonProperty(ID)
  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = IntValueFilter::class)
  private var id: Int = 0
}

@JsonPropertyOrder(ID, NAME, CUSTOM_NAME, TAGS, FILES, DESCRIPTION_TEXT, DESCRIPTION_FORMAT, FEEDBACK_LINK, SOLUTION_HIDDEN, TASK_TYPE)
abstract class RemoteTaskMixin : LocalTaskMixin() {
  @JsonProperty(ID)
  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = IntValueFilter::class)
  private var id: Int = 0
}

@JsonPropertyOrder(ID, TITLE, CUSTOM_NAME, TAGS, ITEMS, TYPE)
abstract class RemoteSectionMixin : LocalSectionMixin() {
  @JsonProperty(ID)
  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = IntValueFilter::class)
  private var id: Int = 0
}
