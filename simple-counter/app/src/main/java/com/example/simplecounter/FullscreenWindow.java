package com.example.simplecounter;

import android.app.Activity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

final class FullscreenWindow {
    private FullscreenWindow() {
    }

    static void apply(Activity activity) {
        Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
    }
}
