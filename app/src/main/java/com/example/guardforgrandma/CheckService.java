package com.example.guardforgrandma;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class CheckService extends Service {
    private Timer timer;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        String channelId = "com.guardGrandma";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "guard for grandma report channel", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);

        final IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                Intent batteryStatus = registerReceiver(null, ifilter);
                if(batteryStatus != null){
                    int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    float batteryPct = level * 100 / (float)scale;
                    report("battery", String.valueOf(batteryPct));
                }
                Context context = getApplicationContext();
                ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                boolean isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
                report("wifi", isWiFi +"");
            }
        };
        timer = new Timer();
        timer.schedule(timerTask,0,10*60*1000);
        startForeground(1, builder.build());

    }

    private void report(String title, String data) {
        URL url = null;
        try {
            url = new URL("http://myks790.iptime.org:41086/report");

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);

            try {
                DataOutputStream stream = new DataOutputStream(connection.getOutputStream());
                stream.writeBytes("title="+title+"&data="+data);
                stream.flush();
                stream.close();
                int responseCode = connection.getResponseCode();
            } finally {
                connection.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_REDELIVER_INTENT;
    }
}
