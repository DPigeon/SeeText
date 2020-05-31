package com.ctext.objectdetection.definition;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(MockitoJUnitRunner.class)
public class DefinitionRowItemTest {
    final int iconId = 10000;
    final String type = "noun";
    final String definition = "A unit testing is a software testing method by which individual units of source code are tested to determine whether they are fit for use.";
    final String example = "I wrote some unit tests yesterday to test parts of my software.";

    DefinitionRowItem definitionRowItem;

    @Before
    public void setupMock() {
        definitionRowItem = new DefinitionRowItem(iconId, type, definition, example);
    }

    @Test
    public void testGetIcon() {
        int id = definitionRowItem.getIcon();
        assertEquals(10000, id);
    }

    @Test
    public void testSetIcon() {
        definitionRowItem.setIcon(20000);
        int actual = definitionRowItem.getIcon();
        assertEquals(20000, actual);
    }

    @Test
    public void testGetType() {
        String actual = definitionRowItem.getType();
        assertEquals("noun", actual);
    }

    @Test
    public void testSetType() {
        definitionRowItem.setType("verb");
        String actual = definitionRowItem.getType();
        assertEquals("verb", actual);
    }

    @Test
    public void testGetDefinition() {
        String actual = definitionRowItem.getDefinition();
        assertEquals("A unit testing is a software testing method by which individual units of source code are tested to determine whether they are fit for use.", actual);
    }

    @Test
    public void testSetDefinition() {
        definitionRowItem.setDefinition("Unit tests are tests...");
        String actual = definitionRowItem.getDefinition();
        assertEquals("Unit tests are tests...", actual);
    }

    @Test
    public void testGetExample() {
        String actual = definitionRowItem.getExample();
        assertEquals("I wrote some unit tests yesterday to test parts of my software.", actual);
    }

    @Test
    public void testSetExample() {
        definitionRowItem.setExample("I wrote my unit tests with JUnit.");
        String actual = definitionRowItem.getExample();
        assertEquals("I wrote my unit tests with JUnit.", actual);
    }

    @Test
    public void testToString() {
        String definitionSentence = definitionRowItem.toString();
        assertNotEquals(type + definition + example, definitionSentence); // Must have skip lines
    }
}