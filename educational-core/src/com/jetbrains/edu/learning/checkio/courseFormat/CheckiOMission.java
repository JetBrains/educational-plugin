package com.jetbrains.edu.learning.checkio.courseFormat;

import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import org.jetbrains.annotations.NotNull;

public class CheckiOMission extends EduTask {
  @Transient private CheckiOStation myStation;

  public CheckiOMission() {  }

  @Transient
  @NotNull
  public CheckiOStation getStation() {
    return myStation;
  }

  @Transient
  public void setStation(@NotNull CheckiOStation station) {
    myStation = station;
  }
}
