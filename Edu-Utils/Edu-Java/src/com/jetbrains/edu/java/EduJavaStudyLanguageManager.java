package com.jetbrains.edu.java;

import com.jetbrains.edu.learning.StudyLanguageManager;
import org.jetbrains.annotations.NotNull;

public class EduJavaStudyLanguageManager implements StudyLanguageManager {

  public static final String TEST_JAVA = "Test.java";

  @NotNull
  @Override
  public String getTestFileName() {
    return TEST_JAVA;
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
