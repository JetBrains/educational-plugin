package com.jetbrains.edu.learning.checkio;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class CheckiOCourseContentGenerator {
  @NotNull
  public  CheckiOCourse generateCourseFromMissions(@NotNull List<CheckiOMission> missionsList) {
    missionsList.forEach(this::generateTaskFile);

    final List<CheckiOStation> stations = generateStationsFromMissions(missionsList);
    return generateCourseFromStations(stations);
  }

  private void generateTaskFile(@NotNull CheckiOMission mission) {
    final TaskFile taskFile = new TaskFile();
    taskFile.name = mission.getName() + getTaskFileExtension();
    setTaskFileText(taskFile, mission.getCode());
    taskFile.setHighlightErrors(true);
    mission.addTaskFile(taskFile);
  }

  protected abstract String getTaskFileExtension();

  @SuppressWarnings("MethodMayBeStatic")
  @NotNull
  private  List<CheckiOStation> generateStationsFromMissions(@NotNull List<CheckiOMission> missions) {
    final Multimap<CheckiOStation, CheckiOMission> stationsMap = MultimapBuilder
      .treeKeys(Comparator.comparing(CheckiOStation::getId))
      .treeSetValues(Comparator.comparing(CheckiOMission::getId))
      .build();

    missions.forEach(mission -> stationsMap.put(mission.getStation(), mission));

    stationsMap.forEach(((station, mission) -> {
      station.addMission(mission);
      mission.setStation(station);
    }));

    return new ArrayList<>(stationsMap.keySet());
  }

  @NotNull
  private  CheckiOCourse generateCourseFromStations(@NotNull List<CheckiOStation> stationsList) {
    final CheckiOCourse course = getNewCourseInstance();

    stationsList.forEach(station -> {
      course.addStation(station);
      station.setCourse(course);
    });

    return course;
  }

  protected abstract CheckiOCourse getNewCourseInstance();

  private static void setTaskFileText(@NotNull TaskFile taskFile, @NotNull String text) {
    taskFile.text = text.replaceAll("\r\n", "\n");
  }
}
