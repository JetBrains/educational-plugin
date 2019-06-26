package com.jetbrains.edu.yaml

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer.isCourseConfigFile
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType
import com.jetbrains.jsonSchema.impl.JsonSchemaVersion

class EduYamlSchemaProviderFactory : JsonSchemaProviderFactory {
  override fun getProviders(project: Project): List<JsonSchemaFileProvider> {
    return listOf(object : JsonSchemaFileProvider {
      override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema

      override fun getName(): String = "Course Configuration"

      override fun isAvailable(file: VirtualFile): Boolean = file.isCourseConfigFile

      override fun isUserVisible(): Boolean = false

      override fun getSchemaVersion(): JsonSchemaVersion = JsonSchemaVersion.SCHEMA_7

      override fun getSchemaFile(): VirtualFile? {
        val resourcePath = "/yaml/course-schema.json"
        if (isUnitTestMode) {
          val path = VfsUtilCore.urlToPath(EduYamlSchemaProviderFactory::class.java.getResource(resourcePath).path)
          VfsRootAccess.allowRootAccess(project, path)
        }
        return JsonSchemaProviderFactory.getResourceFile(EduYamlSchemaProviderFactory::class.java, resourcePath)
      }
    })
  }
}