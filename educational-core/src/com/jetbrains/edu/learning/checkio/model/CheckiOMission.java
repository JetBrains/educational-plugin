package com.jetbrains.edu.learning.checkio.model;

public class CheckiOMission {
  private int id;
  private boolean isSolved;
  private boolean isPublished;
  private int stationId;
  private String stationName;
  private boolean isPublishable;
  private boolean isPublicationRequired;
  private String slug;
  private String title;
  private String description;
  private String code;
  private String initialCode;

  public int getId() {
    return id;
  }

  public boolean isSolved() {
    return isSolved;
  }

  public boolean isPublished() {
    return isPublished;
  }

  public boolean isPublishable() {
    return isPublishable;
  }

  public boolean isPublicationRequired() {
    return isPublicationRequired;
  }

  public String getSlug() {
    return slug;
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

  public String getInitialCode() {
    return initialCode;
  }

  public int getStationId() {
    return stationId;
  }

  public String getStationName() {
    return stationName;
  }
}
