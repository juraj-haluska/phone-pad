package net.spacive.apps.phonepad;

import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    private float prevX = 0;
    private float prevY = 0;

    private boolean menuOpened = false;

    private LinearLayout layoutMenu;

    private Socket clientSocket;

    private float sensitivity = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        layoutMenu = findViewById(R.id.layout_menu);

        findViewById(R.id.btn_left).setOnClickListener((btn) -> {
            onLeftClicked();
        });

        findViewById(R.id.btn_right).setOnClickListener((btn) -> {
            onRightClicked();
        });

        findViewById(R.id.img_menu).setOnClickListener((img) -> {

            // animations
            if (menuOpened) {
                img.startAnimation(AnimationUtils.loadAnimation(
                        getApplicationContext(),
                        R.anim.menu_icon_close
                ));
                layoutMenu.setVisibility(View.INVISIBLE);
            } else {
                img.startAnimation(AnimationUtils.loadAnimation(
                        getApplicationContext(),
                        R.anim.menu_icon_open
                ));
                layoutMenu.setVisibility(View.VISIBLE);
            }

            menuOpened = !menuOpened;
        });

        findViewById(R.id.img_info).setOnClickListener((img -> {
            onInfoClicked();
        }));

        findViewById(R.id.img_settings).setOnClickListener((img) -> {
            onSettingsClicked();
        });

        getWindow().getDecorView().getRootView().setOnTouchListener((view, motionEvent) -> {
            onTouch(motionEvent.getX(), motionEvent.getY());
            view.performClick();
            return false;
        });
    }

    private void onLeftClicked() {

    }

    private void onRightClicked() {

    }

    private void onSettingsClicked() {
        listenForClient();
    }

    private void onInfoClicked() {
        try {
            List<NetworkInterface> ifaces = Collections.list(
                    NetworkInterface.getNetworkInterfaces()
            );

            String addrString = null;

            for (NetworkInterface itf : ifaces) {
                List<InetAddress> addrs = Collections.list(itf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        try {
                            Inet4Address addr4 = (Inet4Address) addr;
                            addrString = addr4.getHostAddress();
                            break;
                        } catch (ClassCastException ex) { }
                    }
                }
            }

            Toast.makeText(this, addrString, Toast.LENGTH_SHORT).show();
        } catch (SocketException e) {
            e.printStackTrace();
            Log.d(TAG, "interfaces error");
        }
    }

    private void onTouch(float x, float y) {
        final float dx = x - prevX;
        final float dy = y - prevY;

        // send data
        if (clientSocket != null && clientSocket.isConnected()) {

            int X = Math.round(dx * sensitivity);
            int Y = Math.round(dy * sensitivity);

            String data = Integer.toString(X) + ":" + Integer.toString(Y) + "\r\n";

            Log.d(TAG, "sending data: " + data);

            try {
                clientSocket.getOutputStream().write(data.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        prevX = x;
        prevY = y;
    }

    private void listenForClient() {
        Log.d(TAG, "listening for client");

        final ServerSocket serverSocket;

        try {
            serverSocket = new ServerSocket(8000);

            new Thread(() -> {
                try {
                    clientSocket = serverSocket.accept();
                    runOnUiThread(() -> onClientConnected());
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> onListeningFailed());
                } finally {
                    try {
                        serverSocket.close();
                    } catch (IOException e1) { }
                    Log.d(TAG, "finally called");
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> onListeningFailed());
        }
    }

    private void onClientConnected() {
        Log.d(TAG, "client connected");
    }

    private void onListeningFailed() {
        Log.d(TAG, "listening for client failed");
    }
}
