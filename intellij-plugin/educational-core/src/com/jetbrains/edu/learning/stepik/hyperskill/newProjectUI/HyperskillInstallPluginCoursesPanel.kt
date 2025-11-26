package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillStartupActivity.Companion.NEW_HYPERSKILL_PLUGIN_INFO_LINK
import kotlinx.coroutines.CoroutineScope
import javax.swing.Icon
import javax.swing.JPanel

class HyperskillInstallPluginCoursesPanel(
  platformProvider: HyperskillInstallPluginPlatformProvider,
  scope: CoroutineScope,
  disposable: Disposable
) : CoursesPanel(platformProvider, scope, disposable) {
  override fun createNoCoursesPanel(disposable: Disposable): JPanel = object : Wrapper() {
    init {
      val panel = panel {
        row {
          cell(createTextPanel())
        }
        row {
          cell(HyperskillInstallPluginInteractivePanel(disposable))
        }
      }.apply {
        border = JBEmptyBorder(JBUI.scale(40))
      }
      setContent(panel)
    }

    private fun createTextPanel(): DialogPanel = panel {
      row {
        icon(warningIcon)
      }
      row {
        text(EduCoreBundle.message("hyperskill.new.plugin.courses.panel.text.first")).applyToComponent {
          font = JBFont.h3().asBold()
        }
      }
      row {
        text(EduCoreBundle.message("hyperskill.new.plugin.courses.panel.text.second")).applyToComponent {
          font = JBFont.regular()
        }
      }
      row {
        browserLink(EduCoreBundle.message("hyperskill.new.plugin.courses.panel.link.text"), NEW_HYPERSKILL_PLUGIN_INFO_LINK)
      }
    }

    private val warningIcon: Icon
      get() = IconUtil.resizeSquared(AllIcons.General.BalloonWarning, JBUI.scale(24))
  }
}