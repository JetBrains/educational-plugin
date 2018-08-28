package com.jetbrains.edu.javascript.learning.checkio.checker;

import com.intellij.lang.javascript.JavaScriptFileType;
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOApiConnector;
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOOAuthConnector;
import com.jetbrains.edu.javascript.learning.checkio.utils.JsCheckiONames;
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator;
import com.jetbrains.edu.learning.checkio.checker.CheckiOCheckListener;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import org.jetbrains.annotations.NotNull;

public class JsCheckiOCheckListener extends CheckiOCheckListener {
  public JsCheckiOCheckListener() {
    super(
      new CheckiOCourseContentGenerator(
        JavaScriptFileType.INSTANCE,
        JsCheckiOApiConnector.getInstance()
      ),
      JsCheckiOOAuthConnector.getInstance()
    );
  }

  @Override
  protected boolean isEnabledForCourse(@NotNull CheckiOCourse course) {
    return course.getLanguageID().equals(JsCheckiONames.JS_CHECKIO_LANGUAGE);
  }
}
