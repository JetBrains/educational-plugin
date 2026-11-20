// Init script used by `startup.sh` to download all dependencies of the project without building it.
// Registers `airResolveDependencies` task in every project which resolves all its resolvable configurations,
// downloading all dependencies and IDEs into the Gradle cache.

allprojects {
  tasks.register("airResolveDependencies") {
    doLast {
      // Resolution may register new configurations (e.g. by IntelliJ Platform Gradle Plugin),
      // so iterate over snapshots until there is nothing left to resolve
      val resolved = mutableSetOf<String>()
      while (true) {
        val configs = project.configurations.filter { it.isCanBeResolved && it.name !in resolved }
        if (configs.isEmpty()) break
        for (config in configs) {
          resolved += config.name
          config.resolve()
        }
      }
    }
  }
}
