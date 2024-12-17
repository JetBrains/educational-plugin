package com.jetbrains.edu.yaml

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.FRAMEWORK
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.learning.yaml.YamlConfigSettings
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LESSON
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SECTION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TASK
import com.jetbrains.edu.yaml.messages.EduYAMLBundle
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType
import com.jetbrains.jsonSchema.impl.JsonSchemaVersion
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping

class EduYamlSchemaProviderFactory : JsonSchemaProviderFactory {
  override fun getProviders(project: Project): List<StudyItemConfigSchemaProvider> {

    return listOf(
      CourseConfigSchemaProvider(project),
      SectionConfigSchemaProvider(project),
      LessonConfigSchemaProvider(project),
      FrameworkLessonConfigSchemaProvider(project),
      TaskGeneralConfigSchemaProvider(project),
      *getTaskSpecificProviderNames().keys
        .map { TaskSpecificConfigSchemaProvider(project, it) }
        .toTypedArray()
    )
  }

  abstract class StudyItemConfigSchemaProvider(protected val project: Project) : JsonSchemaFileProvider {

    abstract val itemKind: String

    override fun isAvailable(file: VirtualFile): Boolean = CCUtils.isCourseCreator(project)
                                                           && YamlConfigSettings.getLocalConfigFileName(itemKind) == file.name

    override fun getSchemaFile(): VirtualFile? {
      return JsonSchemaProviderFactory.getResourceFile(EduYamlSchemaProviderFactory::class.java, getSchemaResourcePath())
    }

    override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema

    override fun getSchemaVersion(): JsonSchemaVersion = JsonSchemaVersion.SCHEMA_7

    @VisibleForTesting
    open fun getSchemaResourcePath(): String = "/yaml/${itemKind}-schema.json"
  }

  class CourseConfigSchemaProvider(project: Project) : StudyItemConfigSchemaProvider(project) {

    override val itemKind: String = EduNames.COURSE

    override fun getName(): String {
      return EduYAMLBundle.message("yaml.schema.provider.for.course.name")
    }
  }

  class SectionConfigSchemaProvider(project: Project) : StudyItemConfigSchemaProvider(project) {

    override val itemKind: String = SECTION

    override fun getName(): String {
      return EduYAMLBundle.message("yaml.schema.provider.for.section.name")
    }

  }

  class TaskSpecificConfigSchemaProvider(project: Project, private val taskType: String)
    : StudyItemConfigSchemaProvider(project) {

    override val itemKind: String = TASK

    override fun getName(): String {
      return getTaskSpecificProviderNames()[taskType] ?: error("Task specific provider wasn't found for $taskType")
    }

    override fun isAvailable(file: VirtualFile): Boolean {
      val task = file.getContainingTask(project) ?: return false
      return super.isAvailable(file) && task.itemType == taskType
    }

    @VisibleForTesting
    override fun getSchemaResourcePath(): String = "/yaml/${taskType}-${itemKind}-schema.json"
  }

  class TaskGeneralConfigSchemaProvider(project: Project) : StudyItemConfigSchemaProvider(project) {

    override val itemKind: String = TASK
    override fun isAvailable(file: VirtualFile): Boolean {
      // We need to exclude task types with specific Config Schema Provider
      // to make providers mapped one to one for every yaml file.
      val task = file.getContainingTask(project) ?: return false
      return super.isAvailable(file) && task.itemType !in getTaskSpecificProviderNames()
    }

    override fun getName(): String {
      return EduYAMLBundle.message("yaml.schema.provider.for.task.name")
    }
  }

  class FrameworkLessonConfigSchemaProvider(project: Project) : StudyItemConfigSchemaProvider(project) {

    override val itemKind: String = LESSON

    override fun getName(): String = EduYAMLBundle.message("yaml.schema.provider.for.framework.lesson.name")

    override fun isAvailable(file: VirtualFile): Boolean {
      if (!super.isAvailable(file)) return false
      return file.isFrameworkLessonConfig(project)
    }

    override fun getSchemaResourcePath(): String = "/yaml/framework-lesson-schema.json"
  }

  class LessonConfigSchemaProvider(project: Project) : StudyItemConfigSchemaProvider(project) {

    override val itemKind: String = LESSON

    override fun isAvailable(file: VirtualFile): Boolean {
      if (!super.isAvailable(file)) return false
      return !file.isFrameworkLessonConfig(project)
    }

    override fun getName(): String {
      return EduYAMLBundle.message("yaml.schema.provider.for.lesson.name")
    }
  }

  companion object {
    private fun getTaskSpecificProviderNames(): Map<String, String> = mapOf(
      ChoiceTask.CHOICE_TASK_TYPE to EduYAMLBundle.message("yaml.schema.provider.for.choice.task.name")
    )
  }
}

private fun VirtualFile.isFrameworkLessonConfig(project: Project): Boolean {
  return runReadAction {
    val psiFile = PsiManager.getInstance(project).findFile(this) as? YAMLFile ?: return@runReadAction false
    val mapping = psiFile.documents.firstOrNull()?.topLevelValue as? YAMLMapping ?: return@runReadAction false
    val typeKeyValue = mapping.getKeyValueByKey("type") ?: return@runReadAction false
    typeKeyValue.valueText == FRAMEWORK
  }
}
