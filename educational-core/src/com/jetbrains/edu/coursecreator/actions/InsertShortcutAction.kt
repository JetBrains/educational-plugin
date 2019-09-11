package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ex.QuickListsManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.keymap.KeymapUtil.getActiveKeymapShortcuts
import com.intellij.openapi.keymap.impl.ui.ActionsTreeUtil
import com.intellij.openapi.keymap.impl.ui.Group
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.OnePixelDivider
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.*
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.speedSearch.FilteringListModel
import com.intellij.ui.speedSearch.NameFilteringListModel
import com.intellij.ui.speedSearch.SpeedSearch
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduUtils
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

@Suppress("ComponentNotRegistered")
open class InsertShortcutAction : AnAction("Insert Shortcut", "Inserts shortcut to render in Task Description", null) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val editor = CommonDataKeys.EDITOR.getData(e.dataContext) ?: return

    val allActions = mutableSetOf<AnAction>()

    ProgressManager.getInstance().run(object : Task.Modal(project, "Collecting shortcuts", false) {
      override fun run(indicator: ProgressIndicator) {
        runReadAction {
          val mainGroup = ActionsTreeUtil.createMainGroup(project,
                                                          KeymapManager.getInstance().activeKeymap,
                                                          QuickListsManager.instance.allQuickLists,
                                                          "",
                                                          true, null)


          mainGroup.collectAllActions(allActions)
        }
      }
    })


    val callback = object : (AnAction) -> Unit {
      var balloon: JBPopup? = null

      override fun invoke(action: AnAction) {
        runInEdt {
          WriteCommandAction.runWriteCommandAction(project) {
            editor.document.insertString(editor.caretModel.offset,
                                         "${EduUtils.SHORTCUT_ENTITY}${ActionManager.getInstance().getId(action)};")
          }
        }
        balloon?.closeOk(null)
      }
    }
    val listWithSearchField = ListWithSearchField(allActions, callback)

    callback.balloon = createAndShowBalloon(listWithSearchField, project)
  }

  protected open fun createAndShowBalloon(listWithSearchField: ListWithSearchField, project: Project): JBPopup {
    val balloon = JBPopupFactory.getInstance().createComponentPopupBuilder(listWithSearchField, listWithSearchField.searchField)
      .setProject(project)
      .setCancelOnClickOutside(true)
      .setRequestFocus(true)
      .createPopup()
    balloon.showCenteredInCurrentWindow(project)
    return balloon
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    val virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(e.dataContext) ?: return
    e.presentation.isEnabledAndVisible = CCUtils.isCourseCreator(project) && EduUtils.isTaskDescriptionFile(virtualFile.name)
  }

  private fun Group.collectAllActions(allActions: MutableSet<AnAction>) {
    for (child in children) {
      if (child is Group) {
        child.collectAllActions(allActions)
      }
      else {
        val actionId = child.toString()
        if (getActiveKeymapShortcuts(actionId).shortcuts.isNotEmpty()) {
          val action = ActionManager.getInstance().getAction(actionId) ?: error("Action $actionId has shortcut, but can't be found")
          if (action.templateText != null) {
            allActions.add(action)
          }
        }
      }
    }
  }

  protected class ListWithSearchField(actions: Set<AnAction>, private val elementSelectedCallback: (AnAction) -> Unit) : JPanel(BorderLayout()) {
    val searchField: SearchTextField
    private val speedSearch: SpeedSearch
    val list: JList<AnAction> = JBList(actions)
    private val filteringModel: FilteringListModel<AnAction>

    init {
      speedSearch = SpeedSearchForTextField()
      filteringModel = NameFilteringListModel(list.model, AnAction::getTemplateText, speedSearch::shouldBeShowing) {
        speedSearch.filter ?: ""
      }
      searchField = BorderlessSearchTextField()
      initializeList()
      add(createScrollPane(), BorderLayout.CENTER)
      add(searchField, BorderLayout.NORTH)
    }

    private fun createScrollPane(): JScrollPane {
      val scrollPane = ScrollPaneFactory.createScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
      scrollPane.border = null
      ScrollingUtil.installActions(list, searchField)
      return scrollPane
    }

    private fun initializeList() {
      list.model = filteringModel
      list.addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
          if (e.clickCount > 1) {
            elementSelectedCallback(list.selectedValue)
          }
        }
      })

      list.addKeyListener(object : KeyAdapter() {
        override fun keyPressed(e: KeyEvent?) {
          if (KeyEvent.VK_ENTER == e?.keyCode) {
            elementSelectedCallback(list.selectedValue)
          }
        }
      })

      list.cellRenderer = ActionListCellRenderer()
    }

    private inner class SpeedSearchForTextField : SpeedSearch() {
      init {
        installSupplyTo(list)
        setEnabled(true)
      }

      override fun update() {
        searchField.text = filter
        filteringModel.refilter()
      }

      override fun processKeyEvent(e: KeyEvent?) {
        super.processKeyEvent(e)

        // SpeedSearch isn't designed to work with JTextField:
        // there is only "backspace released" event in this case,
        // but SpeedSearch itself handles only "backspace pressed"
        if (KeyEvent.VK_BACK_SPACE == e?.keyCode) {
          backspace()
          e.consume()
          update()
        }

        if (KeyEvent.VK_ENTER == e?.keyCode && list.selectedIndex != -1) {
          elementSelectedCallback(list.selectedValue)
        }
      }
    }

    private inner class BorderlessSearchTextField : SearchTextField(false) {
      init {
        val empty = JBUI.Borders.empty()
        textEditor.border = empty
        textEditor.putClientProperty("JTextField.Search.Gap", 0)
        val bottomLine = JBUI.Borders.customLine(OnePixelDivider.BACKGROUND, 0, 0, 1, 0)
        border = JBUI.Borders.merge(empty, bottomLine, true)
        background = list.background
        textEditor.addKeyListener(speedSearch)
      }

      override fun onFieldCleared() {
        speedSearch.reset()
      }
    }
  }

  private class ActionListCellRenderer : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(list: JList<*>?,
                                              value: Any?,
                                              index: Int,
                                              isSelected: Boolean,
                                              cellHasFocus: Boolean): Component {
      val panel = JPanel(BorderLayout())
      panel.border = JBUI.Borders.empty(2)
      panel.background = UIUtil.getListBackground(isSelected, cellHasFocus)
      panel.toolTipText = "Double-click on selected item or press 'Enter'"

      val anAction = value as AnAction
      val presentation = anAction.templatePresentation

      panel.add(createIconLabel(presentation.icon), BorderLayout.WEST)
      panel.add(JBLabel(presentation.text), BorderLayout.CENTER)
      panel.add(createShortcutComponent(anAction), BorderLayout.EAST)

      return panel
    }

    private fun createShortcutComponent(anAction: AnAction): JComponent {
      val shortcuts = getActiveKeymapShortcuts(ActionManager.getInstance().getId(anAction)).shortcuts
      val shortcutText = KeymapUtil.getPreferredShortcutText(shortcuts)

      val shortcutComponent = SimpleColoredComponent()
      shortcutComponent.append(shortcutText, SimpleTextAttributes.GRAY_ATTRIBUTES)
      return shortcutComponent
    }

    private fun createIconLabel(icon: Icon?): JLabel {
      val layeredIcon = LayeredIcon(2)
      layeredIcon.setIcon(EmptyIcon.ICON_18, 0)
      val iconLabel = JLabel(layeredIcon)
      if (icon != null) {
        val width = icon.iconWidth
        val height = icon.iconHeight
        layeredIcon.setIcon(icon, 1, (EmptyIcon.ICON_18.iconWidth - width) / 2,
                            (EmptyIcon.ICON_18.iconHeight - height) / 2)
      }
      iconLabel.border = JBUI.Borders.empty(0, 2, 0, 2)
      return iconLabel
    }
  }
}

