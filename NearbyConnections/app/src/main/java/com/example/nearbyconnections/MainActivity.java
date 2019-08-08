package com.example.nearbyconnections;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

public class MainActivity extends AppCompatActivity {

    private final static String SERVICE_ID = "Vladut";
    private final static String USER_NICKNAME = "Vladut";
    private EndpointDiscoveryCallback mEndpointDiscoveryCallback;

    private static final String[] REQUIRED_PERMISSIONS =
        new String[] {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        };

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
        }

        startDiscovery();
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /** Handles user acceptance (or denial) of our permission request. */
    @CallSuper
    @Override
    public void onRequestPermissionsResult(
        int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CODE_REQUIRED_PERMISSIONS) {
            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "No permission", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        recreate();
    }

    private void init(Context context) {

        final PayloadCallback mPayloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    // A new payload is being sent over.
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    // Payload progress has updated.
                    Log.v("HERE", "onPayloadTransferUpdate");
                }
            };

        final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    // Automatically accept the connection on both sides.
                    Log.v("HERE", "accept connection");
                    Nearby.getConnectionsClient(context).acceptConnection(endpointId, mPayloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    Log.v("HERE", "on connection result");
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            Log.v("HERE data", "now can send data");
                            for (int i = 0; i < 50; i++) {
                                Payload mPayload = Payload.fromBytes(new byte[]{0xa, 0xb, 0xc, 0xd});
                                Nearby.getConnectionsClient(context).sendPayload(endpointId, mPayload);
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                Log.v("HERE", "sent batch");
                            }
                            // We're connected! Can now start sending and receiving data.
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            // The connection was rejected by one or both sides.
                            break;
                        case ConnectionsStatusCodes.STATUS_ERROR:
                            // The connection broke before it was able to be accepted.
                            break;
                        default:
                            // Unknown status code
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    Log.v("HERE", "on disconnected");
                    // We've been disconnected from this endpoint. No more data can be
                    // sent or received.
                }
            };

        mEndpointDiscoveryCallback = new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
                Log.v("HERE", "onEndpointFound");
                Nearby.getConnectionsClient(context)
                    .requestConnection(getUserNickname(), endpointId, connectionLifecycleCallback)
                        .addOnSuccessListener(
                            (Void unused) -> {
                                // We successfully requested a connection. Now both sides
                                // must accept before the connection is established.
                                Log.v("HERE FOUND", "mesaj");
                            })
                        .addOnFailureListener(
                            (Exception e) -> {
                                // Nearby Connections failed to request the connection.
                                Log.v("HERE FOUND", e.getMessage());
                            });
            }

            @Override
            public void onEndpointLost(String s) {
                Log.v("HERE", "onEndpointLost");
            }
        };
    }

    private String getUserNickname() {
        return USER_NICKNAME;
    }

    private void startDiscovery() {

        init(this);
        DiscoveryOptions discoveryOptions = new DiscoveryOptions(Strategy.P2P_STAR);

        Nearby.getConnectionsClient(this)
            .startDiscovery(SERVICE_ID, mEndpointDiscoveryCallback, discoveryOptions)
            .addOnSuccessListener(
                (Void unused) -> {
                    Log.v("HERE", "success listener");
                })
            .addOnFailureListener(
                (Exception e) -> {
                    Log.v("HERE", e.getMessage());
                });
    }
}
