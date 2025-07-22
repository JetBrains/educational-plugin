/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.edu.learning.settings

import com.intellij.openapi.extensions.BaseExtensionPointName
import com.intellij.openapi.options.CompositeConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ex.ConfigurableWrapper
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.NlsContexts.ConfigurableName
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.JComponent
import javax.swing.JPanel

class EduConfigurable : CompositeConfigurable<OptionsProvider>(), Configurable.WithEpDependencies {
  private val mainPanel: JPanel = JPanel(VerticalFlowLayout())

  @ConfigurableName
  override fun getDisplayName(): String = EduCoreBundle.message("settings.education")
  override fun getHelpTopic(): String = ID

  override fun createComponent(): JComponent {
    mainPanel.removeAll()
    for (provider in configurables) {
      val component = provider.createComponent() ?: continue
      mainPanel.add(component)
    }
    return mainPanel
  }

  override fun createConfigurables(): List<OptionsProvider> {
    return ConfigurableWrapper.createConfigurables(OptionsProvider.EP_NAME).filter { it.isAvailable() }
  }

  override fun getDependencies(): Collection<BaseExtensionPointName<*>> = listOf(OptionsProvider.EP_NAME)

  companion object {
    const val ID = "com.jetbrains.edu.learning.stepik.EduConfigurable"
  }
}
