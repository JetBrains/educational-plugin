package com.jetbrains.edu.python.learning.checkio;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOConnector;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation;
import com.jetbrains.edu.learning.checkio.model.CheckiOMissionListWrapper;
import com.jetbrains.edu.learning.checkio.model.CheckiOMissionWrapper;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.python.learning.PyCourseBuilder;
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PyCheckiOCourseProjectGenerator extends PyCourseProjectGenerator {
  private static final Logger LOG = Logger.getInstance(PyCheckiOCourseProjectGenerator.class);

  public PyCheckiOCourseProjectGenerator(@NotNull PyCourseBuilder builder,
                                         @NotNull Course course) {
    super(builder, course);
  }

  @Override
  protected void createAdditionalFiles(@NotNull Project project, @NotNull VirtualFile baseDir) {

  }

  @Override
  protected boolean beforeProjectGenerated() {
    final CheckiOMissionListWrapper missionListWrapper = CheckiOConnector.getMissionList();
    final CheckiOCourse newCourse = generateCourseFromMissions(missionListWrapper);

    if (newCourse == null) {
      LOG.warn("Error occurred generating course");
      return false;
    }

    myCourse = newCourse;
    return true;
  }

  @Nullable
  private static CheckiOCourse generateCourseFromMissions(@Nullable CheckiOMissionListWrapper missionsListWrapper) {
    if (missionsListWrapper == null) {
      LOG.warn("Mission list is null");
      return null;
    }

    final List<CheckiOMission> missionsList = generateMissionsFromWrapper(missionsListWrapper);
    final List<CheckiOStation> stations = generateStationsFromMissions(missionsList);
    return generateCourseFromStations(stations);
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

    final TaskFile taskFile = new TaskFile();
    taskFile.name = mission.getName() + ".py";
    taskFile.text = missionWrapper.getCode().replaceAll("\r\n", "\n");
    taskFile.setHighlightErrors(true);
    mission.addTaskFile(taskFile);

    return mission;
  }

  @NotNull
  private static List<CheckiOMission> generateMissionsFromWrapper(@NotNull CheckiOMissionListWrapper missionListWrapper) {
    return missionListWrapper.getMissions().stream().map(PyCheckiOCourseProjectGenerator::generateMissionFromWrapper)
      .collect(Collectors.toList());
  }

  @NotNull
  private static List<CheckiOStation> generateStationsFromMissions(@NotNull List<CheckiOMission> missions) {
    final Multimap<CheckiOStation, CheckiOMission> stationsMap = MultimapBuilder
      .treeKeys(Comparator.comparing(CheckiOStation::getId))
      .treeSetValues(Comparator.comparing(CheckiOMission::getId))
      .build();

    missions.forEach(mission -> stationsMap.put(mission.getStation(), mission));
    stationsMap.forEach(CheckiOStation::addMission);

    return new ArrayList<>(stationsMap.keySet());
  }

  @NotNull
  private static CheckiOCourse generateCourseFromStations(@NotNull List<CheckiOStation> stationsList) {
    final CheckiOCourse course = new CheckiOCourse();
    stationsList.forEach(course::addStation);
    return course;
  }
}
