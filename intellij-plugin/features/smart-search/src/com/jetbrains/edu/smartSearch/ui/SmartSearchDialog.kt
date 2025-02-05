package com.jetbrains.edu.smartSearch.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.dsl.builder.*
import com.jetbrains.edu.smartSearch.messages.EduSmartSearchBundle
import javax.swing.JComponent

class SmartSearchDialog : DialogWrapper(true) {
  private var searchQuery: String = ""

  private var collection: Collection? = Collection.DEFAULT

  private var documents: Documents? = Documents.DEFAULT

  init {
    title = EduSmartSearchBundle.message("dialog.smart.search.title")
    isOKActionEnabled = true
    init()
  }

  override fun createCenterPanel(): JComponent = panel {
    row {
      textArea()
        .resizableColumn()
        .bindText(::searchQuery)
        .columns(COLUMNS_MEDIUM)
        .cellValidation {
          addInputRule(EduSmartSearchBundle.message("dialog.smart.search.error.empty")) {
            it.text.isEmpty() || it.text.isBlank()
          }
          addInputRule(EduSmartSearchBundle.message("dialog.smart.search.error.large")) {
            it.text.trim().split(" ").size > 10
          }
        }
        .align(Align.FILL)
        .applyToComponent {
          emptyText.text = EduSmartSearchBundle.message("dialog.smart.search.query.placeholder")
        }
    }
    collapsibleGroup(EduSmartSearchBundle.message("dialog.smart.search.settings")) {
      row(EduSmartSearchBundle.message("dialog.smart.search.settings.collection")) {
        comboBox(CollectionComboBoxModel(Collection.values().toList(), collection))
          .bindItem(::collection)
      }
      row(EduSmartSearchBundle.message("dialog.smart.search.settings.number.of.documents")) {
        comboBox(CollectionComboBoxModel(Documents.values().toList(), documents))
          .bindItem(::documents)
      }
    }.apply {
      packWindowHeight = true
    }
  }

  fun showAndGetWithSearchQuery(): SmartSearchDialogResult? {
    if (!showAndGet()) return null
    return SmartSearchDialogResult(
      searchQuery,
      collection?.type ?: Collection.DEFAULT.type,
      documents?.numberOfDocuments ?: Documents.DEFAULT.numberOfDocuments
    )
  }

  enum class Collection(val type: String, private val presentableName: String) {
    ALL_TASK_DESCRIPTIONS("all_task_descriptions", "All Task Descriptions"),
    EDU_TASK_DESCRIPTIONS("edu_task_descriptions", "EduTask Task Descriptions"),
    EDU_TASK_VISIBLE_CODE("edu_task_visible_code", "Edu Task Visible Code Files"),
    EDU_TASK_VISIBLE_CODE_JOINED("edu_task_visible_code_joined", "Edu Task Visible Code Files (Joined)");

    override fun toString(): String = presentableName

    companion object {
      val DEFAULT: Collection = EDU_TASK_DESCRIPTIONS
    }
  }

  enum class Documents(val numberOfDocuments: Int) {
    THREE(3), FIVE(5), TEN(10);

    override fun toString(): String = numberOfDocuments.toString()

    companion object {
      val DEFAULT: Documents = THREE
    }
  }

  data class SmartSearchDialogResult(
    val searchQuery: String,
    val collectionName: String = Collection.DEFAULT.type,
    val numberOfDocuments: Int = Documents.DEFAULT.numberOfDocuments,
  )
}