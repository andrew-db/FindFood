package com.krakenjaws.findfood.ui;

public interface IProfile {
    // An avatar uses the image as an integer resource in Firebase Firestore
    void onImageSelected(int resource);
}
