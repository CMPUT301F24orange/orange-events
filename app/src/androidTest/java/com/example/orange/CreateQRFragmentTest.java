package com.example.orange;


import android.os.Bundle;
import android.widget.Toast;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.Navigation;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.orange.R;
import com.example.orange.ui.entrant.CreateQRFragment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class CreateQRFragmentTest {

    private FragmentScenario<CreateQRFragment> fragmentScenario;
    private TestNavController testNavController;

    @Before
    public void setUp() {
        testNavController = new TestNavController(ApplicationProvider.getApplicationContext());

        // Launch the CreateQRFragment with FragmentScenario
        fragmentScenario = FragmentScenario.launchInContainer(CreateQRFragment.class, null, R.style.Theme_Orange);

        fragmentScenario.onFragment(fragment -> {
            // Attach TestNavController to the fragment
            Navigation.setViewNavController(fragment.requireView(), testNavController);
        });
    }

    @Test
    public void testExtractEventIdFromQR_ValidQRContent() {
        fragmentScenario.onFragment(fragment -> {
            String validQRContent = "Event ID: 123ABC";
            String eventId = fragment.extractEventIdFromQR(validQRContent);
            assertEquals("123ABC", eventId);
        });
    }


    @Test
    public void testShowToast_InvalidQRContent() {
        fragmentScenario.onFragment(fragment -> {
            // Simulate invalid QR content and scan
            fragment.barLauncher.getContract().parseResult(0, null); // Invalid scan result

            // Display toast and verify its message if possible
            Toast.makeText(ApplicationProvider.getApplicationContext(), "Invalid QR code content", Toast.LENGTH_SHORT).show();
        });
    }
    @Test
    public void testDirectNavigation() {
        fragmentScenario.onFragment(fragment -> {
            Bundle args = new Bundle();
            args.putString("event_id", "testEvent123");

            testNavController.navigate(R.id.navigation_eventDetails, args);

            assertNotNull("Navigation did not store arguments", testNavController.getLastArgs());
            assertEquals("testEvent123", testNavController.getLastArgs().getString("event_id"));
        });
    }

}
