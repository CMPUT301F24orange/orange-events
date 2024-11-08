package com.example.orange;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility;

import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import com.example.orange.R;
import com.example.orange.ui.organizer.DisplayQRFragment;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DisplayQRTest {

    @Rule
    public GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.READ_EXTERNAL_STORAGE);

    private Uri sampleUri;

    @Before
    public void setUp() {
        // Use a sample Uri for testing
        sampleUri = Uri.parse("content://com.example.orange/test_qr_image");
    }

    @Test
    public void testQRCodeImageDisplayed_whenUriIsPassed() {
        // Pass the sample URI as an argument
        Bundle args = new Bundle();
        args.putParcelable("qr_uri", sampleUri);

        // Launch the fragment with the argument
        FragmentScenario<DisplayQRFragment> scenario = FragmentScenario.launchInContainer(DisplayQRFragment.class, args);

        // Check if the ImageView is displayed
        onView(withId(R.id.qrImageView)).check(matches(isDisplayed()));
    }
}

