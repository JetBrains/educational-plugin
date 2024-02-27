### The conditional operator as an expression: variables

In Kotlin, the `if` expression can be also assigned to a variable
or be a returned value in a function; in such a case, the **last** operator in each branch will be returned:
```kotlin
val max = if (x > y) {
    println(x)
    // if x > y return x
    x
} else {
    // The opposite condition: x <= y
    println(y)
    // if x <= y return y
    y
}
```
For input `x = 5` and `y = 15`, the result will be `max = 15`; and otherwise,
if `x = 20` and `y = 15`, the result will be `max = 20`.

### The conditional operator as an expression: functions

It is the same with functions:
```kotlin
fun max(x: Int, y: Int) = if (x > y) {
    println(x)
    // if x > y return x
    x
  } else {
    // The opposite condition: x <= y
    println(y)
    // if x <= y return y
    y
  }
```
Or, in the full notation:
```kotlin
fun max(x: Int, y: Int): Int {
    return if (x > y) {
      println(x)
      // if x > y return x
      x
    } else {
      // The opposite condition: x <= y
      println(y)
      // if x <= y return y
      y
    }
}
```