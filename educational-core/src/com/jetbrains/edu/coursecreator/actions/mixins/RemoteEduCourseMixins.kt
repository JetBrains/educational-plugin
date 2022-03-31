@file:JvmName("RemoteEduCourseMixins")
@file:Suppress("unused")

package com.jetbrains.edu.coursecreator.actions.mixins

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonAppend
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.introspect.AnnotatedClass
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter
import com.fasterxml.jackson.databind.util.Annotations
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.ADDITIONAL_FILES
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.AUTHORS
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.COURSE_TYPE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.ENVIRONMENT
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.ID
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.IS_PRIVATE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.IS_TEMPLATE_BASED
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.ITEMS
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.LANGUAGE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.PLUGINS
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.PLUGIN_VERSION
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.PROGRAMMING_LANGUAGE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.SOLUTIONS_HIDDEN
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.SUMMARY
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.TAGS
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.TASK_LIST
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.TITLE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.TYPE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.UPDATE_DATE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.VENDOR
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.VERSION
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Vendor
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.marketplace.api.MARKETPLACE_COURSE_VERSION
import com.jetbrains.edu.learning.pluginVersion
import com.jetbrains.edu.learning.serialization.IntValueFilter
import com.jetbrains.edu.learning.serialization.TrueValueFilter
import java.util.*

/**
 * If you need to change something in the marketplace course archive format, you should do the following:
 * - Add description to the `educational-core/resources/marketplace/format_description.md`
 * - Create a pull request to the `https://github.com/JetBrains/intellij-plugin-verifier/tree/master/intellij-plugin-structure/structure-edu`
 * and wait for it to be accepted and deployed.
 */

@JsonAutoDetect(setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder(MARKETPLACE_COURSE_VERSION, VERSION, ENVIRONMENT, SUMMARY, TITLE, PROGRAMMING_LANGUAGE, LANGUAGE, COURSE_TYPE,
                   PLUGIN_VERSION, VENDOR, FEEDBACK_LINK, IS_PRIVATE, SOLUTIONS_HIDDEN, PLUGINS, ITEMS, AUTHORS, TAGS, ID, UPDATE_DATE,
                   ADDITIONAL_FILES)
@JsonAppend(props = [JsonAppend.Prop(VersionPropertyWriter::class, name = VERSION, type = Int::class),
  JsonAppend.Prop(PluginVersionPropertyWriter::class, name = PLUGIN_VERSION, type = String::class)])
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

@JsonPropertyOrder(TITLE, TAGS, TASK_LIST, IS_TEMPLATE_BASED, TYPE)
abstract class RemoteFrameworkLessonMixin : RemoteLessonMixin() {
  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = TrueValueFilter::class)
  @JsonProperty(IS_TEMPLATE_BASED)
  private var isTemplateBased: Boolean = true
}

@JsonPropertyOrder(ID)
abstract class RemoteLessonMixin : LocalLessonMixin() {
  @JsonProperty(ID)
  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = IntValueFilter::class)
  private var id: Int = 0
}

@JsonPropertyOrder(ID)
abstract class RemoteTaskMixin : LocalTaskMixin() {
  @JsonProperty(ID)
  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = IntValueFilter::class)
  private var id: Int = 0
}

@JsonPropertyOrder(ID, TITLE, TAGS, ITEMS, TYPE)
abstract class RemoteSectionMixin : LocalSectionMixin() {
  @JsonProperty(ID)
  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = IntValueFilter::class)
  private var id: Int = 0
}

private class PluginVersionPropertyWriter : VirtualBeanPropertyWriter {

  constructor()

  constructor(propDef: BeanPropertyDefinition, contextAnnotations: Annotations, declaredType: JavaType) : super(propDef,
                                                                                                                contextAnnotations,
                                                                                                                declaredType)

  override fun withConfig(config: MapperConfig<*>?,
                          declaringClass: AnnotatedClass,
                          propDef: BeanPropertyDefinition,
                          type: JavaType): VirtualBeanPropertyWriter {
    return PluginVersionPropertyWriter(propDef, declaringClass.annotations, type)
  }

  override fun value(bean: Any, gen: JsonGenerator, prov: SerializerProvider): Any {
    return if (isUnitTestMode) TEST_PLUGIN_VERSION else pluginVersion(EduNames.PLUGIN_ID) ?: "unknown"
  }

  companion object {
    private const val TEST_PLUGIN_VERSION = "yyyy.2-yyyy.1-TEST"
  }
}
