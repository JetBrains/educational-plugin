package com.jetbrains.edu.coursecreator.ui

import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.Document

abstract class CCSyncTextField : JBTextField() {

  private var myIsChangedByUser: Boolean = false
  private var myIsSyncEnabled: Boolean = true

  var complementaryTextField: CCSyncTextField? = null

  private val documentListener: DocumentListener = object : DocumentAdapter() {
    override fun textChanged(e: DocumentEvent) {
      myIsChangedByUser = true
      sync()
    }
  }

  init {
    document?.addDocumentListener(object : DocumentAdapter() {
      override fun textChanged(e: DocumentEvent) {
        myIsChangedByUser = true
        sync()
      }
    })
  }

  val isChangedByUser: Boolean get() = myIsChangedByUser

  override fun setDocument(doc: Document?) {
    document?.removeDocumentListener(documentListener)
    super.setDocument(doc)
    doc?.addDocumentListener(documentListener)
  }

  fun setTextManually(text: String) {
    val isChangedByUser = myIsChangedByUser
    myIsSyncEnabled = false
    setText(text)
    myIsSyncEnabled = true
    myIsChangedByUser = isChangedByUser
  }

  protected fun sync() {
    val complementaryTextField = complementaryTextField ?: return
    if (myIsSyncEnabled && !complementaryTextField.myIsChangedByUser) {
      doSync(complementaryTextField)
    }
  }

  protected abstract fun doSync(complementaryTextField: CCSyncTextField)
}
