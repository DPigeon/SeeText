package com.seetext.translator;

/*
 * An interface to give the translated text as a response to Main Activity
 */

public interface TranslatorCallback {
    void translateTheText(String text);
}
