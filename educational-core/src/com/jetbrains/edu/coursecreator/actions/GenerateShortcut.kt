package com.jetbrains.edu.coursecreator.actions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ide.DataManager
import com.intellij.ide.util.gotoByName.ChooseByNamePopup
import com.intellij.ide.util.gotoByName.ChooseByNamePopupComponent
import com.intellij.ide.util.gotoByName.GotoActionItemProvider
import com.intellij.ide.util.gotoByName.GotoActionModel
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.impl.ActionManagerImpl
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.MinusculeMatcher
import com.intellij.psi.codeStyle.NameUtil
import com.intellij.util.Processor
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.JBIterable
import com.jetbrains.edu.coursecreator.CCUtils
import java.awt.Component
import java.util.*
import javax.swing.JList
import javax.swing.ListCellRenderer


class GenerateShortcut : IntentionAction {
  override fun startInWriteAction() = false

  override fun getFamilyName() = "educator"

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
    //TODO: available only in task description files
    return CCUtils.isCourseCreator(project)
  }

  override fun getText(): String = "Insert shortcut"

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
    val gotoActionModel = GotoActionWithShortcutModel(project, editor!!)
    val provider = object : GotoActionItemProvider(gotoActionModel) {
      override fun filterElements(pattern: String?, consumer: Processor<GotoActionModel.MatchedValue>?): Boolean {
        pattern ?: return false
        val actionManager = ActionManager.getInstance() as ActionManagerImpl
        val actionIds = actionManager.actionIds
        val matcher = NameUtil.buildMatcher("*" + pattern, NameUtil.MatchingCaseSensitivity.NONE)
        val actions = JBIterable.from(actionIds).filterMap {
          val action = actionManager.getAction(it)
          action?: return@filterMap null
          if (KeymapUtil.getFirstKeyboardShortcutText(action).isEmpty()) return@filterMap null
          return@filterMap action
        }
        val actionWrappers = actions.unique().filterMap {
          val matchMode = gotoActionModel.actionMatches(pattern, matcher, it!!)
          if (matchMode != GotoActionModel.MatchMode.NAME) {
            return@filterMap null
          }
          return@filterMap GotoActionModel.ActionWrapper(it, null, matchMode, DataManager.getInstance().getDataContext(editor.component), gotoActionModel)
        }

        val matched = ContainerUtil.newArrayList<GotoActionModel.MatchedValue>(
          actionWrappers.map({ o ->
                               if (o is GotoActionModel.MatchedValue) o
                               else o?.let { GotoActionModel.MatchedValue(it, pattern) }
                             }))

        Collections.sort<GotoActionModel.MatchedValue>(matched)
        return ContainerUtil.process(matched, consumer!!)
      }
    }
    val popup = object : ChooseByNamePopup(project, gotoActionModel, provider, null, null, false, 0) {
      override fun isCheckboxVisible() = false
    }
    popup.invoke(object : ChooseByNamePopupComponent.Callback() {
      override fun elementChosen(element: Any?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
      }

    }, ModalityState.current(), false)
  }

  class GotoActionWithShortcutModel(project: Project, editor: Editor) : GotoActionModel(project, editor.component, editor) {
    override fun getPromptText() = "Enter action"
    public override fun actionMatches(pattern: String, matcher: MinusculeMatcher?, anAction: AnAction): MatchMode {
      return super.actionMatches(pattern, matcher, anAction)
    }

    override fun getListCellRenderer(): ListCellRenderer<*> {
      return object: GotoActionListCellRenderer(this::getGroupName) {
        override fun getListCellRendererComponent(list: JList<*>,
                                                  matchedValue: Any?,
                                                  index: Int,
                                                  isSelected: Boolean,
                                                  cellHasFocus: Boolean): Component {
//          if (matchedValue is String) {
//            return super.getListCellRendererComponent(list, matchedValue, index, isSelected, cellHasFocus)
//          }
          val value = (matchedValue as MatchedValue).value
          val actionWithParentGroup = value as ActionWrapper
          val anAction = actionWithParentGroup.action
          val presentation = anAction.templatePresentation
          val oldEnabled = presentation.isEnabled
          val oldVisible = presentation.isVisible
          presentation.isEnabledAndVisible = true
          val component = super.getListCellRendererComponent(list, matchedValue, index, isSelected, cellHasFocus)
          presentation.isEnabled = oldEnabled
          presentation.isVisible = oldVisible
          return component
        }
      }
    }
  }
}