For complex conditions in `if` expressions,
special [built-in operators](https://kotlinlang.org/docs/booleans.html) `||` or `&&` can be used:
`||` is true if _at least one_ condition is true;
`&&` is true if _all_ the conditions are true:
```kotlin
// Will be true if x > 5 OR y > 5, e.g., x = 3, y = 6 (true) or x = 6, y = 6 (true)
if (x > 5 || y > 5) {
    TODO("Not implemented yet")
}
```
```kotlin
// Will be true if x > 5 AND y > 5, e.g., for x = 3, y = 6 (false) or x = 6, y = 6 (true)
if (x > 5 && y > 5) {
    TODO("Not implemented yet")
}
```

