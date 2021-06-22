package com.jetbrains.edu.learning.codeforces

import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import javax.swing.Icon

class CodeforcesFileTemplateDescriptor(fileName: String, icon: Icon, private val templateName: String): FileTemplateDescriptor(fileName, icon) {
  override fun getDisplayName(): String {
    return templateName
  }
}