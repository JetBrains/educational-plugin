package com.jetbrains.edu.learning.checkio.courseFormat;

import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class CheckiOCourse extends Course {
  public CheckiOCourse() {
    setName("CheckiO");
    setDescription("CheckiO description"); // TODO
    setLanguage(EduNames.CHECKIO_PYTHON);
  }

  public void addStation(@NotNull CheckiOStation station) {
    addLesson(station);
  }

  @Nullable
  public CheckiOStation getStation(@NotNull String name) {
    final Lesson lesson = getLesson(name);
    return (lesson instanceof CheckiOStation ? (CheckiOStation) lesson : null);
  }

  @NotNull
  public List<CheckiOStation> getStations() {
    return items.stream().filter(CheckiOStation.class::isInstance).map(CheckiOStation.class::cast).collect(Collectors.toList());
  }
}
