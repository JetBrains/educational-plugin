package com.jetbrains.edu.yaml

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.yaml.YamlFormatSettings
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType
import com.jetbrains.jsonSchema.impl.JsonSchemaVersion

class EduYamlSchemaProviderFactory : JsonSchemaProviderFactory {
  override fun getProviders(project: Project): List<StudyItemConfigSchemaProvider> {
    return listOf(StudyItemConfigSchemaProvider(project, EduNames.COURSE),
                  StudyItemConfigSchemaProvider(project, EduNames.SECTION),
                  StudyItemConfigSchemaProvider(project, EduNames.LESSON),
                  TaskSpecificConfigSchemaProvider(project, ChoiceTask.CHOICE_TASK_TYPE),
                  TaskGeneralConfigSchemaProvider(project, listOf(ChoiceTask.CHOICE_TASK_TYPE)))
  }

  open class StudyItemConfigSchemaProvider(protected val project: Project, protected val itemKind: String) : JsonSchemaFileProvider {
    override fun getName(): String = "${itemKind.capitalize()} Configuration"

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

    override fun getName(): String = "${taskType.capitalize()} ${super.getName()}"

    override fun isAvailable(file: VirtualFile): Boolean {
      val task = EduUtils.getTaskForFile(project, file) ?: return false
      return super.isAvailable(file) && task.itemType == taskType
    }

    @VisibleForTesting
    override fun getSchemaResourcePath(): String = "/yaml/${taskType}-${itemKind}-schema.json"
  }

  class TaskGeneralConfigSchemaProvider(project: Project, private val typesForIgnore: List<String>)
    : StudyItemConfigSchemaProvider(project, EduNames.TASK) {

    override fun isAvailable(file: VirtualFile): Boolean {
      // We need exclude task types with specific Config Schema Provider
      // to make providers mapped one to one for every yaml file.
      val task = EduUtils.getTaskForFile(project, file) ?: return false
      return super.isAvailable(file) && typesForIgnore.find { it == task.itemType } == null
    }
  }
}
