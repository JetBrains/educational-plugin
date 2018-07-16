package com.jetbrains.edu.learning.checkio.model;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CheckiOMissionList {
  @SerializedName("objects")
  private List<CheckiOMission> missions;

  private CheckiOMissionList() {
    missions = new ArrayList<>();
  }

  @NotNull
  public List<CheckiOStation> groupByStation() {
    Map<CheckiOStation, List<CheckiOMission>> missionsByStation = missions.stream()
      .collect(Collectors.groupingBy((mission) -> new CheckiOStation(mission.getStationId(), mission.getStationName())));

    return missionsByStation.entrySet().stream()
      .map((e) -> {
        CheckiOStation station = e.getKey();
        List<CheckiOMission> missions = e.getValue();

        station.addMissions(missions);
        return station;
      })
      .sorted(Comparator.comparing(CheckiOStation::getId))
      .collect(Collectors.toList());
  }
}
