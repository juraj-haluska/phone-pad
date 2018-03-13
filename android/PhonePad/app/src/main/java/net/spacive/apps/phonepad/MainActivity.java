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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    private float prevX = 0;
    private float prevY = 0;

    private boolean menuOpened = false;

    private LinearLayout layoutMenu;

    private DatagramSocket socket;
    private byte[] buffer = new byte[256];

    private InetAddress cliAddr = null;
    private int cliPort = 0;

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
        waitForClient();
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

        // try to send data
        if (cliAddr != null && cliPort != 0) {
            int X = Math.round(dx * sensitivity);
            int Y = Math.round(dy * sensitivity);

            String data = Integer.toString(X) + ":" + Integer.toString(Y);

            DatagramPacket packet = new DatagramPacket(
                    data.getBytes(),
                    data.getBytes().length,
                    cliAddr,
                    cliPort
            );

            try {
                socket.send(packet);
                Log.d(TAG, "sending data ok: " + data);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "sending data fail: " + data);
            }
        }

        prevX = x;
        prevY = y;
    }

    private void waitForClient() {
        Log.d(TAG, "waiting for client");

        final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        try {

            if (socket != null) {
                socket.disconnect();
                socket.close();
                socket = null;
            }

            socket = new DatagramSocket(8000);

            new Thread(() -> {
                try {
                    socket.receive(packet);
                    runOnUiThread(() -> onClientAvailable(
                            packet.getAddress(),
                            packet.getPort()
                    ));
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> onListeningFailed());
                }
            }).start();
        } catch (SocketException e) {
            e.printStackTrace();
            runOnUiThread(() -> onListeningFailed());
        }
    }

    private void onClientAvailable(InetAddress addr, int port) {

        Toast.makeText(this, "client connected", Toast.LENGTH_SHORT).show();

        cliAddr = addr;
        cliPort = port;

        Log.d(TAG, "client connected");
    }

    private void onListeningFailed() {
        Log.d(TAG, "listening for client failed");
    }
}
