package com.example.orange.ui.entrant;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.orange.R;
import com.journeyapps.barcodescanner.ScanOptions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

@RunWith(AndroidJUnit4.class)
public class CreateQRFragmentTest {

    @InjectMocks
    private CreateQRFragment createQRFragment;

    @Mock
    private FragmentActivity mockActivity;

    @Mock
    private NavController mockNavController;


}
