@file:JvmName("CourseExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.ide.plugins.InstalledPluginsState
import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.compatibility.CourseCompatibility
import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProvider
import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProviderEP
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.HyperskillCourseAdvertiser
import kotlin.math.max

val Course.configurator: EduConfigurator<*>? get() {
  val language = languageById ?: return null
  return EduConfiguratorManager.findConfigurator(itemType, environment, language)
}

val Course.compatibilityProvider: CourseCompatibilityProvider?
  get() {
    return CourseCompatibilityProviderEP.find(languageId, environment)
  }

val Course.sourceDir: String? get() = configurator?.sourceDir
val Course.testDirs: List<String> get() = configurator?.testDirs.orEmpty()

val Course.project: Project? get() {
  for (project in ProjectManager.getInstance().openProjects) {
    if (this == StudyTaskManager.getInstance(project).course) {
      return project
    }
  }
  return null
}

val Course.hasSections: Boolean get() = sections.isNotEmpty()

val Course.hasTopLevelLessons: Boolean get() = lessons.isNotEmpty()

val Course.allTasks: List<Task> get() {
  val allTasks = mutableListOf<Task>()
  course.visitTasks { allTasks += it }
  return allTasks
}

val Course.languageDisplayName: String get() = languageById?.displayName ?: languageId

val Course.technologyName: String?
  get() = compatibilityProvider?.technologyName ?: languageById?.displayName

val Course.supportedTechnologies: List<String>
  get() {
    return when (this) {
      is HyperskillCourseAdvertiser -> this.supportedLanguages
      else -> if (technologyName != null) listOf(technologyName!!) else emptyList()
    }
  }

val Course.tags: List<Tag>
  get() {
    val tags = mutableListOf<Tag>()
    if (course is HyperskillCourseAdvertiser) {
      tags.addAll((this as HyperskillCourseAdvertiser).supportedLanguages.map { ProgrammingLanguageTag(it) })
      tags.add(HumanLanguageTag(humanLanguage))
      return tags
    }

    technologyName?.let { tags.add(ProgrammingLanguageTag(it)) }
    tags.add(HumanLanguageTag(humanLanguage))

    if (course is EduCourse) {
      if (visibility is CourseVisibility.FeaturedVisibility) {
        tags.add(FeaturedTag())
      }
    }
    return tags
  }

val Course.languageById: Language?
  get() = Language.findLanguageByID(languageId)


val Course.isPreview: Boolean
  get() = this is EduCourse && isPreview

val Course.compatibility: CourseCompatibility
  get() {
    if (this is HyperskillCourseAdvertiser) {
      return CourseCompatibility.Compatible
    }

    // @formatter:off
    return versionCompatibility() ?:
           pluginCompatibility() ?:
           configuratorCompatibility() ?:
           CourseCompatibility.Compatible
    // @formatter:on
  }

private fun Course.versionCompatibility(): CourseCompatibility? {
  if (this !is EduCourse) {
    return null
  }

  if (languageId.isEmpty()) {
    return CourseCompatibility.Unsupported
  }

  val maximalSupportedVersion = max(JSON_FORMAT_VERSION, JSON_FORMAT_VERSION_WITH_FILES_OUTSIDE)

  if (formatVersion > maximalSupportedVersion) {
    return CourseCompatibility.IncompatibleVersion
  }

  return null
}

// projectLanguage parameter should be passed only for hyperskill courses because for Hyperskill
// it can differ from the course.programmingLanguage
fun Course.validateLanguage(projectLanguage: String = languageId): Result<Unit, CourseValidationResult> {
  val pluginCompatibility = pluginCompatibility()
  if (pluginCompatibility is CourseCompatibility.PluginsRequired) {
    return Err(PluginsRequired(pluginCompatibility.toInstallOrEnable))
  }

  if (configurator == null) {
    val message = EduCoreBundle.message("rest.service.language.not.supported", ApplicationNamesInfo.getInstance().productName,
                                        projectLanguage.capitalize())
    return Err(ValidationErrorMessage(message))
  }
  return Ok(Unit)
}

private fun Course.pluginCompatibility(): CourseCompatibility? {
  val requiredPlugins = mutableListOf<PluginInfo>()
  compatibilityProvider?.requiredPlugins()?.let { requiredPlugins.addAll(it) }
  for (pluginInfo in course.pluginDependencies) {
    if (requiredPlugins.find { it.stringId == pluginInfo.stringId } != null) {
      continue
    }
    requiredPlugins.add(pluginInfo)
  }

  if (requiredPlugins.isEmpty()) {
    return null
  }

  val pluginsState = InstalledPluginsState.getInstance()

  // TODO: O(requiredPlugins * allPlugins) because PluginManager.getPlugin takes O(allPlugins).
  //  Can be improved at least to O(requiredPlugins * log(allPlugins))
  val loadedPlugins = PluginManager.getLoadedPlugins()
  val notLoadedPlugins = requiredPlugins
    .mapNotNull {
      if (pluginsState.wasInstalledWithoutRestart(PluginId.getId(it.stringId))) {
        return@mapNotNull null
      }

      val pluginDescriptor = PluginManagerCore.getPlugin(PluginId.getId(it.stringId))
      if (pluginDescriptor == null || pluginDescriptor !in loadedPlugins) {
        it to pluginDescriptor
      }
      else {
        null
      }
    }

  val toInstallOrEnable = notLoadedPlugins.filter { (info, pluginDescriptor) ->
    // Plugin is just installed and not loaded by IDE (i.e. it requires restart)
    pluginDescriptor == null && !pluginsState.wasInstalled(PluginId.getId(info.stringId)) ||
    // Plugin is installed but disabled
    pluginDescriptor?.isEnabled == false
  }

  return if (notLoadedPlugins.isNotEmpty()) CourseCompatibility.PluginsRequired(toInstallOrEnable.map { it.first }) else null
}

private fun Course.configuratorCompatibility(): CourseCompatibility? {
  return if (configurator == null) CourseCompatibility.Unsupported else null
}

fun Course.updateEnvironmentSettings(project: Project, configurator: EduConfigurator<*>? = this.configurator) {
  course.environmentSettings += configurator?.getEnvironmentSettings(project).orEmpty()
}

fun Course.visitEduFiles(visitor: (EduFile) -> Unit) {
  visitTasks { task ->
    for (taskFile in task.taskFiles.values) {
      visitor(taskFile)
    }
  }

  for (additionalFile in additionalFiles) {
    visitor(additionalFile)
  }
}