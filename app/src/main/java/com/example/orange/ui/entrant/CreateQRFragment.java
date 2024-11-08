    package com.example.orange.ui.entrant;

    import android.os.Bundle;
    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.Toast;

    import androidx.activity.result.ActivityResultLauncher;
    import androidx.annotation.NonNull;
    import androidx.annotation.Nullable;
    import androidx.fragment.app.Fragment;
    import androidx.navigation.NavController;
    import androidx.navigation.Navigation;

    import com.example.orange.R;
    import com.journeyapps.barcodescanner.ScanContract;
    import com.journeyapps.barcodescanner.ScanOptions;

    import java.util.regex.Matcher;
    import java.util.regex.Pattern;

    /**
     * QR scanner that scans event QR codes to send it to the event details
     *
     * @author Brandon Ramirez
     */
    public class CreateQRFragment extends Fragment {
        public ActivityResultLauncher<ScanOptions> barLauncher;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            initializeBarLauncher();
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            // Inflate the fragment layout to avoid blank screens and ensure setup.
            View view = inflater.inflate(R.layout.fragment_event_qr_code, container, false);
            return view;
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            // Initiate the scan after the view is created.
            scanQRCode();
        }

        private void initializeBarLauncher() {
            barLauncher = registerForActivityResult(
                    new ScanContract(),
                    result -> {
                        if (result.getContents() != null) {
                            String qrContent = result.getContents();
                            String eventId = extractEventIdFromQR(qrContent);
                            if (eventId != null) {
                                Bundle args = new Bundle();
                                args.putString("event_id", eventId);
                                Log.d("CreateQRFragment", "Navigating with args: " + args);
                                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                                navController.navigate(R.id.navigation_eventDetails, args);
                            } else {
                                Toast.makeText(requireContext(), "Invalid QR code content", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
            );
        }

        public String extractEventIdFromQR(String qrContent) {
            Pattern pattern = Pattern.compile("Event ID: (\\w+)");
            Matcher matcher = pattern.matcher(qrContent);
            return matcher.find() ? matcher.group(1) : null;
        }

        private void scanQRCode() {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan your QR code");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            options.setCaptureActivity(CaptureAct.class);
            barLauncher.launch(options);
        }
    }

