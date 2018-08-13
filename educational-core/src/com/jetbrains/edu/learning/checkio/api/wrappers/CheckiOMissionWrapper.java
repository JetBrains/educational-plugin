package com.jetbrains.edu.learning.checkio.api.wrappers;

import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat;

@SuppressWarnings("unused")
public class CheckiOMissionWrapper implements ResponseWrapper<CheckiOMission> {
  private int id;
  private boolean isSolved;
  private int stationId;
  private String stationName;
  private String title;
  private String description;
  private String code;
  private long secondsPast;

  @Override
  public CheckiOMission unwrap() {
    CheckiOMission mission = new CheckiOMission();

    CheckiOStation station = new CheckiOStation();
    station.setId(stationId);
    station.setName(stationName);

    mission.setStation(station);
    mission.setStepId(id);
    mission.setName(title);
    mission.setDescriptionFormat(DescriptionFormat.HTML);
    mission.setDescriptionText(description);
    mission.setStatus(isSolved ? CheckStatus.Solved : CheckStatus.Unchecked);
    mission.setCode(code);
    mission.setSecondsFromLastChangeOnServer(secondsPast);

    return mission;
  }
}
