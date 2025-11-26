package com.jetbrains.edu.coursecreator.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.NlsSafe
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.settings.OptionsProvider

@Suppress("UnstableApiUsage")
@NlsSafe
private const val HTML = "Html"

@Suppress("UnstableApiUsage")
@NlsSafe
private const val MARKDOWN = "Markdown"

class CCOptions : BoundConfigurable(EduCoreBundle.message("ccoptions.display.name")), OptionsProvider {

  override fun createPanel(): DialogPanel {
    val settings = CCSettings.getInstance()
    return panel {
      group(displayName) {
        buttonsGroup {
          row {
            label(EduCoreBundle.message("ccoptions.description.format"))
            radioButton(HTML, value = true)
            radioButton(MARKDOWN, value = false)
          }
        }.bind(settings::useHtmlAsDefaultTaskFormat)
        row {
          checkBox(EduCoreBundle.message("ccoptions.copy.tests"))
            .applyToComponent {
              toolTipText = EduCoreBundle.message("ccoptions.copy.tests.tooltip")
            }
            .bindSelected(settings::copyTestsInFrameworkLessons)
        }
        if (isFeatureEnabled(EduExperimentalFeatures.SPLIT_EDITOR)) {
          row {
            checkBox(EduCoreBundle.message("ccoptions.split.editor"))
              .bindSelected(settings::showSplitEditor)
          }
        }
      }
    }
  }
}
