package com.jetbrains.edu.learning.courseFormat.tasks;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ChoiceTask extends Task {

  @SuppressWarnings("unused") //used for deserialization
  public ChoiceTask() {}

  private List<String> myChoiceVariants = new ArrayList<>();
  private boolean myIsMultipleChoice;
  private List<Integer> mySelectedVariants = new ArrayList<>();

  public List<Integer> getSelectedVariants() {
    return mySelectedVariants;
  }

  public void setSelectedVariants(List<Integer> selectedVariants) {
    mySelectedVariants = selectedVariants;
  }

  public boolean isMultipleChoice() {
    return myIsMultipleChoice;
  }

  public void setMultipleChoice(boolean multipleChoice) {
    myIsMultipleChoice = multipleChoice;
  }

  public List<String> getChoiceVariants() {
    return myChoiceVariants;
  }

  public void setChoiceVariants(List<String> choiceVariants) {
    myChoiceVariants = choiceVariants;
  }

  public ChoiceTask(@NotNull final String name) {
    super(name);
  }

  @Override
  public String getTaskType() {
    return "choice";
  }
}
