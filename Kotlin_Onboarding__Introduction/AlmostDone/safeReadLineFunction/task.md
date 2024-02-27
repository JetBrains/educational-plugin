In previous assignments, we sometimes used the predefined `safeReadLine` function instead of the built-in `readlnOrNull`.
The main reason is that `readlnOrNull` returns a _nullable_ value (`String?`).
The pre-defined `safeReadLine` function processed the user's input with the Elvis operator:
it returns the string or throws an error if a `null` value was received.
Now, it's time to implement this function on your own!

### Task

Implement the `safeReadLine` function, which returns the string input by the user or throws an error
if a `null` value was received.

<div class="hint" title="Click me to see the signature of the safeReadLine function">

The signature of the function is:
```kotlin
fun safeReadLine(): String
```
</div>
