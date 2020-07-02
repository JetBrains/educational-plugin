package com.jetbrains.edu.coursecreator.taskDescription

import com.intellij.codeInsight.template.impl.DefaultLiveTemplatesProvider

class HintTemplateProvider : DefaultLiveTemplatesProvider {
  override fun getDefaultLiveTemplateFiles() = arrayOf("liveTemplates/hint.xml")

  override fun getHiddenLiveTemplateFiles() = emptyArray<String>()
}
