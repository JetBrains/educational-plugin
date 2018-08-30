package com.jetbrains.edu.learning.stepik.courseFormat;

import com.jetbrains.edu.learning.courseFormat.Section;
import com.jetbrains.edu.learning.courseFormat.remote.RemoteInfo;
import com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo.StepikSectionRemoteInfo;
import org.jetbrains.annotations.NotNull;

public class StepikSection extends Section {

  public StepikSection() {
    setRemoteInfo(new StepikSectionRemoteInfo());
  }

  @NotNull
  public StepikSectionRemoteInfo getStepikRemoteInfo() {
    final RemoteInfo info = super.getRemoteInfo();
    assert info instanceof StepikSectionRemoteInfo;
    return (StepikSectionRemoteInfo)info;
  }
}
