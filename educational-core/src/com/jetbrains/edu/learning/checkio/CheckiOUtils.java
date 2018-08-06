package com.jetbrains.edu.learning.checkio;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOConnector;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation;
import com.jetbrains.edu.learning.checkio.model.CheckiOMissionListWrapper;
import com.jetbrains.edu.learning.checkio.model.CheckiOMissionWrapper;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class CheckiOUtils {
  private static final Logger LOG = Logger.getInstance(CheckiOUtils.class);

  private static boolean myNeedSkipStations = false;

  public static void setTaskFileText(@NotNull TaskFile taskFile, @NotNull String code) {
    taskFile.text = code.replaceAll("\r\n", "\n");
  }

  // Used for course updating testing purposes
  @Nullable
  public static CheckiOCourse getCourseFromServerSkipped() {
    myNeedSkipStations = true;
    return getCourseFromServer();
  }

  @Nullable
  public static CheckiOCourse getCourseFromServer() {
    return generateCourseFromWrapper(CheckiOConnector.getMissionList());
  }

  @Nullable
  private static CheckiOCourse generateCourseFromWrapper(@Nullable CheckiOMissionListWrapper missionsListWrapper) {
    if (missionsListWrapper == null) {
      LOG.warn("Mission list is null");
      return null;
    }

    final List<CheckiOMission> missionsList = generateMissionsFromWrapper(missionsListWrapper);
    final List<CheckiOStation> stations = generateStationsFromMissions(missionsList);
    return generateCourseFromStations(stations);
  }

  @NotNull
  private static List<CheckiOMission> generateMissionsFromWrapper(@NotNull CheckiOMissionListWrapper missionListWrapper) {
    return missionListWrapper.getMissions().stream().map(CheckiOUtils::generateMissionFromWrapper)
      .collect(Collectors.toList());
  }

  @NotNull
  private static CheckiOMission generateMissionFromWrapper(@NotNull CheckiOMissionWrapper missionWrapper) {
    CheckiOMission mission = new CheckiOMission();

    CheckiOStation station = new CheckiOStation();
    station.setId(missionWrapper.getStationId());
    station.setName(missionWrapper.getStationName());

    mission.setStation(station);
    mission.setStepId(missionWrapper.getId());
    mission.setName(missionWrapper.getTitle());
    mission.setDescriptionFormat(DescriptionFormat.HTML);
    mission.setDescriptionText(missionWrapper.getDescription());
    mission.setStatus(missionWrapper.isSolved() ? CheckStatus.Solved : CheckStatus.Unchecked);
    mission.setCode(missionWrapper.getCode());
    mission.setSecondsFromLastChangeOnServer(missionWrapper.getSecondsFromLastChange());

    final TaskFile taskFile = new TaskFile();
    taskFile.name = mission.getName() + ".py";
    setTaskFileText(taskFile, missionWrapper.getCode());
    taskFile.setHighlightErrors(true);
    mission.addTaskFile(taskFile);

    return mission;
  }

  @NotNull
  private static List<CheckiOStation> generateStationsFromMissions(@NotNull List<CheckiOMission> missions) {
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
  private static CheckiOCourse generateCourseFromStations(@NotNull List<CheckiOStation> stationsList) {
    final CheckiOCourse course = new CheckiOCourse();

    stationsList.stream().skip(myNeedSkipStations ? 1 : 0).forEach(station -> {
      course.addStation(station);
      station.setCourse(course);
    });

    return course;
  }
}
