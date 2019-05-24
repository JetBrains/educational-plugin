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
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.ui.CCCreateCourseArchiveDialog;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.EduCourse;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.statistics.EduUsagesCollector;
import org.jetbrains.annotations.NotNull;

import java.io.File;

import static com.jetbrains.edu.coursecreator.CCUtils.askToWrapTopLevelLessons;
import static com.jetbrains.edu.learning.EduUtils.addMnemonic;

@SuppressWarnings("ComponentNotRegistered") // educational-core.xml
public class CCCreateCourseArchive extends DumbAwareAction {

  public static final String GENERATE_COURSE_ARCHIVE = "Generate Course Archive";
  public static final String LAST_ARCHIVE_LOCATION = "Edu.CourseCreator.LastArchiveLocation";

  private String myZipName;
  private String myLocationDir;

  public void setZipName(String zipName) {
    myZipName = zipName;
  }

  public void setLocationDir(String locationDir) {
    myLocationDir = locationDir;
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
    boolean isSuccessful = createCourseArchive(project, myZipName, myLocationDir, true);
    if (isSuccessful) {
      PropertiesComponent.getInstance(project).setValue(LAST_ARCHIVE_LOCATION, myLocationDir);
      EduUsagesCollector.createdCourseArchive();
    }
    else {
      Messages.showErrorDialog("Can not create archive for current course", "Failed to Create Course Archive");
    }
  }

  /**
   * @return true if course archive was created successfully, false otherwise
   */
  public static boolean createCourseArchive(@NotNull Project project, String zipName, String locationDir, boolean showMessage) {
    VirtualFile jsonFolder = CCUtils.generateFolder(project, zipName);
    if (jsonFolder == null) {
      return false;
    }
    FileDocumentManager.getInstance().saveAllDocuments();
    final File zipFile = new File(locationDir, zipName + ".zip");
    return ApplicationManager.getApplication()
      .runWriteAction(new CourseArchiveCreator(project, jsonFolder, zipFile, showMessage));
  }
}
