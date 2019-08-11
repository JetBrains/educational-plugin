package com.jetbrains.edu.yaml

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
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
                  StudyItemConfigSchemaProvider(project, EduNames.TASK))
  }

  class StudyItemConfigSchemaProvider(private val project: Project, private val itemKind: String) : JsonSchemaFileProvider {
    override fun getName(): String = "${itemKind.capitalize()} Configuration"

    override fun isAvailable(file: VirtualFile): Boolean = CCUtils.isCourseCreator(project)
                                                           && YamlFormatSettings.getLocalConfigFileName(itemKind) == file.name

    override fun getSchemaFile(): VirtualFile? {
      return JsonSchemaProviderFactory.getResourceFile(EduYamlSchemaProviderFactory::class.java, getSchemaResourcePath())
    }

    override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema

    override fun getSchemaVersion(): JsonSchemaVersion = JsonSchemaVersion.SCHEMA_7

    @VisibleForTesting
    fun getSchemaResourcePath(): String = "/yaml/${itemKind}-schema.json"
  }
}
