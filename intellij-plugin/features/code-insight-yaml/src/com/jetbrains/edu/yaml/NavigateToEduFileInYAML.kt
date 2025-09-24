package com.jetbrains.edu.yaml

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.navigation.NavigateToEduFileExtension
import com.jetbrains.edu.learning.yaml.configFile
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ADDITIONAL_FILES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FILES
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLValue

class NavigateToEduFileInYAML : NavigateToEduFileExtension {
  override fun navigateToEduFile(project: Project, eduFile: EduFile): Boolean {
    val itemWithConfig = if (eduFile is TaskFile) {
      eduFile.task
    }
    else {
      project.course ?: return false
    }

    val configToOpen = itemWithConfig.configFile(project) ?: return false

    val yamlElement = runReadAction {
      findNameElement(project, eduFile, configToOpen)
    } ?: return false

    yamlElement.navigate(true)

    return true
  }

  private fun findNameElement(project: Project, eduFile: EduFile, configToOpen: VirtualFile): YAMLValue? {
    val yamlFile = PsiManager.getInstance(project).findFile(configToOpen) as? YAMLFile ?: return null

    val root = yamlFile.documents.getOrNull(0)
      ?.topLevelValue as? YAMLMapping ?: return null

    val fieldWithFiles = if (eduFile is TaskFile) {
      FILES
    }
    else {
      ADDITIONAL_FILES
    }

    val filesList = root.getKeyValueByKey(fieldWithFiles)?.value as? YAMLSequence ?: return null
    val namesElements = filesList.items.mapNotNull {
      val fileConfig = it.value as? YAMLMapping
      fileConfig?.getKeyValueByKey(YamlMixinNames.NAME)?.value as? YAMLScalar
    }

    return namesElements.find { it.textValue == eduFile.name }
  }
}