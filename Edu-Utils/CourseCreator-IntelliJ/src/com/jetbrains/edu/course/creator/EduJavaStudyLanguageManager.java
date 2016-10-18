package com.jetbrains.edu.course.creator;

import com.jetbrains.edu.learning.StudyLanguageManager;
import org.jetbrains.annotations.NotNull;

public class EduJavaStudyLanguageManager implements StudyLanguageManager {
  @NotNull
  @Override
  public String getTestFileName() {
    return "Test.java";
  }

  @NotNull
  @Override
  public String getTestHelperFileName() {
    return "";
  }

  @NotNull
  @Override
  public String getUserTester() {
    return "";
  }
}
