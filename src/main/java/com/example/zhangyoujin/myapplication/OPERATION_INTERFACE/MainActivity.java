package com.example.zhangyoujin.myapplication.OPERATION_INTERFACE;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.zhangyoujin.myapplication.BLE_PACKAGE.BLEMessageServer;
import com.example.zhangyoujin.myapplication.R;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = DataDisplayFragment.class.getSimpleName();

    public static BLEMessageServer mBLEMessageServer;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBLEMessageServer = ((BLEMessageServer.LocalBinder) service).getService();
            Log.d(TAG, "on serivce connected");
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBLEMessageServer = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DataDisplayFragment())
                    .commit();
        }
        Intent serviceIntent = new Intent(this, BLEMessageServer.class);    //绑定服务
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);  //解绑服务
    }
}
