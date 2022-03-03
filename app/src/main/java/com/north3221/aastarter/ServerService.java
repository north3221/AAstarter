package com.north3221.aastarter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.TaskStackBuilder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import static android.app.NotificationManager.IMPORTANCE_HIGH;

import com.north3221.aastarter.R;

public class ServerService extends Service {

    static final int UdpServerPORT = 4455;
    private static final String TAG = "AAStarter";
    private NotificationManager mNotificationManager;
    private UdpServerThread srvthread = null;
    private DatagramSocket socket = null;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        String CHANNEL_ONE_ID = "org.ventoso.aastarter";
        String CHANNEL_ONE_NAME = "Channel One";
        NotificationChannel notificationChannel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(CHANNEL_ONE_ID,
                    CHANNEL_ONE_NAME, IMPORTANCE_HIGH);
            notificationChannel.enableLights(false);
            //notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(notificationChannel);
        }

        Intent tapIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(tapIntent);
        PendingIntent tapPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent stopIntent = new Intent(this, MainActivity.class);
        stopIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        stopIntent.setAction("org.ventoso.aastarter.STOP");
        PendingIntent stopPendingIntent = PendingIntent.getActivity(this, 0, stopIntent, 0);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder mynotification = new Notification.Builder(this)
                .setContentTitle("Android Auto Starter")
                .setContentText("Running")
                .setSmallIcon(R.drawable.aastarter)
                .setContentIntent(tapPendingIntent)
                .addAction(0, "STOP", stopPendingIntent)
                .setTicker("");
        if (Build.VERSION.SDK_INT>=26)
            mynotification.setChannelId(CHANNEL_ONE_ID);

        startForeground(1, mynotification.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        if (srvthread!=null)
            return START_STICKY;
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Service Started");
        srvthread = new UdpServerThread(UdpServerPORT);
        srvthread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (srvthread!=null) {
            Toast.makeText(this, "service stopping", Toast.LENGTH_SHORT).show();
            srvthread.terminate();
            srvthread = null;
        }
        mNotificationManager.cancelAll();
    }

    private class UdpServerThread extends Thread{

        int serverPort;

        boolean running;

        public UdpServerThread(int serverPort) {
            super();
            this.serverPort = serverPort;
        }

        public void terminate(){
            this.running = false;
            try {
                if (socket != null)
                    socket.close();
            } catch (Exception e) {
                Log.e(TAG, "tcp - closing socket "+e.getMessage());
            }
        }

        @Override
        public void run() {

            running = true;

            try {
                socket = new DatagramSocket(serverPort);

                Log.d(TAG, "UDP Server is running");

                while(running) {

                    byte[] buf = new byte[1];

                    // receive request
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    try {
                        socket.receive(packet);
                    } catch (IOException e) {
                        if (running) {
                            Log.e(TAG, e.getMessage());
                        } else {
                            Log.d(TAG, "thread terminated, socket.receive threw "+e.getMessage());
                        }
                    }

                    if (running) {
                        String address = packet.getAddress().getHostAddress();
                        Log.d(TAG, "Received " + new String(buf) + " from " + address);
                        int port = packet.getPort();
                        if (buf[0] != 'E') {
                            try {
                                AALaunch.connect(getApplicationContext(), address);
                            } catch (Exception e) {
                                Log.e(TAG, e.getMessage());
                            }
                        }
                    }
                }
                Log.e(TAG, "UDP Server ended");
            } catch (SocketException e) {
                Log.e(TAG, e.getMessage());
            }
            stopSelf();
        }
    }
}
