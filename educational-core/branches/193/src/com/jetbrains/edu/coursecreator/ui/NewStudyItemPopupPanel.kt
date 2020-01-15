package com.jetbrains.edu.coursecreator.ui

import com.intellij.ide.ui.newItemPopup.NewItemWithTemplatesPopupPanel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.jetbrains.edu.coursecreator.actions.StudyItemVariant
import javax.swing.Icon
import javax.swing.JList

class NewStudyItemPopupPanel(
  variants: List<StudyItemVariant>,
  selectedItem: StudyItemVariant? = null
) : NewItemWithTemplatesPopupPanel<StudyItemVariant>(variants, RENDERER) {

  init {
    myTemplatesList.addListSelectionListener { e ->
      val selectedValue = myTemplatesList.selectedValue
      if (selectedValue != null) {
        setTextFieldIcon(selectedValue.icon)
      }
    }
    selectTemplate(selectedItem)
  }

  fun getSelectedItem(): StudyItemVariant? = myTemplatesList.selectedValue

  private fun selectTemplate(selectedItem: StudyItemVariant?) {
    if (selectedItem == null) {
      myTemplatesList.selectedIndex = 0
      return
    }

    val model = myTemplatesList.model
    for (i in 0 until model.size) {
      val templateID = model.getElementAt(i)
      if (selectedItem == templateID) {
        myTemplatesList.selectedIndex = i
        return
      }
    }
  }

  private fun setTextFieldIcon(icon: Icon) {
    myTextField.setExtensions(TemplateIconExtension(icon))
    myTextField.repaint()
  }

  private class TemplateIconExtension(private val icon: Icon) : ExtendableTextComponent.Extension {
    override fun getIcon(hovered: Boolean): Icon = icon
    override fun isIconBeforeText(): Boolean = true
  }
}

private val RENDERER = object: SimpleListCellRenderer<StudyItemVariant>() {
  override fun customize(list: JList<out StudyItemVariant>, value: StudyItemVariant, index: Int, selected: Boolean, hasFocus: Boolean) {
    text = value.type
    icon = value.icon
  }
}
