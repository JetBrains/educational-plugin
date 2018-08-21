package com.jetbrains.edu.learning.checkio;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.progress.ProgressManager;
import com.jetbrains.edu.learning.checkio.api.exceptions.ApiException;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOApiConnector;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation;
import com.jetbrains.edu.learning.checkio.exceptions.CheckiOLoginRequiredException;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CheckiOCourseContentGenerator {
  private static final String DEFAULT_TASK_FILE_NAME = "mission";

  private final LanguageFileType myFileType;
  private final CheckiOApiConnector myApiConnector;

  public CheckiOCourseContentGenerator(@NotNull LanguageFileType fileType, @NotNull CheckiOApiConnector apiConnector) {
    myFileType = fileType;
    myApiConnector = apiConnector;
  }

  public List<CheckiOStation> getStationsFromServer()
    throws ApiException, CheckiOLoginRequiredException {
    return generateStationsFromMissions(myApiConnector.getMissionList());
  }

  public List<CheckiOStation> getStationsFromServerUnderProgress() throws Exception {
    return ProgressManager.getInstance().runProcessWithProgressSynchronously(
      () -> getStationsFromServer(),
      "Getting Course from Server",
      false,
      null
    );
  }

  @NotNull
  private List<CheckiOStation> generateStationsFromMissions(@NotNull List<CheckiOMission> missions) {
    missions.forEach(this::generateTaskFile);

    final Multimap<CheckiOStation, CheckiOMission> stationsMap  = TreeMultimap.create(
      Comparator.comparing(CheckiOStation::getId),
      Comparator.comparing(CheckiOMission::getId)
    );


    missions.forEach(mission -> stationsMap.put(mission.getStation(), mission));

    stationsMap.forEach(((station, mission) -> {
      station.addMission(mission);
      mission.setStation(station);
    }));

    return new ArrayList<>(stationsMap.keySet());
  }

  private void generateTaskFile(@NotNull CheckiOMission mission) {
    final TaskFile taskFile = new TaskFile();
    taskFile.name = DEFAULT_TASK_FILE_NAME + "." + myFileType.getDefaultExtension();
    setTaskFileText(taskFile, mission.getCode());
    taskFile.setHighlightErrors(true);
    mission.addTaskFile(taskFile);
  }

  private static void setTaskFileText(@NotNull TaskFile taskFile, @NotNull String text) {
    taskFile.text = text.replaceAll("\r\n", "\n");
  }
}
