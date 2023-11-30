package com.jetbrains.edu.fleet.frontend.ui

import fleet.frontend.PluginScopeKey
import fleet.frontend.layout.*
import fleet.kernel.ChangeScope
import fleet.util.plus
import noria.NoriaContext
import noria.model.CommonTriggers
import noria.model.performAction
import noria.ui.components.*
import noria.ui.core.dialog
import noria.ui.text.uiText

interface CourseIdDialogEntity : DialogEntity {
  var id: String?
}

fun ChangeScope.showCourseIdDialog(window: WindowEntity, onConfirm: (String?) -> Unit): Int? {
  val dialog = new(CourseIdDialogEntity::class) {
    this.modal = true
    this.closeOn = CloseOn.CANCEL + CloseOn.CLICK_OUTSIDE
    this.defaultAction = { _ ->
      onConfirm(id)
      ConfirmDialogCommand.CLOSE
    }
  }
  showDialog(window, dialog)
  return dialog.id?.toInt()
}

fun NoriaContext.courseIdDialog(courseDialog: CourseIdDialogEntity) {
  val pluginScope = PluginScopeKey.value
  dialog {
    hbox(align = Align.Center) {
      uiText("Enter course id: ")
      gap(width = 2)
      textInput("", onInput = { newValue ->
        pluginScope.changeAsync {
          courseDialog.id = newValue
        }
      })
      gap(width = 2)
      button("Open") {
        actionContext.performAction(CommonTriggers.ConfirmDialog.trigger)
      }
    }
  }
}
