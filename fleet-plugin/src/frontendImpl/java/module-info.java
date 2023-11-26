import com.jetbrains.edu.fleet.frontend.EduFrontendPlugin;

module com.jetbrains.edu.fleet.frontend {
  requires kotlin.stdlib;
  requires kotlinx.coroutines.core;
  requires fleet.kernel;
  requires fleet.rhizomedb;
  requires fleet.frontend.ui;
  requires com.jetbrains.edu.fleet.common;
  requires com.jetbrains.edu.format;
  requires fleet.dock.connectors;

  exports com.jetbrains.edu.fleet.frontend;
  exports com.jetbrains.edu.fleet.frontend.actions;
  exports com.jetbrains.edu.fleet.frontend.ui;

  provides fleet.kernel.plugins.Plugin with EduFrontendPlugin;
}
