package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.stepik.StepikCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.yaml.YamlMapper.CURRENT_YAML_VERSION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.YAML_VERSION
import org.jetbrains.annotations.VisibleForTesting

class YamlMigrator private constructor(
  private val project: Project,
  private val loadedYamlVersion: Int
) {

  fun needToMigrate(): Boolean = loadedYamlVersion < CURRENT_YAML_VERSION

  /**
   * This method is supposed to fix the structure of YAML if it changed between versions.
   * It is called before the course if loaded, and should operate over configTree.
   */
  fun migrateStructure() {
    // TODO migrate configTree to a new tree and save it to course-info.yaml
  }

  fun migrateModel(course: Course) {
    if (!needToMigrate()) return

    for (version in loadedYamlVersion + 1..CURRENT_YAML_VERSION) {
      updateModelToVersion(version, course)
    }
  }

  @VisibleForTesting
  fun updateModelToVersion(version: Int, course: Course) {
    when (version) {
      1 -> {} // Do nothing. The 1st version only adds "yaml_version: 1" to the end of config.
      2 -> migrateCanCheckLocallyYaml(course)
      3 -> migratePropagatableYamlFields(course)
    }
  }

  private fun migrateCanCheckLocallyYaml(course: Course) {
    val propertyComponent = PropertiesComponent.getInstance(project)
    if (propertyComponent.getBoolean(YAML_MIGRATED)) return
    propertyComponent.setValue(YAML_MIGRATED, true)
    if (course !is HyperskillCourse && course !is StepikCourse) return

    course.visitTasks {
      if (it is ChoiceTask) {
        it.canCheckLocally = false
      }
    }
  }

  private fun migratePropagatableYamlFields(course: Course) {
    if (!CCUtils.isCourseCreator(project)) return
    val propertiesComponent = PropertiesComponent.getInstance(project)
    if (propertiesComponent.getBoolean(YAML_MIGRATED_PROPAGATABLE)) return
    propertiesComponent.setValue(YAML_MIGRATED_PROPAGATABLE, true)

    var hasPropagatableFlag = false
    val nonPropagatableFiles = mutableListOf<TaskFile>()
    course.visitTasks { task: Task ->
      if (task.lesson is FrameworkLesson) {
        for (taskFile in task.taskFiles.values) {
          if (!taskFile.isPropagatable) {
            hasPropagatableFlag = true
            return@visitTasks
          }
          if (!taskFile.isVisible || !taskFile.isEditable) {
            nonPropagatableFiles += taskFile
          }
        }
      }
    }
    if (hasPropagatableFlag) return

    for (taskFile in nonPropagatableFiles) {
      taskFile.isPropagatable = false
    }
  }

  companion object {
    // These fields are keys for the PropertiesComponent, they used to be used before YamlMigrator appeared
    @VisibleForTesting
    const val YAML_MIGRATED_PROPAGATABLE = "Edu.Yaml.Migrate.Propagatable"
    private const val YAML_MIGRATED = "Edu.Yaml.Migrate"

    private val YAML_MAPPER = ObjectMapper(YAMLFactory())

    fun getInstance(project: Project): YamlMigrator? {
      val courseDir = project.courseDir
      val configFile = courseDir.findChild(YamlConfigSettings.COURSE_CONFIG)

      if (configFile == null) {
        logger<YamlMigrator>().warn("Failed to find course-info.yaml config file")
        return null
      }

      val configText = runReadAction {
        VfsUtil.loadText(configFile)
      }

      return getInstance(project, configText)
    }

    @VisibleForTesting
    fun getInstance(project: Project, substitutedYamlVersion: Int): YamlMigrator {
      return YamlMigrator(project, substitutedYamlVersion)
    }

    fun getInstance(project: Project, configText: String): YamlMigrator? {
      val configTree = YAML_MAPPER.readTree(configText) as? ObjectNode

      if (configTree == null) {
        logger<YamlMigrator>().warn("Failed course-info.yaml to load config file")
        return null
      }

      val version = configTree.get(YAML_VERSION)?.asInt(0) ?: 0

      if (version > CURRENT_YAML_VERSION) {
        logger<YamlMigrator>().warn("YAML version of the project is $version which is greater than the latest supported version $CURRENT_YAML_VERSION")
      }

      return YamlMigrator(project, version)
    }
  }
}
