package com.jetbrains.edu.learning.checkio.model;

@SuppressWarnings("unused")
public class CheckiOMissionWrapper {
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
}
