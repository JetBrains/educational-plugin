package com.jetbrains.edu.javascript.learning.checkio.checker;

import com.intellij.lang.javascript.JavaScriptFileType;
import com.intellij.lang.javascript.JavascriptLanguage;
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOApiConnector;
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOOAuthConnector;
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator;
import com.jetbrains.edu.learning.checkio.checker.CheckiOCheckListener;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import org.jetbrains.annotations.NotNull;

public class JsCheckiOCheckListener extends CheckiOCheckListener {
  public JsCheckiOCheckListener() {
    super(
      new CheckiOCourseContentGenerator(
        JavaScriptFileType.INSTANCE,
        JsCheckiOApiConnector.INSTANCE
      ),
      JsCheckiOOAuthConnector.INSTANCE
    );
  }

  @Override
  protected boolean isEnabledForCourse(@NotNull CheckiOCourse course) {
    return CourseExt.getLanguageById(course) == JavascriptLanguage.INSTANCE;
  }
}
