package com.example.orange;
import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;

import android.content.Context;
import android.os.Bundle;

public class TestNavController extends NavController {
    private Bundle lastArgs;

    public TestNavController(@NonNull Context context) {
        super(context);
    }

    @Override
    public void navigate(int resId, Bundle args, NavOptions navOptions) {
        this.lastArgs = args; // Store arguments for verification
        System.out.println("navigate called with args: " + args);
    }

    public Bundle getLastArgs() {
        return lastArgs;
    }
}

