package icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public class EducationalCoreIcons {
  private static Icon load(String path) {
    return IconLoader.getIcon(path, EducationalCoreIcons.class);
  }

  public static final Icon JavaLogo = load("/icons/com/jetbrains/edu/learning/JavaLogo.png"); // 16x16
  public static final Icon ScalaLogo = load("/icons/com/jetbrains/edu/learning/ScalaLogo.png"); // 16x16

  public static final Icon ShowHint = load("/icons/com/jetbrains/edu/learning/showHint.png"); // 16x16
  public static final Icon WatchInput = load("/icons/com/jetbrains/edu/learning/WatchInput.png"); // 24x24

  public static final Icon Stepik = load("/icons/com/jetbrains/edu/learning/Stepik.png"); // 16x16
  public static final Icon StepikOff = load("/icons/com/jetbrains/edu/learning/StepikOff.png"); // 16x16
  public static final Icon StepikRefresh = load("/icons/com/jetbrains/edu/learning/StepikRefresh.png"); // 16x16

  public static final Icon Student = load("/icons/com/jetbrains/edu/student.png"); // 180x180
  public static final Icon StudentHover = load("/icons/com/jetbrains/edu/studentHover.png"); // 180x180
  public static final Icon Teacher = load("/icons/com/jetbrains/edu/teacher.png"); // 180x180
  public static final Icon TeacherHover = load("/icons/com/jetbrains/edu/teacherHover.png"); // 180x180

  public static final Icon Task = load("/icons/com/jetbrains/edu/eduTaskDefault.png"); // 16x16
  public static final Icon TaskSolved = load("/icons/com/jetbrains/edu/eduTaskDone.png"); // 16x16
  public static final Icon CheckTask = load("/icons/com/jetbrains/edu/eduCheckTask.png"); // 16x16
  public static final Icon TaskFailed = load("/icons/com/jetbrains/edu/eduTaskFailed.png"); // 16x16
  public static final Icon IdeTask = load("/icons/com/jetbrains/edu/eduTaskIdeDefault.png"); // 16x16
  public static final Icon IdeTaskSolved = load("/icons/com/jetbrains/edu/eduTaskIdeDone.png"); // 16x16
  public static final Icon Lesson = load("/icons/com/jetbrains/edu/eduLessonDefault.png"); // 16x16
  public static final Icon LessonSolved = load("/icons/com/jetbrains/edu/eduLessonDone.png"); // 16x16
  public static final Icon Section = load("/icons/com/jetbrains/edu/eduSectionDefault.png"); // 16x16
  public static final Icon SectionSolved = load("/icons/com/jetbrains/edu/eduSectionDone.png"); // 16x16

  public static final Icon CourseAction = load("/icons/com/jetbrains/edu/eduCourseAction.png"); // 16x16
  public static final Icon CourseTree = load("/icons/com/jetbrains/edu/eduCourseTree.png"); // 16x16
  public static final Icon CourseToolWindow = load("/icons/com/jetbrains/edu/eduCourseTask181.png"); // 13x13
}
