package com.jetbrains.edu.learning.checker;

import com.jetbrains.edu.learning.courseFormat.StudyStatus;

public class CheckResult {
  public static final CheckResult USE_LOCAL_CHECK = new CheckResult(StudyStatus.Unchecked, "Always use local check");
  private StudyStatus myStatus;
  private String myMessage;

  public CheckResult(StudyStatus status, String message) {
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
