package com.jetbrains.edu.learning.checkio.courseFormat;

import com.jetbrains.edu.learning.checkio.model.CheckiOUserInfo;
import com.jetbrains.edu.learning.courseFormat.Course;
import jdk.nashorn.internal.objects.annotations.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class CheckiOCourse extends Course {
  // used for deserialization
  public CheckiOCourse() {}

  public CheckiOCourse(@NotNull String name, @NotNull String description, @NotNull String languageID) {
    setName(name);
    setDescription(description); // TODO
    setLanguage(languageID);
  }

  public void addStation(@NotNull CheckiOStation station) {
    addLesson(station);
  }

  @NotNull
  public List<CheckiOStation> getStations() {
    return items.stream().filter(CheckiOStation.class::isInstance).map(CheckiOStation.class::cast).collect(Collectors.toList());
  }

  @Override
  public String toString() {
    return "stations=[" + getStations().stream().map(CheckiOStation::toString).collect(Collectors.joining("\n")) + "]";
  }
}
