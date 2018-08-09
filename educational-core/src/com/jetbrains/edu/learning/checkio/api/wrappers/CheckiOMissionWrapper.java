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

  public int getId() {
    return id;
  }

  public boolean isSolved() {
    return isSolved;
  }

  public int getStationId() {
    return stationId;
  }

  public String getStationName() {
    return stationName;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public String getCode() {
    return code;
  }

  public long getSecondsFromLastChange() {
    return secondsPast;
  }

  @Override
  public CheckiOMission unwrap() {
    CheckiOMission mission = new CheckiOMission();

    CheckiOStation station = new CheckiOStation();
    station.setId(getStationId());
    station.setName(getStationName());

    mission.setStation(station);
    mission.setStepId(getId());
    mission.setName(getTitle());
    mission.setDescriptionFormat(DescriptionFormat.HTML);
    mission.setDescriptionText(getDescription());
    mission.setStatus(isSolved() ? CheckStatus.Solved : CheckStatus.Unchecked);
    mission.setCode(getCode());
    mission.setSecondsFromLastChangeOnServer(getSecondsFromLastChange());

    //final TaskFile taskFile = new TaskFile();
    //taskFile.name = mission.getName() + getTaskFileExtension();
    //taskFile.text = getCode().replaceAll("\r\n", "\n");
    //taskFile.setHighlightErrors(true);
    //mission.addTaskFile(taskFile);

    return mission;
  }
}
