package com.jetbrains.edu.python.learning.checkio.pycharm;

import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiOSdkValidator;
import com.jetbrains.edu.python.learning.pycharm.PyLanguageSettings;
import org.jetbrains.annotations.Nullable;

public class PyCheckiOLanguageSettings extends PyLanguageSettings {
  @Nullable
  @Override
  public String validate(@Nullable Course course) {
    return PyCheckiOSdkValidator.validateSdk(getSettings().getSdk());
  }
}
