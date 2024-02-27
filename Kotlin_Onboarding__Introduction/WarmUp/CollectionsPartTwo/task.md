To be able to implement `countPartialMatches` function on the next step, firstly to add 
a new function `countAllMatches` to calculate all matches

### Task

Add and implement a new function _countAllMatches_ which has two string arguments (_secret_ and _guess_)
and returns the number of matched letters between them that don't depend on the position.

<div class="hint" title="Click me to see the signature of the countAllMatches function">

The signature of the function is:
```kotlin
fun countAllMatches(secret: String, guess: String): Int
```
</div>

You can implement this function in any possible way, but we _recommend_ to look into the `filter` and `minOf` built-in functions.

<div class="Hint" title="Click me to learn more about filter built-in function">

You can use the <a href="https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/filter.html"><code>filter</code></a> function
to filter elements from one collection that are in another:

```kotlin
val list1 = listOf(1, 2, 3, 4)
val list2 = listOf(3, 4, 5, 6)
println(list1.filter{ it in list2 }) // [3, 4]
```
</div>

<div class="Hint" title="Click me to learn more about minOf built-in function">

You can use the <a href="https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.comparisons/min-of.html"><code>minOf</code></a> function
to find a minimum values from several ones:

```kotlin
println(minOf(2, 3, 5, -1)) // -1
```
</div>

If you have any difficulties, **hints will help you solve this task**.

----

### Hints

<div class="Hint" title="Click me to see examples of the `countAllMatches` function's work">

Here are several examples of the _countAllMatches_ function's work:

- guess = "ACEB", secret = "BCDF", result = 2;
- guess = "ABCD", secret = "DCBA", result = 4;
- guess = "AAAA", secret = "ABBB", result = 1;
- guess = "BBBB", secret = "BBDH", result = 2.
</div>

<div class="Hint" title="Click me to learn the main idea of the algorithm">

You can just count the number of letters from the guess that are in secret.
But in some cases it will prodice an incorrect answer, for example, if the guess has the same letters:
```text
guess = "BBBB", secret = "BBDH"
```
If we calculate the number of letter from the guess `BBBB` that are in the secret `BBDH`, 
we will get `4`, but the correct answer is `2`.

To avoid these mistakes we can calculate the number of letters from the guess that are in secret, 
but also the opposite one - the number of letters from the secret that are in guess.
And then just to calculate the minimum of them:
```text
guess = "BBBB", secret = "BBDH"
1) the number of letter from the guess `BBBB` that are in the secret `BBDH` is 4
2) the number of letter from the secret `BBDH` that are in the guess `BBBB`is 2
3) the minimum is 2. The correct answer is 2.
```
</div>

