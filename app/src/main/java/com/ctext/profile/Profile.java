package com.ctext.profile;

/*
 * Profile of the user used through SharedPreferences
 */

public class Profile {
    private int languageId;
    private int languageOutputId;

    public Profile(int languageId, int languageOutputId) {
        this.languageId = languageId;
        this.languageOutputId = languageOutputId;
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
}
