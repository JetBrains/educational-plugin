package com.jetbrains.edu.python.learning.checkio;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.checkio.api.exceptions.ApiException;
import com.jetbrains.edu.learning.checkio.api.exceptions.NetworkException;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.checkio.exceptions.LoginRequiredException;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.python.learning.PyCourseBuilder;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOApiConnector;
import com.jetbrains.edu.python.learning.checkio.messages.PyCheckiOErrorInformer;
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGenerator;
import org.jetbrains.annotations.NotNull;

public class PyCheckiOCourseProjectGenerator extends PyCourseProjectGenerator {
  private static final Logger LOG = Logger.getInstance(PyCheckiOCourseProjectGenerator.class);

  public PyCheckiOCourseProjectGenerator(@NotNull PyCourseBuilder builder,
                                         @NotNull Course course) {
    super(builder, course);
  }

  @Override
  protected void createAdditionalFiles(@NotNull Project project, @NotNull VirtualFile baseDir) {

  }

  @Override
  protected boolean beforeProjectGenerated() {
    try {
      final CheckiOCourse newCourse = PyCheckiOCourseContentGenerator
        .getInstance().generateCourseFromMissions(PyCheckiOApiConnector.getInstance().getMissionList());

      newCourse.setCourseOwner(PyCheckiOAccountHolder.getInstance().getAccount().getUserInfo());
      myCourse = newCourse;
      return true;
    }
    catch (LoginRequiredException e) {
      LOG.warn(e);
      PyCheckiOErrorInformer.getInstance().showLoginRequiredMessage("Failed to join the course");
      return false;
    }
    catch (NetworkException e) {
      LOG.warn(e);
      int result = PyCheckiOErrorInformer.getInstance().showNetworkErrorMessage("Failed to join the course");
      if ( result == Messages.OK) {
        return beforeProjectGenerated();
      }
      return false;
    }
    catch (ApiException e) {
      LOG.warn(e);
      PyCheckiOErrorInformer.getInstance().showErrorDialog("Something went wrong. Course cannot be generated.", "Failed to join the course");
      return false;
    }
  }
}
