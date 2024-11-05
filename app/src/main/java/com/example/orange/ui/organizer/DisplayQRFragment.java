package com.example.orange.ui.organizer;

import android.graphics.Bitmap;
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

public class DisplayQRFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_displayqr, container, false);

        ImageView qrImageView = view.findViewById(R.id.qrImageView);
        Bitmap qrBitmap = getArguments() != null ? getArguments().getParcelable("qr_bitmap") : null;

        if (qrBitmap != null) {
            qrImageView.setImageBitmap(qrBitmap);
        } else {
            Toast.makeText(requireContext(), "Failed to load QR code", Toast.LENGTH_SHORT).show();
        }

        return view;
    }
}

