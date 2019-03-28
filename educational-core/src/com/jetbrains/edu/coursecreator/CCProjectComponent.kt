package com.jetbrains.edu.coursecreator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.jetbrains.edu.coursecreator.handlers.CCVirtualFileListener;
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer;
import com.jetbrains.edu.coursecreator.yaml.YamlLoader;
import com.jetbrains.edu.learning.CourseSetListener;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.statistics.EduUsagesCollector;
import org.jetbrains.annotations.NotNull;

import static com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.isEduYamlProject;
@SuppressWarnings("ComponentNotRegistered") // educational-core.xml
public class CCProjectComponent implements ProjectComponent {

  private CCVirtualFileListener myTaskFileLifeListener;
  private final Project myProject;

  protected CCProjectComponent(Project project) {
    myProject = project;
  }

  private void startTaskDescriptionFilesSynchronization() {
    Course course = StudyTaskManager.getInstance(myProject).getCourse();
    if (course == null) {
      return;
    }
    EditorFactory.getInstance().getEventMulticaster().addDocumentListener(new SynchronizeTaskDescription(myProject), myProject);
  }

  @NotNull
  public String getComponentName() {
    return "CCProjectComponent";
  }

  public void projectOpened() {
    // it is also false for newly created courses as config files isn't created yet.
    // it's ok as we don't need to load course from config
    if (isEduYamlProject(myProject)) {
      loadCourse();
    }

    if (StudyTaskManager.getInstance(myProject).getCourse() != null) {
      initCCProject();
    }
    else {
      myProject.getMessageBus().connect().subscribe(StudyTaskManager.COURSE_SET, new CourseSetListener() {
        @Override
        public void courseSet(@NotNull Course course) {
          // in case course was reset from StudyTaskManager
          if (isEduYamlProject(myProject)) {
            loadCourse();
          }
          initCCProject();
        }
      });
    }
  }

  private void initCCProject() {
    if (CCUtils.isCourseCreator(myProject)) {
      if (!ApplicationManager.getApplication().isUnitTestMode()) {
        registerListener();
      }

      EduUsagesCollector.projectTypeOpened(CCUtils.COURSE_MODE);
      startTaskDescriptionFilesSynchronization();
      YamlFormatSynchronizer.startSynchronization(myProject);
    }
  }

  private void loadCourse() {
    Course course = YamlLoader.loadCourseRecursively(myProject);
    StudyTaskManager.getInstance(myProject).setCourse(course);
  }

  public void registerListener() {
    if (myTaskFileLifeListener == null) {
      myTaskFileLifeListener = new CCVirtualFileListener(myProject);
      VirtualFileManager.getInstance().addVirtualFileListener(myTaskFileLifeListener);
    }
  }

  public void projectClosed() {
    if (myTaskFileLifeListener != null) {
      VirtualFileManager.getInstance().removeVirtualFileListener(myTaskFileLifeListener);
    }
  }
}
