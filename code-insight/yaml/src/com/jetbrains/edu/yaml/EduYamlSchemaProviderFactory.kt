package com.jetbrains.edu.yaml

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.learning.yaml.YamlFormatSettings
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LESSON
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SECTION
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType
import com.jetbrains.jsonSchema.impl.JsonSchemaVersion
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping

class EduYamlSchemaProviderFactory : JsonSchemaProviderFactory {
  override fun getProviders(project: Project): List<StudyItemConfigSchemaProvider> {
    val tasksWithSpecificProvider = setOf(ChoiceTask.CHOICE_TASK_TYPE)

    return listOf(
      StudyItemConfigSchemaProvider(project, EduNames.COURSE),
      StudyItemConfigSchemaProvider(project, SECTION),
      LessonConfigSchemaProvider(project),
      FrameworkLessonConfigSchemaProvider(project),
      TaskGeneralConfigSchemaProvider(project, tasksWithSpecificProvider),
      *tasksWithSpecificProvider
        .map { TaskSpecificConfigSchemaProvider(project, it) }
        .toTypedArray()
    )
  }

  open class StudyItemConfigSchemaProvider(protected val project: Project, protected val itemKind: String) : JsonSchemaFileProvider {
    override fun getName(): String = "${itemKind.replaceFirstChar { it.titlecaseChar() }} Configuration"

    override fun isAvailable(file: VirtualFile): Boolean = CCUtils.isCourseCreator(project)
                                                           && YamlFormatSettings.getLocalConfigFileName(itemKind) == file.name

    override fun getSchemaFile(): VirtualFile? {
      return JsonSchemaProviderFactory.getResourceFile(EduYamlSchemaProviderFactory::class.java, getSchemaResourcePath())
    }

    override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema

    override fun getSchemaVersion(): JsonSchemaVersion = JsonSchemaVersion.SCHEMA_7

    @VisibleForTesting
    open fun getSchemaResourcePath(): String = "/yaml/${itemKind}-schema.json"
  }

  class TaskSpecificConfigSchemaProvider(project: Project, private val taskType: String)
    : StudyItemConfigSchemaProvider(project, EduNames.TASK) {

    override fun getName(): String = "${taskType.replaceFirstChar { it.titlecaseChar() }} ${super.getName()}"

    override fun isAvailable(file: VirtualFile): Boolean {
      val task = file.getContainingTask(project) ?: return false
      return super.isAvailable(file) && task.itemType == taskType
    }

    @VisibleForTesting
    override fun getSchemaResourcePath(): String = "/yaml/${taskType}-${itemKind}-schema.json"
  }

  class TaskGeneralConfigSchemaProvider(project: Project, private val tasksWithSpecificProvider: Collection<String>)
    : StudyItemConfigSchemaProvider(project, EduNames.TASK) {

    override fun isAvailable(file: VirtualFile): Boolean {
      // We need to exclude task types with specific Config Schema Provider
      // to make providers mapped one to one for every yaml file.
      val task = file.getContainingTask(project) ?: return false
      return super.isAvailable(file) && !tasksWithSpecificProvider.contains(task.itemType)
    }
  }

  class FrameworkLessonConfigSchemaProvider(project: Project) : StudyItemConfigSchemaProvider(project, LESSON) {

    override fun getName(): String = "Framework Lesson Configuration"

    override fun isAvailable(file: VirtualFile): Boolean {
      if (!super.isAvailable(file)) return false
      return file.isFrameworkLessonConfig(project)
    }

    override fun getSchemaResourcePath(): String = "/yaml/framework-lesson-schema.json"
  }

  class LessonConfigSchemaProvider(project: Project) : StudyItemConfigSchemaProvider(project, EduNames.LESSON) {
    override fun isAvailable(file: VirtualFile): Boolean {
      if (!super.isAvailable(file)) return false
      return !file.isFrameworkLessonConfig(project)
    }
  }
}

private fun VirtualFile.isFrameworkLessonConfig(project: Project): Boolean {
  return runReadAction {
    val psiFile = PsiManager.getInstance(project).findFile(this) as? YAMLFile ?: return@runReadAction false
    val mapping = psiFile.documents.firstOrNull()?.topLevelValue as? YAMLMapping ?: return@runReadAction false
    val typeKeyValue = mapping.getKeyValueByKey("type") ?: return@runReadAction false
    typeKeyValue.valueText == EduNames.FRAMEWORK
  }
}
