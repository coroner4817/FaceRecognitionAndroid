# Face Recognition Android App
An Android app tagging face with Clarifai API.  

## Implementation

* There are basically three activities:
  
  - `TagActivity`: find the face and tag it if find a match and update the embed in the database. Otherwise, just add the new face data to the database.  
    - I set the `MaxFaces` to be 1 to avoid get embedding of two faces in a image. Also, I slightly modify the code of `FaceCropper.java` to make it more flexible. I reduce the size of the cropped area to avoid interference of the background.
    - The matching is by calculate the Eucliden Distance. Here I hardcode the threshold to be 0.6, which is derived by testing on some dataset. 
    - Each time the app find a match face, it can update the embedding corresponding to this tag in the database. Here I just update it with equal weights, so that user can help correct their database to make the embedding not overfitting.
    - I also add a checkbox for user to input if the person detected is a family member or not. This can be viewed as using users themselves to label the data. This is based on the belief that family member will have similar looking, and similar embed as well. We can use this data for a secondary classification. 

  - `EditDBActivity`: enable user to view a listview of the current database and they can delete a tag by long clicking.

  - `VisualizeDBActivity`: perform PCA on the embedding data and plot the projection of the original data on the top 2 Principal Components space. This can help user better view the face difference and similarity. We can also using the PC score to discriminate the family member and the others, since family member's plot should locate at a certain area of the graph if they look similar..

## Demo

* Here is a short demo video: https://vimeo.com/213613220

