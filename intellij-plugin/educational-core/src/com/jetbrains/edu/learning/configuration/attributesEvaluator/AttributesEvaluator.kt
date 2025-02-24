package com.jetbrains.edu.learning.configuration.attributesEvaluator

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore.VFS_SEPARATOR_CHAR
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.configuration.CourseFileAttributes
import com.jetbrains.edu.learning.configuration.InclusionPolicy
import com.jetbrains.edu.learning.courseFormat.Course

/**
 * See [AttributesEvaluator]
 */
class AttributesBuilderContext private constructor(
  private val specificity: Int,
  private val pathSegmentSpecifications: List<PathSegmentSpecification>
) {

  constructor() : this(0, emptyList())

  internal var rules: MutableList<Rule> = mutableListOf()
  private val setupAttributes: MutableList<AttributesMutator> = mutableListOf()

  fun excludeFromArchive() {
    setupAttributes += { it.excludedFromArchive = true }
  }

  fun includeIntoArchive() {
    setupAttributes += { it.excludedFromArchive = false }
  }

  fun inclusionPolicy(policy: InclusionPolicy) {
    setupAttributes += { it.inclusionPolicy = policy }
  }

  private fun addRule(
    args: Iterable<Any>,
    direct: Boolean,
    mustBeDirectory: Boolean,
    mustBeFile: Boolean,
    specificityIncrease: Int = 1,
    subRules: AttributesBuilderContext.() -> Unit
  ) {
    // rules with any do not increase specificity
    val newSpecificity = specificity + specificityIncrease

    val newPathSegmentSpecifications = pathSegmentSpecifications + PathSegmentSpecification(
      direct, PathSegmentPredicate.create(args, mustBeDirectory, mustBeFile)
    )

    val subContext = AttributesBuilderContext(newSpecificity, newPathSegmentSpecifications)
    subContext.subRules()

    val baseRule = Rule(
      newSpecificity,
      subContext.setupAttributes,
      newPathSegmentSpecifications
    )

    rules += baseRule
    rules += subContext.rules
  }

  /**
   * Matches directories
   */
  fun dir(
    vararg args: Any,
    direct: Boolean = false,
    subRules: AttributesBuilderContext.() -> Unit
  ) = addRule(args.asIterable(), direct, mustBeDirectory = true, mustBeFile = false, 1, subRules)

  /**
   * The rule specification
   * ```
   * dirAndChildren(x) {
   *  // some rules
   * }
   * ```
   * is equivalent to:
   * ```
   * dir(x) {
   *   // some rules
   *   any {
   *     // the same rules
   *   }
   * }
   * ```
   *
   * So, the rules apply to the directory itself and to all its children.
   */
  fun dirAndChildren(
    vararg args: Any,
    direct: Boolean = false,
    subRules: AttributesBuilderContext.() -> Unit
  ) = dir(*args, direct = direct) {
    subRules(this)
    any(direct, subRules)
  }

  /**
   * Matches files. [subRules] should not have any inner matchers, they will never match.
   */
  fun file(
    vararg args: Any,
    direct: Boolean = false,
    subRules: AttributesBuilderContext.() -> Unit
  ) = addRule(args.asIterable(), direct, mustBeDirectory = false, mustBeFile = true, 1, subRules)

  /**
   * Matches either a file or a directory
   */
  fun name(
    vararg args: Any,
    direct: Boolean = false,
    subRules: AttributesBuilderContext.() -> Unit
  ) = addRule(args.asIterable(), direct, mustBeDirectory = false, mustBeFile = false, 1, subRules)

  /**
   * Matches just anything. This rule specification does not increase the specificity of sub rules.
   */
  fun any(direct: Boolean = false, subRules: AttributesBuilderContext.() -> Unit) =
    addRule(listOf(predicateAny), direct, mustBeDirectory = false, mustBeFile = false, 0, subRules)

  /**
   * Matches files with the specified extensions.
   */
  fun extension(vararg args: String, direct: Boolean = false, subRules: AttributesBuilderContext.() -> Unit) =
    addRule(args.map {
      ExtensionPredicate(it)
    }, direct, mustBeDirectory = false, mustBeFile = true, 1, subRules)

  /**
   * Creates an arbitrary predicate on a name of a path segment
   */
  fun pred(accepts: (String) -> Boolean): PathSegmentNamePredicate = object : PathSegmentNamePredicate {
    override fun accepts(name: String): Boolean = accepts(name)
  }
}

/**
 * A DSL for building rules to set file attributes based on file paths. Example:
 *
 * ```
 * dir("build", "out", "target", direct=true) { // matches folders with the specified names, they must be root folders (direct==true)
 *   file("a.txt) {
 *     // sets attributes for a.txt files that are directly or indirectly inside build/out/target directories. Specificity = 1
 *   }
 *
 *   any {
 *     // sets attributes for any file or directory inside build/out/target directories. Specificity = 0 ("any" does not increase)
 *   }
 *
 *   set some attributes // sets attributes specifically for build/out/target directories. Specificity = 0
 * }
 * ```
 */
class AttributesEvaluator(base: AttributesEvaluator? = null, rulesBuilder: AttributesBuilderContext.() -> Unit) {

  private val rules: MutableList<Rule> = base?.rules?.toMutableList() ?: mutableListOf()

  init {
    val baseContext = AttributesBuilderContext()
    rulesBuilder(baseContext)
    this.rules += baseContext.rules.sortedBy { it.specificity }
  }

  fun attributesForFile(holder: CourseInfoHolder<out Course?>, file: VirtualFile): CourseFileAttributes {
    val relativePath = FileUtil.getRelativePath(holder.courseDir.path, file.path, VFS_SEPARATOR_CHAR)
                       ?: return CourseFileAttributesMutable().toImmutable()

    val fixedRelativePath = if (relativePath == ".") "" else relativePath

    return attributesForPath(fixedRelativePath, file.isDirectory)
  }

  fun attributesForPath(relativePath: String, isDirectory: Boolean = false): CourseFileAttributes {
    val pathSegmentsNames = relativePath.split(VFS_SEPARATOR_CHAR)
    val pathSegments = pathSegmentsNames.mapIndexed { i, segmentName ->
      val directory = i < pathSegmentsNames.lastIndex || isDirectory
      PathSegment(segmentName, directory)
    }

    val attributes = CourseFileAttributesMutable()

    for (rule in rules) {
      if (rule.matches(pathSegments)) {
        rule.setupAttributes.forEach { it(attributes) }
      }
    }

    return attributes.toImmutable()
  }
}