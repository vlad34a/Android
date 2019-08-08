package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Thread Thread1 = null;
    private EditText etIP, etPort;
    private TextView tvMessages;
    private EditText etMessage;
    private Button btnSend;
    private String SERVER_IP = "192.168.100.8";
    private int SERVER_PORT = 12362;
    private List<Thread> threadPool;
    private TextView batteryTxt;

    private BroadcastReceiver mBatteryInfo = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            batteryTxt.setText(String.valueOf(level) + "%");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        batteryTxt = (TextView)this.findViewById(R.id.batteryTxt);
        this.registerReceiver(this.mBatteryInfo, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));


        etIP = findViewById(R.id.etIP);
        etPort = findViewById(R.id.etPort);
        tvMessages = findViewById(R.id.tvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        Button btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvMessages.setText("");
//                SERVER_IP = etIP.getText().toString().trim();
//                SERVER_PORT = Integer.parseInt(etPort.getText().toString().trim());
                Thread1 = new Thread(new Thread1());
                Thread1.start();
            }
        });
        threadPool = new ArrayList<>();
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = etMessage.getText().toString().trim();
                if (!message.isEmpty()) {
                    new Thread(new Thread3(message)).start();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        this.unregisterReceiver(this.mBatteryInfo, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }


    private int getBatteryPercentage(Context context) {

        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);

        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

        float batteryPct = level / (float) scale;

        return (int) (batteryPct * 100);
    }

    private PrintWriter output;
    private BufferedReader input;

    class Thread1 implements Runnable {
        public void run() {
            Socket socket;
            try {
                socket = new Socket(SERVER_IP, SERVER_PORT);
                output = new PrintWriter(socket.getOutputStream());
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Log.v("HERE", "Before runOnUi");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvMessages.setText("Connected\n");
                    }
                });
                Log.v("HERE", "Before thread 2 start");
                new Thread(new Thread2()).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    class Thread2 implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Log.v("HERE", "Waiting msg");
                    final String message = input.readLine();

                    Log.v("HERE", "RECV msg");
                    if (message != null) {
                        Log.v("HERE", "Msg is not null");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvMessages.append("server: " + message + "\n");
                            }
                        });
                        Thread thread = new Thread(new Thread3(message));
                        thread.start();
                        thread.join();
                    }
//                    else {
//                        Log.v("HERE", "Msg is null");
//                        Thread1 = new Thread(new Thread1());
//                        Thread1.start();
//                        return;
//                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    class Thread3 implements Runnable {
        private String message;
        Thread3(String message) {
            this.message = message;
        }
        @Override
        public void run() {
            output.write(message);
            output.flush();
            calculateFactorial(message);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvMessages.append("client: " + message + "\n");
                    etMessage.setText("");
                }
            });
        }

        //cpu intensive
        private void calculateFactorial(String message) {
            int i = 1;
            int factorial = 1;
            int nr = Integer.valueOf(message);
            while( i < nr ){
                factorial *= i;
                i++;
            }
            Log.v("HERE", "" + factorial);


        }
    }
}
