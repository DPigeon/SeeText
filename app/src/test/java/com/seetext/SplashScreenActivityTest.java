package com.seetext;

import android.os.Bundle;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SplashScreenActivityTest {

    @Mock
    SplashScreenActivity mockActivity;

    @Before
    public void setupMock() {
        mockActivity = mock(SplashScreenActivity.class);
    }

    @Test
    public void testOnCreate() {
        doNothing().when(mockActivity).onCreate(any(Bundle.class));

        Bundle savedInstanceState = new Bundle();
        mockActivity.onCreate(savedInstanceState);

        verify(mockActivity).onCreate(any(Bundle.class));
    }
}