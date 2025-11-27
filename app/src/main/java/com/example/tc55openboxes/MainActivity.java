package com.example.tc55openboxes;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private TextView scanResultTextView;
    private EditText manualInputEditText;
    private Button scanButton;
    private Button submitButton;

    private BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.symbol.emdk.scanner.ACTION_SCAN")) {
                byte[] data = intent.getByteArrayExtra("com.symbol.emdk.datawedge.data");
                if (data != null) {
                    String decodedData = new String(data);
                    scanResultTextView.setText(decodedData);
                    Toast.makeText(context, "Scanned: " + decodedData, Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanResultTextView = findViewById(R.id.scanResultTextView);
        manualInputEditText = findViewById(R.id.manualInputEditText);
        scanButton = findViewById(R.id.scanButton);
        submitButton = findViewById(R.id.submitButton);

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Trigger hardware scan button (implementation depends on scanner type)
                Toast.makeText(MainActivity.this, "Press hardware scan trigger", Toast.LENGTH_SHORT).show();
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String manualText = manualInputEditText.getText().toString();
                if (!manualText.isEmpty()) {
                    scanResultTextView.setText(manualText);
                    Toast.makeText(MainActivity.this, "Manual input: " + manualText, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Please enter barcode manually", Toast.LENGTH_SHORT).show();
                }
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.symbol.emdk.scanner.ACTION_SCAN");
        registerReceiver(scanReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(scanReceiver);
    }
}
