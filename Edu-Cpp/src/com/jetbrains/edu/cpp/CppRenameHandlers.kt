package com.jetbrains.edu.cpp

import com.jetbrains.edu.coursecreator.handlers.CCLessonRenameHandler
import com.jetbrains.edu.coursecreator.handlers.CCSectionRenameHandler
import com.jetbrains.edu.coursecreator.handlers.CCTaskRenameHandler

class CppTaskRenameHandler : CCTaskRenameHandler() {
  override fun performCustomNameValidation(name: String): String? = validateStudyItemName(name)
}

class CppLessonRenameHandler : CCLessonRenameHandler() {
  override fun performCustomNameValidation(name: String): String? = validateStudyItemName(name)
}

class CppSectionRenameHandler : CCSectionRenameHandler() {
  override fun performCustomNameValidation(name: String): String? = validateStudyItemName(name)
}