Let's add one more function to help us solve the task.

### Task

Implement the `applyFilter` function. It accepts a picture
and a filter name, applies the `trimPicture` function to the picture, then 
applies the specified filter, and finally returns the updated picture. To apply a filter, 
just call one of the already defined functions: `applyBordersFilter` or `applySquaredFilter`.

<div class="hint" title="Click me to see the signature of the applyFilter function">

The signature of the function is:
```kotlin
fun applyFilter(picture: String, filter: String): String
```
</div>

The possible values for the `filter` argument:

- `borders` – this adds a border to the image using the `applyBordersFilter` function
- `squared` - this replicates the image 4 times using the `applySquaredFilter` function

Should an unrecognized filter name be used, the `applyFilter` function will throw an error to alert the user.
