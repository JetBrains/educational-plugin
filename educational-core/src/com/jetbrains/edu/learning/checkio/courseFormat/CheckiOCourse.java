package com.jetbrains.edu.learning.checkio.courseFormat;

import com.jetbrains.edu.learning.courseFormat.Course;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

import static com.jetbrains.edu.learning.checkio.utils.CheckiONames.CHECKIO_TYPE;

public class CheckiOCourse extends Course {
  private static final String COURSE_DESCRIPTION =
    "CheckiO is a game where you code in Python or JavaScript.\n" +
    "Progress in the game by solving code challenges and compete for the most elegant and creative solutions.\n" +
    "<a href=\"http://www.checkio.org/\">http://www.checkio.org/</a>";

  // used for deserialization
  public CheckiOCourse() {}

  public CheckiOCourse(@NotNull String name, @NotNull String languageID) {
    setName(name);
    setDescription(COURSE_DESCRIPTION);
    setLanguage(languageID);
    setCourseType(CHECKIO_TYPE);
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
