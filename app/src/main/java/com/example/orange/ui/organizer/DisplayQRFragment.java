package com.example.orange.ui.organizer;


import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.orange.R;
/**
 * In charge of displaying the QR code that was generated in the ViewMyEventsFragment
 *
 * @author Brandon Ramirez.
 */
public class DisplayQRFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_displayqr, container, false);
        ImageView qrImageView = view.findViewById(R.id.qrImageView);

        // Retrieve the URI from arguments
        Uri qrUri = getArguments().getParcelable("qr_uri");
        if (qrUri != null) {
            qrImageView.setImageURI(qrUri); //Display the image
        } else {
            Toast.makeText(requireContext(), "Failed to load QR image", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Nullify or release resources here
    }

}

