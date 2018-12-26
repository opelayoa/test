package pelayo.com.mx.gpstest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;

public class MyService extends Service {

    private Handler handler;
    private Runnable runnable;

    GeolocationHelper geolocationHelper;
    private final static int INTERVAL = 5000;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        geolocationHelper = new GeolocationHelper(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        runnable = new TimerRunnable();
        initLocation();
        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable, INTERVAL);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(runnable);
        mNotificationManager.cancelAll();
        super.onDestroy();
    }

    private class TimerRunnable implements Runnable {

        public TimerRunnable() {

        }

        @Override
        public void run() {

            Location location = geolocationHelper.getLastLocation();
            sendLocation(location);
            handler.postDelayed(this, INTERVAL);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initLocation() {
        geolocationHelper.addLocationListener(new CallbackResponse<Location>() {
            @Override
            public void sendValue(Location response) {

            }

            @Override
            public void error(Exception e) {

            }
        });
    }

    private void sendLocation(Location location) {
        sendNotification();
    }

    private NotificationManager mNotificationManager;

    public void sendNotification() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this.getApplicationContext(), "notify_001");


        // mBuilder.setContentIntent(pendingIntent);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher_round);
        mBuilder.setContentTitle("Your Title");
        mBuilder.setContentText("Your text");
        mBuilder.setPriority(Notification.PRIORITY_MAX);

        mBuilder.setOngoing(true);
        mBuilder.setSound(null);

        mNotificationManager =
                (NotificationManager) this.getSystemService(this.getApplicationContext().NOTIFICATION_SERVICE);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "YOUR_CHANNEL_ID";
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_NONE);
            mNotificationManager.createNotificationChannel(channel);
            mBuilder.setChannelId(channelId);
        }

        mNotificationManager.notify(0, mBuilder.build());
    }
}
