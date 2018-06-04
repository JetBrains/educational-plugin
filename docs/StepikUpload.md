## Item-wise update

It's an update that is called from the context menu on an item, e.g. course, section, lesson or task.

####Post

- Course

    - with top-level lessons
    
      *Result.* On Stepik course consist of the section named after a course and contains all top-level lessons.
    - with sections only

      *Result.* On Stepik course has the same structure as a local course.
      
    - with sections and top-level lessons both
    
      *Result.* Before push, plugin warns a user that since there are sections top-level lessons will be wrapped into sections.
       If users agreed then we wrap each lesson into a separate section and post a course as a course with sections.
       
- Section

    *Result.* A section is posted on Stepik. An additional material section should be the last section.
    
- Lesson
    - Top-level lesson. The course contains top-level lessons only
    
      *Result.* A lesson is added to existing section that named after a course.
      
    - Top-level lesson. The course contains sections only
    
      *Result.* Before push, plugin warns a user that since there are sections this top-level lesson will be wrapped into a section. 
      If users agreed then we wrap this lesson into a separate section and post the corresponding section.
      
- Lesson in section

  *Result.* A lesson is posted in the corresponding section.
  
- Task
    
  *Result.* A task is posted into the corresponding lesson.
  
####Update

- Course

  *Result*. Currently, it posts all new tasks and updates all posted tasks.

-Section

  *Possible changes*. Section name, section position, section lessons.
  
  *Currently not working*. Section and its lesson removing.
  
  *Result.* A section is updated on Stepik, it means that name, position and its content was updated.

- Lesson

  *Possible changes*. Lesson name, lesson position, lesson content
  
  *Currently not working*. Lesson removing, lesson moving from one section to another.
  
  *Result.* Lesson info and/or its tasks are updated on Stepik.
  
  
  *Possible changes*. Moving pushed top-level lessons into a section
  
  *Result*. Post created a section, remove the old section for top-level lessons that was named after course.


##Event-based update
This update is called from synchronize course menu on Stepik widget and from the context menu when clicking on course item.

Since the last update, we watch all changes to items files and items info and mark them as changed. When user updates course, we

compare all marked items with the latest server version. If an item was deleted, we asked a user if he really wants to delete it. 