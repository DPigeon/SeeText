package com.ctext;

public class Profile {
    private int languageId;

    public Profile(int languageId) {
        this.languageId = languageId;
    }

    public void setLanguage(int id) {
        languageId = id;
    }

    public int getLanguage() {
        return languageId;
    }
}
