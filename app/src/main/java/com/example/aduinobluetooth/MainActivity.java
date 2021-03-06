package com.example.aduinobluetooth;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    EditText edtSendText;
    Button btnSend;
    TextView tvReceive;
    ImageView ivConnect;
    BluetoothAdapter bluetoothAdapter;
    int pairedDeviceCount=0;
    Set<BluetoothDevice> devices;
    BluetoothDevice remoteDevice;
    BluetoothSocket bluetoothSocket;
    OutputStream outputStream=null;
    InputStream inputStream=null;
    Thread workerThread=null;
    String strDelimiter="\n";
    char charDelimiter='\n';
    byte readBuffer[];
    int readBufferPosition;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        edtSendText=findViewById(R.id.edtSendText);
        btnSend=findViewById(R.id.btnSend);
        tvReceive=findViewById(R.id.tvReceive);
        ivConnect=findViewById(R.id.ivConnect);
        ivConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkBluetooth();
            }
        });
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData(edtSendText.getText().toString());
                edtSendText.setText("");
            }
        });
    }//onCreate ????????? ???~~

    //??????????????? ???????????? ?????? ?????? ??????
    void checkBluetooth() {
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter==null) {
            showToast("??????????????? ???????????? ?????? ???????????????.");
        }else {
            //????????? ??????????????? ???????????? ??????
            if(!bluetoothAdapter.isEnabled()) {
                Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, 10);
            }else {
                selectDevice();
            }
        }
    }

    //???????????? ?????? ?????? ?????? ??? ??????
    void selectDevice() {
        devices=bluetoothAdapter.getBondedDevices();
        pairedDeviceCount=devices.size();
        if(pairedDeviceCount==0){
            showToast("???????????? ????????? ????????? ????????????.");
        }else {
            AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("???????????? ?????? ??????");
            List<String> listItems=new ArrayList<String>();
            for(BluetoothDevice device:devices) {
                listItems.add(device.getName());
            }
            listItems.add("??????");
            final CharSequence[] items=listItems.toArray(new CharSequence[listItems.size()]);
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(which==pairedDeviceCount){
                        showToast("????????? ??????????????????.");
                    }else {
                        connectToSelectedDevice(items[which].toString());
                    }
                }
            });
            builder.setCancelable(false);  //?????? ?????? ?????? ????????????
            AlertDialog dlg=builder.create();
            dlg.show();
        }
    }

    //????????? ???????????? ???????????? ??????
    void connectToSelectedDevice(String selectedDeviceName){
        remoteDevice=getDeviceFromBondedList(selectedDeviceName);
        UUID uuid=UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        try{
            bluetoothSocket=remoteDevice.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect(); //????????? ????????? ??????
            ivConnect.setImageResource(R.drawable.bluetooth_icon);
            outputStream=bluetoothSocket.getOutputStream();
            inputStream=bluetoothSocket.getInputStream();
            beginListenForData();
        }catch (Exception e){
            showToast("?????? ????????? ?????? ????????????.");
        }
    }

    //????????? ?????? ?????? ??? ??????
    void beginListenForData() {
        final Handler handler=new Handler();
        readBuffer=new byte[1024]; //????????????
        readBufferPosition=0;  //?????? ??? ?????? ?????? ?????? ??????
        //????????? ?????? ?????????
        workerThread=new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        int bytesAvailable=inputStream.available(); //?????? ????????? ??????
                        if(bytesAvailable > 0) {
                            byte[] packetBytes=new byte[bytesAvailable];
                            inputStream.read(packetBytes);
                            for(int i=0; i<bytesAvailable; i++){
                                byte b=packetBytes[i];
                                if(b==charDelimiter){
                                    byte[] encodeBytes=new byte[readBufferPosition];
                                    System.arraycopy(readBuffer,0,encodeBytes, 0, encodeBytes.length);
                                    final String data=new String(encodeBytes,"US-ASCII");
                                    readBufferPosition=0;
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            //data????????? ????????? ???????????? ?????? ?????? ??????
                                            tvReceive.setText("???????????? : " + data);
                                        }
                                    });
                                }else {
                                    readBuffer[readBufferPosition++]=b;
                                }
                            }
                        }
                    }catch (IOException e) {
                        showToast("????????? ?????? ??? ????????? ??????????????????.");
                    }
                }
            }
        });
        workerThread.start();
    }
    //???????????? ???????????? ????????? ???????????? ??????
    BluetoothDevice getDeviceFromBondedList(String name) {
        BluetoothDevice selectedDevice=null;
        for(BluetoothDevice device: devices){
            if(name.equals(device.getName())){
                selectedDevice=device;
                break;
            }
        }
        return selectedDevice;
    }

    //????????? ??????
    void sendData(String msg){
        msg+=strDelimiter;
        try {
            outputStream.write(msg.getBytes());  //????????? ??????
        }catch (Exception e) {
            showToast("????????? ?????? ????????? ????????? ??????????????????.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            workerThread.interrupt();
            inputStream.close();
            outputStream.close();
            bluetoothSocket.close();
        }catch (Exception e) {
            showToast("??? ?????? ??? ?????? ??????");
        }
    }

    void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 10:
                if(resultCode==RESULT_OK) {
                    selectDevice();
                }else if(resultCode==RESULT_CANCELED){
                    showToast("???????????? ???????????? ??????????????????.");
                }
                break;
        }
    }
}