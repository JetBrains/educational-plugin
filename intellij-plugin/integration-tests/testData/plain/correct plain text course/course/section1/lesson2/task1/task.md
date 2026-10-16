
This is a task description file.
Its content will be displayed to a learner
in the **Task Description** window.

It supports both Markdown and HTML.
To toggle the format, you can rename **task.md**
to **task.html**, or vice versa.
The default task description format can be changed
in **Preferences | Tools | Education**,
but this will not affect any existing task description files.

The following features are available in
**task.md/task.html** which are specific to the JetBrains Academy plugin:

- Hints can be added anywhere in the task text.
  Type "hint" and press Tab.
  Hints should be added to an empty line in the task text.
  In hints you can use both HTML and Markdown.
<div class="hint">

Text of your hint

</div>

- You may need to refer your learners to a particular lesson,
task, or file. To achieve this, you can use the in-course links.
Specify the path using the `[link_text](course://lesson1/task1/file1)` format.

- You can insert shortcuts in the task description.
While **task.html/task.md** is open, right-click anywhere
on the **Editor** tab and choose the **Insert shortcut** option
from the context menu.
For example: &shortcut:FileStructurePopup;.

- Insert the &percnt;`IDE_NAME`&percnt; macro,
which will be replaced by the actual IDE name.
For example, **%IDE_NAME%**.

- Insert PSI elements, by using links like
`[element_description](psi_element://link.to.element)`.
To get such a link, right-click the class or method
and select **Copy Reference**.
Then press &shortcut:EditorPaste; to insert the link where appropriate.
For example, a [link to the "contains" method](psi_element://java.lang.String#contains).

- You can add link to file using **full path** like this:
  `[file_link](file://lesson1/task1/file.txt)`.