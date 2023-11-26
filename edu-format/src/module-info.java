open module com.jetbrains.edu.format {
  requires com.fasterxml.jackson.core;
  requires com.fasterxml.jackson.databind;
  requires com.fasterxml.jackson.annotation;
  requires com.fasterxml.jackson.dataformat.yaml;
  requires com.fasterxml.jackson.datatype.jsr310;
  requires com.fasterxml.jackson.kotlin;
  requires okhttp3;
  requires okhttp3.logging;
  requires retrofit2;
  requires retrofit2.converter.jackson;
  requires java.logging;
  requires kotlin.stdlib;
  requires org.jetbrains.annotations;

  exports com.jetbrains.edu.learning.courseFormat;
  exports com.jetbrains.edu.learning.courseFormat.tasks;
  exports com.jetbrains.edu.learning.marketplace.api;
  exports com.jetbrains.edu.learning;
  exports com.jetbrains.edu.learning.json;
  exports com.jetbrains.edu.learning.json.mixins;
  exports com.jetbrains.edu.learning.yaml;
  exports com.jetbrains.edu.learning.yaml.errorHandling;
  exports com.jetbrains.edu.learning.yaml.format;
}
