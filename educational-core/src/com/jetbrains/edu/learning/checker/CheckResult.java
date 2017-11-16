package com.jetbrains.edu.learning.checker;

import com.jetbrains.edu.learning.courseFormat.CheckStatus;

public class CheckResult {
  public static final CheckResult USE_LOCAL_CHECK = new CheckResult(CheckStatus.Unchecked, "Always use local check");
  private CheckStatus myStatus;
  private String myMessage;

  public CheckResult(CheckStatus status, String message) {
    myStatus = status;
    myMessage = message;
  }

  public CheckStatus getStatus() {
    return myStatus;
  }

  public String getMessage() {
    return myMessage;
  }
}
