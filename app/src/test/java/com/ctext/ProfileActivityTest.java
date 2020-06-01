package com.ctext;

import android.os.Bundle;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class ProfileActivityTest {

    @Mock
    ProfileActivity mockActivity;

    @Before
    public void setupMock() {
        mockActivity = mock(ProfileActivity.class);
    }

    @Test
    public void testLifeCycle() {
        // TODO: mock some calls

        Bundle savedInstanceState = new Bundle();
        mockActivity.onCreate(savedInstanceState);
        mockActivity.onStart();

    }

}
