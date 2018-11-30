package com.jetbrains.edu.coursecreator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.jetbrains.edu.coursecreator.configuration.YamlFormatSynchronizer;
import com.jetbrains.edu.coursecreator.handlers.CCVirtualFileListener;
import com.jetbrains.edu.learning.CourseSetListener;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.statistics.EduUsagesCollector;
import org.jetbrains.annotations.NotNull;

public class CCProjectComponent extends AbstractProjectComponent {
  private static final Logger LOG = Logger.getInstance(CCProjectComponent.class);

  private CCVirtualFileListener myTaskFileLifeListener;
  private final Project myProject;

  protected CCProjectComponent(Project project) {
    super(project);
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
    if (StudyTaskManager.getInstance(myProject).getCourse() != null) {
      initCCProject();
    } else {
      myProject.getMessageBus().connect().subscribe(StudyTaskManager.COURSE_SET, new CourseSetListener() {
        @Override
        public void courseSet(@NotNull Course course) {
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
      YamlFormatSynchronizer.saveAll(myProject);
    }
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
