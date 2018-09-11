package com.jetbrains.edu.learning.checker;

import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import org.jetbrains.annotations.Nullable;

public class CheckResult {
  public static final CheckResult USE_LOCAL_CHECK = new CheckResult(CheckStatus.Unchecked, "Always use local check");
  public static final CheckResult FAILED_TO_CHECK = new CheckResult(CheckStatus.Unchecked, CheckUtils.FAILED_TO_CHECK_MESSAGE);
  public static final CheckResult LOGIN_NEEDED = new CheckResult(CheckStatus.Unchecked, CheckUtils.LOGIN_NEEDED_MESSAGE);
  public static final CheckResult CONNECTION_FAILED = new CheckResult(CheckStatus.Unchecked, "Connection failed");

  private CheckStatus myStatus;
  private String myMessage;
  @Nullable private String myDetails;

  public CheckResult(CheckStatus status, String message) {
    this(status, message, null);
  }

  public CheckResult(CheckStatus status, String message, @Nullable String details) {
    myStatus = status;
    myMessage = message;
    myDetails = details;
  }

  public CheckStatus getStatus() {
    return myStatus;
  }

  public String getMessage() {
    return myMessage;
  }

  @Nullable
  public String getDetails() {
    return myDetails;
  }
}
