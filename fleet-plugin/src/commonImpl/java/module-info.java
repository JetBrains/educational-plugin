module com.jetbrains.edu.fleet.common {
  requires kotlin.stdlib;
  requires kotlinx.coroutines.core;
  requires fleet.common;
  requires fleet.kernel.plugins;
  requires fleet.rhizomedb;
  requires fleet.util.network;

  requires com.jetbrains.edu.format;
  requires retrofit2;
  requires com.fasterxml.jackson.databind;
  requires okhttp3;
  requires retrofit2.converter.jackson;

  exports com.jetbrains.edu.fleet.common;
  exports com.jetbrains.edu.fleet.common.marketplace;
  exports com.jetbrains.edu.fleet.common.generation;
  exports com.jetbrains.edu.fleet.common.yaml;
}
