package com.jetbrains.edu.coursecreator.ui

import com.intellij.ide.ui.newItemPopup.NewItemWithTemplatesPopupPanel
import com.intellij.ui.*
import com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES
import com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.StudyItemType
import com.jetbrains.edu.coursecreator.actions.StudyItemVariant
import com.jetbrains.edu.coursecreator.pressEnterToCreateItemMessage
import com.jetbrains.edu.coursecreator.selectItemTypeMessage
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import java.awt.Graphics
import javax.swing.*

class NewStudyItemPopupPanel(
  private val itemType: StudyItemType,
  variants: List<StudyItemVariant>
) : NewItemWithTemplatesPopupPanel<StudyItemVariant>(variants, StudyItemRenderer(itemType)) {

  init {
    // Don't show variant list for single item
    setTemplatesListVisible(variants.size > 1)

    myTemplatesList.addListSelectionListener {
      val selectedValue = myTemplatesList.selectedValue
      if (selectedValue != null) {
        setTextFieldIcon(selectedValue.icon)
      }
    }
    myTemplatesList.selectedIndex = 0
  }

  fun getSelectedItem(): StudyItemVariant? = myTemplatesList.selectedValue

  private fun setTextFieldIcon(icon: Icon) {
    val hintMessage = itemType.pressEnterToCreateItemMessage

    myTextField.setExtensions(TemplateIconExtension(icon), HintExtension(hintMessage, font))
    myTextField.repaint()
  }

  private class TemplateIconExtension(private val icon: Icon) : ExtendableTextComponent.Extension {
    override fun getIcon(hovered: Boolean): Icon = icon
    override fun isIconBeforeText(): Boolean = true
  }

  private class HintExtension(text: String, font: Font): ExtendableTextComponent.Extension {
    // Inspired by Search Everywhere dialog
    private val textIcon = TextIcon(text, JBUI.CurrentTheme.BigPopup.searchFieldGrayForeground(), null, 0)

    init {
      textIcon.setFont(RelativeFont.SMALL.derive(font))
    }

    override fun getIcon(hovered: Boolean): Icon = textIcon
  }
}

private class StudyItemRenderer(private val itemType: StudyItemType) : ListCellRenderer<StudyItemVariant> {

  override fun getListCellRendererComponent(
    list: JList<out StudyItemVariant>,
    value: StudyItemVariant,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    val wrapperPanel = JPanel(BorderLayout())
    wrapperPanel.background = list.background

    val itemPanel = createBaseItemPanel(list, value, isSelected)
    wrapperPanel.add(itemPanel, BorderLayout.CENTER)

    if (index == 0) {
      val separator = createSeparator(list)
      wrapperPanel.add(separator, BorderLayout.NORTH)
    }
    return wrapperPanel
  }

  private fun createBaseItemPanel(list: JList<out StudyItemVariant>, value: StudyItemVariant, isSelected: Boolean): JPanel {

    fun SimpleTextAttributes.withSelectionIfNeeded(): SimpleTextAttributes {
      return if (!isSelected) this else derive(-1, list.selectionForeground, null, null)
    }

    val itemPanel = JPanel(BorderLayout())
    itemPanel.background = if (isSelected) list.selectionBackground else list.background

    val typeComponent = SimpleColoredComponent()
    typeComponent.append(value.type, REGULAR_ATTRIBUTES.withSelectionIfNeeded())
    typeComponent.icon = value.icon

    val descriptionComponent = SimpleColoredComponent()
    descriptionComponent.append(value.description, GRAYED_ATTRIBUTES.withSelectionIfNeeded(), 0, SwingConstants.RIGHT)

    itemPanel.add(typeComponent, BorderLayout.WEST)
    itemPanel.add(descriptionComponent, BorderLayout.EAST)
    return itemPanel
  }

  private fun createSeparator(list: JList<out StudyItemVariant>): SeparatorWithText {
    val separator = object : SeparatorWithText() {
      override fun paintLinePart(g: Graphics?, xMin: Int, xMax: Int, hGap: Int, y: Int) {}
    }
    separator.background = list.background
    separator.font = UIUtil.getLabelFont(UIUtil.FontSize.NORMAL)
    separator.border = JBUI.Borders.empty(UIUtil.DEFAULT_VGAP / 2, 0)
    separator.caption = itemType.selectItemTypeMessage
    separator.setCaptionCentered(false)
    return separator
  }
}
