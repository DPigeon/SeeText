package com.ctext;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ProfileActivityTest {

    @Mock
    ProfileActivity mockActivity;

    @Before
    public void setupMock() {
        mockActivity = mock(ProfileActivity.class);
    }

    @Test
    public void testSetupUI() {
        doNothing().when(mockActivity).setupUI();

        mockActivity.setupUI();

        verify(mockActivity).setupUI();
    }

    @Test
    public void testAddRadioButtons() {
        //when(mockActivity.addRadioButtons(anyString(), )
    }

}
