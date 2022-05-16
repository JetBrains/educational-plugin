package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.DocumentAdapter
import javax.swing.event.DocumentEvent
import javax.swing.text.JTextComponent

abstract class CCDialogWrapperBase : DialogWrapper, Validator {

  protected val textValidators = mutableListOf<JTextComponent>()

  constructor(project: Project?) : super(project)
  constructor(canBeParent: Boolean) : super(canBeParent)

  protected fun addTextValidator(component: JTextComponent) {
    component.document.addDocumentListener(object : DocumentAdapter() {
      override fun textChanged(e: DocumentEvent) {
        updateErrorInfo(doValidateAll())
      }
    })
    textValidators.add(component)
  }

  override fun doValidateAll(): List<ValidationInfo> {
    val validationInfos = ArrayList(super.doValidateAll())
    for (component in textValidators) {
      val errorMessage = validate(component.text) ?: continue
      validationInfos += ValidationInfo(errorMessage, component)
    }
    return validationInfos
  }
}

interface Validator {
  fun validate(componentText: String?): String?
}