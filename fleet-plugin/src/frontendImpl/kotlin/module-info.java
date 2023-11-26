module fleet.plugins.edu.frontend {
  requires kotlinx.coroutines.core;
  requires fleet.common;
  requires fleet.dock.api;
  requires fleet.frontend;
  requires fleet.frontend.ui;
  requires fleet.kernel;
  requires fleet.noria.cells;
  requires fleet.noria.ui;
  requires fleet.plugins.edu.common;
  requires fleet.protocol;
  requires fleet.rhizomedb;
  requires fleet.rpc;
  requires fleet.util.core;
  requires fleet.dock.connectors;
  requires edu.format;

  exports fleet.edu.frontend;
  exports fleet.edu.frontend.ui;
  exports fleet.edu.frontend.actions;
}