package icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public class EducationalCoreIcons {
  private static Icon load(String path) {
    return IconLoader.getIcon(path, EducationalCoreIcons.class);
  }

  public static final Icon JavaLogo = load("/icons/com/jetbrains/edu/learning/JavaLogo.svg"); // 16x16
  public static final Icon KotlinLogo = load("/icons/com/jetbrains/edu/learning/KotlinLogo.svg"); // 16x16
  public static final Icon ScalaLogo = load("/icons/com/jetbrains/edu/learning/ScalaLogo.svg"); // 16x16
  public static final Icon AndroidLogo = load("/icons/com/jetbrains/edu/learning/AndroidLogo.svg"); // 16x16
  public static final Icon PythonLogo = load("/icons/com/jetbrains/edu/learning/PythonLogo.svg"); // 16x16
  public static final Icon JsLogo = load("/icons/com/jetbrains/edu/learning/JavaScriptLogo.svg"); // 16x16
  public static final Icon RustLogo = load("/icons/com/jetbrains/edu/learning/RustLogo.svg"); // 16x16
  public static final Icon CppLogo = load("/icons/com/jetbrains/edu/learning/CAndC++Logo.svg"); // 16x16
  public static final Icon GoLogo = load("/icons/com/jetbrains/edu/learning/GoLogo.svg"); // 16x16

  public static final Icon WatchInput = load("/icons/com/jetbrains/edu/learning/WatchInput.png"); // 24x24

  public static final Icon Stepik = load("/icons/com/jetbrains/edu/learning/Stepik.png"); // 16x16
  public static final Icon StepikRefresh = load("/icons/com/jetbrains/edu/learning/StepikRefresh.png"); // 16x16
  public static final Icon StepikCourseTab = load("/icons/com/jetbrains/edu/learning/StepikBlack.svg"); // 16x16

  public static final Icon CheckiO = load("/icons/com/jetbrains/edu/learning/PyCheckiO.svg");
  public static final Icon JSCheckiO = load("/icons/com/jetbrains/edu/learning/JSCheckiO.svg");

  public static final Icon JB_ACADEMY_ENABLED = load("/icons/com/jetbrains/edu/learning/JB_academy_enabled.svg");
  public static final Icon JB_ACADEMY_DISABLED = load("/icons/com/jetbrains/edu/learning/JB_academy_disabled.svg");
  public static final Icon JB_ACADEMY_TAB = load("/icons/com/jetbrains/edu/learning/JB_academy_course_tab.svg");

  public static final Icon Codeforces = load("/icons/com/jetbrains/edu/learning/codeforces.svg");
  public static final Icon CodeforcesGrayed = load("/icons/com/jetbrains/edu/learning/codeforcesGrayed.svg");

  public static final Icon Coursera = load("/icons/com/jetbrains/edu/learning/coursera.svg");

  public static final Icon Student = load("/icons/com/jetbrains/edu/Learner.svg"); // 180x180
  public static final Icon StudentHover = load("/icons/com/jetbrains/edu/LearnerActive.svg"); // 180x180
  public static final Icon Teacher = load("/icons/com/jetbrains/edu/Teacher.svg"); // 180x180
  public static final Icon TeacherHover = load("/icons/com/jetbrains/edu/TeacherActive.svg"); // 180x180

  public static final Icon Task = load("/icons/com/jetbrains/edu/eduTaskDefault.png"); // 16x16
  public static final Icon TaskSolved = load("/icons/com/jetbrains/edu/eduTaskDone.png"); // 16x16
  public static final Icon TaskSolvedNoFrame = load("/icons/com/jetbrains/edu/eduTaskDoneNoFrame.png"); //11x11
  public static final Icon TaskFailed = load("/icons/com/jetbrains/edu/eduTaskFailed.png"); // 16x16
  public static final Icon TaskFailedNoFrame = load("/icons/com/jetbrains/edu/eduTaskFailedNoFrame.png"); // 11x11
  public static final Icon IdeTask = load("/icons/com/jetbrains/edu/eduTaskIdeDefault.png"); // 16x16
  public static final Icon IdeTaskSolved = load("/icons/com/jetbrains/edu/eduTaskIdeDone.png"); // 16x16
  public static final Icon Lesson = load("/icons/com/jetbrains/edu/eduLessonDefault.png"); // 16x16
  public static final Icon LessonSolved = load("/icons/com/jetbrains/edu/eduLessonDone.png"); // 16x16
  public static final Icon Section = load("/icons/com/jetbrains/edu/eduSectionDefault.png"); // 16x16
  public static final Icon SectionSolved = load("/icons/com/jetbrains/edu/eduSectionDone.png"); // 16x16

  public static final Icon CourseAction = load("/icons/com/jetbrains/edu/eduCourseAction.png"); // 16x16
  public static final Icon CourseTree = load("/icons/com/jetbrains/edu/eduCourseTree.png"); // 16x16
  public static final Icon CourseToolWindow = load("/icons/com/jetbrains/edu/eduCourseTask.svg"); // 13x13

  public static final Icon ResultCorrect = load("/icons/com/jetbrains/edu/learning/resultCorrect.svg"); // 16x16
  public static final Icon ResetTask = load("/icons/com/jetbrains/edu/learning/resetTask.svg"); // 16x16
  public static final Icon CommentTask = load("/icons/com/jetbrains/edu/learning/commentTask.svg"); // 16x16

  public static final Icon User = load("/icons/com/jetbrains/edu/usersNumber.svg"); // 12x12
}
