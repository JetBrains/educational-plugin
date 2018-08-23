package com.jetbrains.edu.learning.courseFormat.remote;

public class StepikRemoteInfo implements RemoteInfo {
  boolean isPublic;

  public boolean isPublic() {
    return isPublic;
  }

  public void setPublic(boolean isPublic) {
    this.isPublic = isPublic;
  }
}
