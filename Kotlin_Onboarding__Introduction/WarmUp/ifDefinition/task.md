The conditional operator (the [`if`](https://kotlinlang.org/docs/control-flow.html#if-expression) expression)
is used when the program has some choice.
For example, if the variable contains a positive number, display it on the screen,
otherwise do nothing.
Or, if the guess matches the secret, end the game.

Consider the following example:
```kotlin
if (y > 0) {
    println(y)
}
```
Another example:
```kotlin
if (x > y) {
    println(x)
} else {
    // The opposite condition: x <= y
    println(y)
}
```

For the `else` branch, the reverse of the original condition is used: for example,
for `x > y`, the opposite condition is `x <= y`.
