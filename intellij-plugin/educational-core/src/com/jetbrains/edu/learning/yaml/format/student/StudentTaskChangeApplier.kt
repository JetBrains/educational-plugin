package com.jetbrains.edu.learning.yaml.format.student

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.yaml.errorHandling.YamlLoadingException
import com.jetbrains.edu.learning.yaml.format.TaskChangeApplier


class StudentTaskChangeApplier(project: Project) : TaskChangeApplier(project) {
  override fun applyChanges(existingItem: Task, deserializedItem: Task) {
    if (existingItem.solutionHidden != deserializedItem.solutionHidden && !ApplicationManager.getApplication().isInternal) {
      throw YamlLoadingException(EduCoreBundle.message("yaml.editor.invalid.visibility.cannot.be.changed"))
    }
    super.applyChanges(existingItem, deserializedItem)
    if (existingItem.status != deserializedItem.status && !ApplicationManager.getApplication().isInternal) {
      throw YamlLoadingException(EduCoreBundle.message("yaml.editor.invalid.status.cannot.be.changed"))
    }
    when (existingItem) {
      is ChoiceTask -> {
        existingItem.record = deserializedItem.record
        existingItem.selectedVariants = (deserializedItem as ChoiceTask).selectedVariants
      }
      is EduTask -> {
        existingItem.record = deserializedItem.record
      }
    }
  }

  override fun changeType(project: Project, existingItem: StudyItem, deserializedItem: Task) {
    throw YamlLoadingException(EduCoreBundle.message("yaml.editor.invalid.not.allowed.to.change.task"))
  }
}