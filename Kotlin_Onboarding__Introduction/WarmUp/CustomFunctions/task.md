In this task we will add several _signatures_ of functions and implement one of them.

### Task

Add several functions to the game:

- _generateSecret_, which should return the hidden word. 
For the time being, let this function always return  `ABCD`.

<div class="hint" title="Click me to see the signature of the generateSecret function">

The signature of the function is:
```kotlin
fun generateSecret(): String
```
</div>

- _countPartialMatches_, which has two string arguments (_secret_ and _guess_)
and returns the number of matched letters between them that are not in the same positions.
You don't need to implement this function now, it is enough to use the `TODO` 
function instead of implementation as a temporary solution. 
We will implement this function during solving next tasks.

<div class="hint" title="Click me to see the signature of the countPartialMatches function">

The signature of the function is:
```kotlin
fun countPartialMatches(secret: String, guess: String): Int
```
</div>

- _countExactMatches_, which has two arguments (_secret_ and _guess_)
and returns the number of exact matched positions between them. 
You don't need to implement this function now, it is enough to use the `TODO`
function instead of implementation as a temporary solution.
We will implement this function during solving next tasks.

<div class="hint" title="Click me to see the signature of the countExactMatches function">

The signature of the function is:
```kotlin
fun countExactMatches(secret: String, guess: String): Int
```
</div>

If you have any difficulties, **hints will help you solve this task**.

----

### Hints

<div class="hint" title="Push to learn why it is better to define generateSecret functions as an expression">

The `generateSecret` is very short, so we can make code shorter and rewrite this function in the form of expression:
```kotlin
fun generateSecret(): String {
    return "ABCD"
}
```
can be rewritten with
```kotlin
fun generateSecret() = "ABCD"
```

The short form is also easier to read and understand.
</div>
