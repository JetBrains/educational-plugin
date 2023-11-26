module fleet.plugins.edu.common {
  requires com.fasterxml.jackson.annotation;
  requires com.fasterxml.jackson.databind;
  requires converter.jackson;
  requires retrofit2;
  requires okhttp3;
  requires com.fasterxml.jackson.dataformat.yaml;

  requires fleet.common;
  requires fleet.protocol;
  requires fleet.rhizomedb;
  requires fleet.util.logging.api;
  requires fleet.util.network;
  requires kotlinx.coroutines.core;
  requires edu.format;

  exports fleet.edu.common;
  exports fleet.edu.common.generation;
  exports fleet.edu.common.marketplace;
  exports fleet.edu.common.yaml;

  opens fleet.edu.common.marketplace;
}