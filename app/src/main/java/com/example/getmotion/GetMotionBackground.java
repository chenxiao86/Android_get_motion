package com.example.getmotion;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class GetMotionBackground extends Service {
    public GetMotionBackground() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
