package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.DocumentAdapter
import javax.swing.event.DocumentEvent
import javax.swing.text.JTextComponent

abstract class CCDialogWrapperBase : DialogWrapper {

  private val textValidators: MutableMap<JTextComponent, (String?) -> String?> = HashMap()

  constructor(project: Project?) : super(project)
  constructor(canBeParent: Boolean) : super(canBeParent)

  protected fun addTextValidator(component: JTextComponent, validator: (String?) -> String?) {
    component.document.addDocumentListener(object : DocumentAdapter() {
      override fun textChanged(e: DocumentEvent) {
        updateErrorInfo(doValidateAll())
      }
    })
    textValidators[component] = validator
  }

  override fun doValidateAll(): List<ValidationInfo> {
    val validationInfos = ArrayList(super.doValidateAll())
    for ((component, validator) in textValidators) {
      val errorMessage = validator(component.text) ?: continue
      validationInfos += ValidationInfo(errorMessage, component)
    }
    return validationInfos
  }
}
