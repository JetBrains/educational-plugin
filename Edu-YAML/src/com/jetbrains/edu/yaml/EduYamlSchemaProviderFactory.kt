package com.jetbrains.edu.yaml

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType
import com.jetbrains.jsonSchema.impl.JsonSchemaVersion

class EduYamlSchemaProviderFactory : JsonSchemaProviderFactory {
  override fun getProviders(project: Project): List<JsonSchemaFileProvider> {
    return listOf(StudyItemConfigSchemaProvider(project, EduNames.COURSE),
                  StudyItemConfigSchemaProvider(project, EduNames.SECTION),
                  StudyItemConfigSchemaProvider(project, EduNames.LESSON),
                  StudyItemConfigSchemaProvider(project, EduNames.TASK))
  }

  private class StudyItemConfigSchemaProvider(private val project: Project, private val itemKind: String) : JsonSchemaFileProvider {
    override fun getName(): String = "${itemKind.capitalize()} Configuration"

    override fun isAvailable(file: VirtualFile): Boolean = YamlFormatSettings.getLocalConfigFileName(itemKind) == file.name

    override fun getSchemaFile(): VirtualFile? {
      val resourcePath = "/yaml/${itemKind}-schema.json"
      if (isUnitTestMode) {
        val path = VfsUtilCore.urlToPath(EduYamlSchemaProviderFactory::class.java.getResource(resourcePath).path)
        VfsRootAccess.allowRootAccess(project, path)
      }
      return JsonSchemaProviderFactory.getResourceFile(EduYamlSchemaProviderFactory::class.java, resourcePath)
    }

    override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema

    override fun getSchemaVersion(): JsonSchemaVersion = JsonSchemaVersion.SCHEMA_7
  }
}