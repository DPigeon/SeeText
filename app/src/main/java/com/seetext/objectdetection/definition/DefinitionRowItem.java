package com.seetext.objectdetection.definition;

/*
 * Class used to define the items in the listView of object definitions
 */

import org.jetbrains.annotations.NotNull;

public class DefinitionRowItem {

    private int icon;
    private String type;
    private String definition;
    private String example;

    public DefinitionRowItem(int icon, String type, String definition, String example) {
        this.icon = icon;
        this.type = type;
        this.definition = definition;
        this.example = example;
    }

    public int getIcon() { return icon; }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    @NotNull
    @Override
    public String toString() {
        return type + "\n" + definition + "\n" + '"' + example + '"';
    }
}
