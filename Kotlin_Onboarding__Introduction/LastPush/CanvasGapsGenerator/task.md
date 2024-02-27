This step is not easy, but we believe you will solve it!

### Task

On this step, you need to implement the `canvasWithGapsGenerator` function, which accepts the `pattern`, `width`, and `height` that were inputted by the user.
This function should return a new string with a generated canvas-with-gaps picture.

<div class="hint" title="Click me to see the new signature of the canvasWithGapsGenerator function">

The signature of the function is:
```kotlin
fun canvasWithGapsGenerator(pattern: String, width: Int, height: Int): String
```
</div>

The `canvasGaps` generator should build a rectangle `width` x `height` from the pattern,
while leaving gaps in place of every other element.
The generator works according to the following algorithm:
1) **None of the levels** of the generated image **change** the pattern;
2) The gap is a rectangle of spaces, the dimensions
   of which are equal to the width and height of the initial pattern;
3) In every **odd level**, the gap should be in **even** positions,
   and in every **even level** - in **odd** positions;
4) When repeated **vertically**, the pattern remains **unchanged**.

<div class="hint" title="Click me to see the `canvasGaps` filter examples">
  For example, let's take the pattern:

```text
 X
/ \
\ /
 X
```

The resulting 5 x 3 picture will be:

```text
            CORRECT:             INCORRECT: 
            
            X     X     X         X     X     X 
1st level: / \   / \   / \       / \   / \   / \
           \ /   \ /   \ /       \ /   \ /   \ /
            X     X     X         X     X     X
               X     X              / \   / \
2nd level:    / \   / \             \ /   \ /    
              \ /   \ /              X     X
               X     X           / \   / \   / \
            X     X     X        \ /   \ /   \ /    
3rd level: / \   / \   / \        X     X     X 
           \ /   \ /   \ /
            X     X     X 
```

None of the levels of the generated image change the pattern.
</div>

The following functions will _not be checked_, but they can help you implement this project:


- `repeatHorizontallyWithGaps`, which accepts a `pattern` and the number of times that pattern should be repeated horizontally (n), e.g.:
```text
n = 5
○○             ○○  ○○  ○○
○○    ---->    ○○  ○○  ○○
```

If you have any difficulties, **hints will help you solve this task**.

----

### Hints

<div class="hint" title="Click me to learn about the `dropLast` built-in function">

To drop <code>n</code> last symbols from a <code>String</code>, you can use the <a href="https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/drop-last.html"><code>dropLast</code></a> function, e.g.:
  ```kotlin
  val a = "MyText"
  println(a.dropLast(3)) // MyT
  ```
If you need to drop <code>n</code> symbols from the beginning of a <code>String</code>, you can use the <a href="https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/drop.html"><code>drop</code></a> function.
</div>

<div class="hint" title="Click me to get a code style hint">

In industrial programming, duplicate code is commonly avoided and put into functions. 
However, it is not always possible to immediately write clear and readable code. 
Try implementing the <code>canvasGenerator</code> and <code>canvasWithGapsGenerator</code> functions 
so that they pass all the tests and then proceed with improving the code. 
This process is called <i>refactoring</i>.
</div>
