package com.jetbrains.edu.learning.checkio.notifications.infos;

import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation;
import com.jetbrains.edu.learning.checkio.notifications.CheckiONotification;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class CheckiOStationsUnlockedNotification extends CheckiONotification.Info {
  public CheckiOStationsUnlockedNotification(@NotNull List<CheckiOStation> newStations) {
    super(
      "New stations unlocked",
      "",
      newStations.stream().map(CheckiOStation::getName).collect(Collectors.joining("\n"))
      ,
      null
    );
  }
}
