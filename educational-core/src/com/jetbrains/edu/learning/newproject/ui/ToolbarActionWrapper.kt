package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.actionSystem.AnAction
import org.jetbrains.annotations.Nls
import java.util.function.Supplier

/**
 * It is used to provide sentence capitalized action text to show it in [BrowseCoursesDialog] dialog toolbar
 */
class ToolbarActionWrapper(@Nls(capitalization = Nls.Capitalization.Sentence) val text: Supplier<String>, val action: AnAction)