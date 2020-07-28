package com.seetext.profile;

/*
 * Profile of the user used through SharedPreferences
 */

public class Profile {

    private int languageId;
    private int languageOutputId;
    private int lensFacing;
    private int mode; // 0 --> SpeechRecognition, 1 --> ObjectDetection

    public Profile(int languageId, int languageOutputId, int lensFacing, int mode) {
        this.languageId = languageId;
        this.languageOutputId = languageOutputId;
        this.lensFacing = lensFacing;
        this.mode = mode;
    }

    public void setLanguage(int id) {
        languageId = id;
    }

    public int getLanguage() {
        return languageId;
    }

    public void setLanguageOutput(int id) {
        languageOutputId = id;
    }

    public int getLanguageOutputId() {
        return languageOutputId;
    }

    public void setLensFacing(int lens) {
        this.lensFacing = lens;
    }

    public int getLensFacing() {
        return lensFacing;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getMode() {
        return mode;
    }
}
