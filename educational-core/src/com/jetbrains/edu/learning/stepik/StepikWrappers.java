package com.jetbrains.edu.learning.stepik;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.jetbrains.edu.learning.EduVersions;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class StepikWrappers {

  public static class Unit {
    public Integer id;
    public int section;
    public int lesson;
    public int position;
    public @SerializedName("update_date") Date updateDate;
    public List<Integer> assignments;

    public void setSection(int section) {
      this.section = section;
    }

    public void setPosition(int position) {
      this.position = position;
    }

    public int getPosition() {
      return position;
    }

    public void setLesson(int lesson) {
      this.lesson = lesson;
    }

    public int getSection() {
      return section;
    }

    public int getId() {
      return id;
    }

    public void setId(int id) {
      this.id = id;
    }

    public Date getUpdateDate() {
      return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
      this.updateDate = updateDate;
    }
  }

  public static class SolutionFile {
    public String name;
    public String text;

    public SolutionFile() {}

    public SolutionFile(String name, String text) {
      this.name = name;
      this.text = text;
    }
  }

  public static class Reply {
    boolean[] choices;
    public String score;
    public List<SolutionFile> solution;
    public String language;
    public String code;
    public String edu_task;
    public int version = EduVersions.JSON_FORMAT_VERSION;

    public Reply() {}

    public Reply(List<SolutionFile> files, String score, String serializedTask) {
      this.score = score;
      solution = files;
      edu_task = serializedTask;
    }
  }

  public static class Submission {
    public int attempt;
    public Reply reply;
    public String id;
    public String status;
    public String hint;

    public Submission() {}

    public Submission(String score, int attemptId, ArrayList<SolutionFile> files, String serializedTask) {
      reply = new Reply(files, score, serializedTask);
      this.attempt = attemptId;
    }
  }

  public static class TaskWrapper {
    @Expose Task task;

    public TaskWrapper(Task task) {
      this.task = task;
    }
  }
}
