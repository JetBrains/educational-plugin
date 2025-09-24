package com.jetbrains.edu.yaml

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.navigation.NavigateToConfigEntryForEduFileExtension
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ADDITIONAL_FILES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FILES
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence

class NavigateToConfigEntryForEduFileInYAML : NavigateToConfigEntryForEduFileExtension {

  override fun findConfigEntry(configFile: PsiFile, eduFile: EduFile): NavigatablePsiElement? {
    val yamlFile = configFile as? YAMLFile ?: return null

    val root = yamlFile.documents.firstOrNull() ?.topLevelValue as? YAMLMapping ?: return null

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