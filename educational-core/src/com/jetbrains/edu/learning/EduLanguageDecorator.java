package com.jetbrains.edu.learning;

import com.intellij.lang.LanguageExtension;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface EduLanguageDecorator {

  String EP_NAME = "Educational.languageDecorator";
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
}
