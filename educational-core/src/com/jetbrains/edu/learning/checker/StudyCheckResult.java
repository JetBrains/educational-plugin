package com.jetbrains.edu.learning.checker;

import com.jetbrains.edu.learning.courseFormat.StudyStatus;

public class StudyCheckResult {
  public static final StudyCheckResult USE_LOCAL_CHECK = new StudyCheckResult(StudyStatus.Unchecked, "Always use local check");
  private StudyStatus myStatus;
  private String myMessage;

  public StudyCheckResult(StudyStatus status, String message) {
    myStatus = status;
    myMessage = message;
  }

  public StudyStatus getStatus() {
    return myStatus;
  }

  public String getMessage() {
    return myMessage;
  }
}
