package com.example.orange.ui.entrant;

import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
/**
 * QR scanner that scans event QR codes to send it to the event details
 *
 * @author Brandon Ramirez
 */
public class CreateQRFragment extends Fragment {
    private final ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                // Send to the event here
            }
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scanQRCode();
    }

    /**
     * Creates a new scan option which activates the camera and changes it settings
     */
    private void scanQRCode() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan your QR code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barLauncher.launch(options);
    }

}
