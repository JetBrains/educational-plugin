package com.jetbrains.edu.learning;

import com.intellij.lang.LanguageExtension;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.EmptyIcon;
import com.jetbrains.edu.coursecreator.ui.CCNewCoursePanel;
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public interface EduLanguageDecorator {

  String EP_NAME = "educational.languageDecorator";

  LanguageExtension<EduLanguageDecorator> INSTANCE = new LanguageExtension<>(EP_NAME);

  /**
   * Used for code highlighting in Task Description tool window.
   * Scripts for different languages can be found <a href="https://codemirror.net">here</a>.
   *
   * For example, <a href="https://github.com/JetBrains/educational-plugins/tree/master/Edu-Python/resources/python.js">highlighting script</a> for python language.
   */
  @NotNull
  default String getLanguageScriptUrl() {
    return "";
  }

  /**
   * Used for code highlighting in Task Description tool window
   *
   * @return parameter for CodeMirror script. Available languages: @see <@linktourl http://codemirror.net/mode/>
   */
  @NotNull
  default String getDefaultHighlightingMode() {
    return "";
  }

  /**
   * Gets tag color for decorator language.
   *
   * If it returns null then color for language will be taken
   * from educational-core/resources/languageColors/colors.json.
   * Original color list can be found <a href="https://github.com/ozh/github-colors/blob/master/colors.json">here</a>
   *
   * @return tag color for decorator language
   */
  @Nullable
  default JBColor languageTagColor() {
    return null;
  }

  /**
   * Returns icon for decorator language.
   * This icon is used in places where course is associated with language.
   * For example, 'Browse Courses' and 'Create New Course' dialogs.
   *
   * @return 16x16 icon
   *
   * @see CoursesPanel
   * @see CCNewCoursePanel
   */
  @NotNull
  default Icon getLogo() {
    return EmptyIcon.ICON_16;
  }
}
