package com.jetbrains.edu.coursecreator.actions;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.ui.CCCreateCourseArchiveDialog;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.EduCourse;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

import static com.intellij.openapi.ui.Messages.showErrorDialog;
import static com.jetbrains.edu.coursecreator.CCUtils.askToWrapTopLevelLessons;
import static com.jetbrains.edu.learning.EduUtils.addMnemonic;

@SuppressWarnings("ComponentNotRegistered") // educational-core.xml
public class CCCreateCourseArchive extends DumbAwareAction {

  public static final String GENERATE_COURSE_ARCHIVE = "Generate Course Archive";
  public static final String LAST_ARCHIVE_LOCATION = "Edu.CourseCreator.LastArchiveLocation";
  public static final String AUTHOR_NAME = "Edu.Author.Name";

  private String myZipName;
  private String myLocationDir;
  private String myAuthorName;

  public void setZipName(String zipName) {
    myZipName = zipName;
  }

  public void setLocationDir(String locationDir) {
    myLocationDir = locationDir;
  }

  public void setAuthorName(String authorName) {
    myAuthorName = authorName;
  }

  public CCCreateCourseArchive() {
    super(addMnemonic(GENERATE_COURSE_ARCHIVE), GENERATE_COURSE_ARCHIVE, null);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    Project project = e.getProject();
    presentation.setEnabledAndVisible(project != null && CCUtils.isCourseCreator(project));
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) return;
    Course course = StudyTaskManager.getInstance(project).getCourse();
    assert course != null;

    if (CourseExt.getHasSections(course) && CourseExt.getHasTopLevelLessons(course)) {
      if (!askToWrapTopLevelLessons(project, (EduCourse)course)) {
        return;
      }
    }

    CCCreateCourseArchiveDialog dlg = new CCCreateCourseArchiveDialog(project, this);
    dlg.show();
    if (dlg.getExitCode() != DialogWrapper.OK_EXIT_CODE) {
      return;
    }
    course.setAuthorsAsString(new String[]{myAuthorName});
    String errorMessage = createCourseArchive(project, myZipName, myLocationDir, true);
    if (errorMessage == null) {
      PropertiesComponent.getInstance(project).setValue(LAST_ARCHIVE_LOCATION, myLocationDir);
      PropertiesComponent.getInstance(project).setValue(AUTHOR_NAME, myAuthorName);
      EduCounterUsageCollector.createCourseArchive();
    }
    else {
      showErrorDialog(project, errorMessage, "Failed to Create Course Archive");
    }
  }

  /**
   * @return null if course archive was created successfully, non-empty error message otherwise
   */
  @Nullable
  public static String createCourseArchive(@NotNull Project project, String zipName, String locationDir, boolean showMessage) {
    String archiveName = zipName + ".zip";
    VirtualFile jsonFolder = CCUtils.generateFolder(project, zipName);
    if (jsonFolder == null) {
      return "Failed to generate " + archiveName;
    }
    FileDocumentManager.getInstance().saveAllDocuments();
    final File zipFile = new File(locationDir, archiveName);
    return ApplicationManager.getApplication()
      .runWriteAction(new CourseArchiveCreator(project, jsonFolder, zipFile, showMessage));
  }
}
