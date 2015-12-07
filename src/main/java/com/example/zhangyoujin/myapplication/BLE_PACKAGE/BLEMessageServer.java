package com.example.zhangyoujin.myapplication.BLE_PACKAGE;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.example.zhangyoujin.myapplication.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangyoujin on 15/11/23.
 */
public class BLEMessageServer extends Service {

    private final static String TAG = BLEMessageServer.class.getSimpleName();

    public final static String ADDRESS_DEVICE0 = "98:7B:F3:56:A0:B6";
    public final static String ADDRESS_DEVICE4 = "8C:8B:83:47:78:3C";

    public final static String UUID_KEY_ADC_SERVER = "0000ffd0-0000-1000-8000-00805f9b34fb";
    public final static String UUID_KEY_ADC_EN     = "0000ffd1-0000-1000-8000-00805f9b34fb";
    public final static String UUID_KEY_ADC_RATE   = "0000ffd2-0000-1000-8000-00805f9b34fb";
    public final static String UUID_KEY_ADC_VAL1   = "0000ffd4-0000-1000-8000-00805f9b34fb";
    public final static String UUID_KEY_IO_SERVER  = "0000fff0-0000-1000-8000-00805f9b34fb";
    public final static String UUID_KEY_IO_TTL5    = "0000fff9-0000-1000-8000-00805f9b34fb";
    public final static String UUID_KEY_RSSI_SERVER= "0000ffa0-0000-1000-8000-00805f9b34fb";
    public final static String UUID_KEY_RSSI_VAL   = "0000ffa1-0000-1000-8000-00805f9b34fb";
    public final static String UUID_KEY_RSSI_RATE  = "0000ffa2-0000-1000-8000-00805f9b34fb";

    public static final String BLE_BROADCAST = "BLE_BROADCAST";
    public static final String BLE_MESSAGE_TYPE = "BLE_MESSAGE_TYPE";
    public static final String BLE_MESSAGE_DATA = "BLE_MESSAGE_DATA";
    public static final int BLE_ADC_VAL  = 8001;
    public static final int BLE_TTL_VAL  = 8002;
    public static final int BLE_RSSI_VAL = 8003;

    private ArrayList<BluetoothDevice> mLeDevices = new ArrayList<>();

    private BluetoothAdapter mBluetoothAdapter;/**搜索BLE终端*/
    private BluetoothLeClass mBLE;/**读写BLE终端*/
    private boolean mScanning;
    private Handler mHandler;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 20000;
    private BluetoothReceiver receiver;

    public void onCreate() {
        System.out.println("----- onCreate---------");
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        receiver = new BluetoothReceiver();
        registerReceiver(receiver, filter);
        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            stopSelf(); //自销毁
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            stopSelf(); //自销毁
            return;
        }
        //开启蓝牙
        mBluetoothAdapter.enable();

        mBLE = new BluetoothLeClass(this);
        if (!mBLE.initialize()) {
            Log.e(TAG, "Unable to initialize Bluetooth");
            stopSelf(); //自销毁
        }
        //发现BLE终端的Service时回调
        mBLE.setOnServiceDiscoverListener(mOnServiceDiscover);
        //收到BLE终端数据交互的事件
        mBLE.setOnDataAvailableListener(mOnDataAvailable);
        super.onCreate();
    }

    public void onDestroy() {
        super.onDestroy();
        System.out.println("----- onDestroy---------");
        scanLeDevice(false);
        mBLE.disconnect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("----- onStartCommand---------");
        scanLeDevice(true);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        System.out.println("----- onBind---------");
        scanLeDevice(true);
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        System.out.println("----- onUnbind---------");
        scanLeDevice(false);
        mBLE.disconnect();
        mBLE.close();
        return true;
    }

    public class LocalBinder extends Binder {
        public BLEMessageServer getService() {
            return BLEMessageServer.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    private class BluetoothReceiver extends BroadcastReceiver {     //发现蓝牙设备
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.e("TAG", "onReceive");
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.e(TAG, device.getName() + " : " + device.getAddress());
                if(device.getAddress().toString().equals(ADDRESS_DEVICE4)) {
                    mLeDevices.add(device); //加入设备列表
                    Log.e(TAG, "add : " + device.getName() + " : " + device.getAddress());
                    if (mScanning) {
                        mBluetoothAdapter.cancelDiscovery();
                        mScanning = false;
                    }
                    mBLE.connect(device.getAddress());
                }
            }
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.cancelDiscovery();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startDiscovery();
        } else {
            mScanning = false;
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    /**
     * 搜索到BLE终端服务的事件
     */
    private BluetoothLeClass.OnServiceDiscoverListener mOnServiceDiscover = new BluetoothLeClass.OnServiceDiscoverListener(){

        @Override
        public void onServiceDiscover(BluetoothGatt gatt) {
            //成功链接上BLE设备
            setGattServices(mBLE.getSupportedGattServices());
        }
    };

    //收到BLE终端数据交互的事件
    private BluetoothLeClass.OnDataAvailableListener mOnDataAvailable = new BluetoothLeClass.OnDataAvailableListener(){

        //BLE终端数据被读的事件
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onCharRead " + characteristic.getUuid().toString()+ " : "+characteristic.getValue()) ;
                sendMsg(BLE_RSSI_VAL, characteristic.getValue());
                //测试读取当前Characteristic数据，会触发mOnDataAvailable.onCharacteristicRead()
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBLE.readCharacteristic(characteristic);
                    }
                }, 500);
            }
        }

        //收到BLE终端写入数据回调
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.i(TAG, "onCharWirte " + gatt.getDevice().getName());
            if(characteristic.getUuid().toString().equals(UUID_KEY_ADC_VAL1)) { //ADC1 VAL
                sendMsg(BLE_ADC_VAL, characteristic.getValue());
            }
            if(characteristic.getUuid().toString().equals(UUID_KEY_IO_TTL5)) { //TTL5 VAL
                sendMsg(BLE_TTL_VAL, characteristic.getValue());
            }
            if(characteristic.getUuid().toString().equals(UUID_KEY_RSSI_VAL)) { //RSSI VAL
                sendMsg(BLE_RSSI_VAL, characteristic.getValue());
            }
        }
    };

    // 发送广播信息
    private void sendMsg(int type, byte[] data){
        // 指定广播目标的 action （注：指定了此 action 的 receiver 会接收此广播）简单来水这个就是个TAG
        Intent intent = new Intent(BLE_BROADCAST);
        // 需要传递的参数
        intent.putExtra(BLE_MESSAGE_TYPE, type);
        intent.putExtra(BLE_MESSAGE_DATA, data);
        // 发送广播
        this.sendBroadcast(intent);
    }

    private void send2BLEMessage(String message){

        for (BluetoothGattService gattService : mBLE.getSupportedGattServices()) {
            List<BluetoothGattCharacteristic> gattCharacteristics =gattService.getCharacteristics();
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                if (gattCharacteristic.getUuid().toString().equals(UUID_KEY_ADC_EN)) {
                    //设置数据内容
                    gattCharacteristic.setValue(message);
                    //往蓝牙模块写入数据
                    mBLE.writeCharacteristic(gattCharacteristic);
                }
            }
        }
    }

    private void setGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        for (BluetoothGattService gattService : gattServices) {
            if(gattService.getUuid().toString().equals(UUID_KEY_ADC_SERVER)) {  //ADC SERVER
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (final BluetoothGattCharacteristic  gattCharacteristic: gattCharacteristics) {
                    if(gattCharacteristic.getUuid().toString().equals(UUID_KEY_ADC_EN)){    //设置ADC使能
                        gattCharacteristic.setValue(0x02, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                        mBLE.writeCharacteristic(gattCharacteristic);
                    }
                    if(gattCharacteristic.getUuid().toString().equals(UUID_KEY_ADC_RATE)){    //设置ADC采样率
                        gattCharacteristic.setValue(0x07D0, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                        mBLE.writeCharacteristic(gattCharacteristic);
                    }
                    if(gattCharacteristic.getUuid().toString().equals(UUID_KEY_ADC_VAL1)){    //设置ADC Notif
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mBLE.readCharacteristic(gattCharacteristic);
                            }
                        }, 500);
                        //mBLE.setCharacteristicNotification(gattCharacteristic, true);
                    }
                }
            }
            if(gattService.getUuid().toString().equals(UUID_KEY_IO_SERVER)) {  //IO SERVER
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (final BluetoothGattCharacteristic  gattCharacteristic: gattCharacteristics) {
                    if(gattCharacteristic.getUuid().toString().equals(UUID_KEY_IO_TTL5)){    //设置IO5 Notif
                        mBLE.setCharacteristicNotification(gattCharacteristic, true);
                    }
                }
            }
            if(gattService.getUuid().toString().equals(UUID_KEY_RSSI_SERVER)) {  //RSSI SERVER
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (final BluetoothGattCharacteristic  gattCharacteristic: gattCharacteristics) {
                    if(gattCharacteristic.getUuid().toString().equals(UUID_KEY_RSSI_VAL)){    //设置RSSI Notif

                        //测试读取当前Characteristic数据，会触发mOnDataAvailable.onCharacteristicRead()
//                        mHandler.postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                mBLE.readCharacteristic(gattCharacteristic);
//                            }
//                        }, 500);
//                        mBLE.setCharacteristicNotification(gattCharacteristic, true);
                    }
                    if(gattCharacteristic.getUuid().toString().equals(UUID_KEY_RSSI_RATE)){    //设置RSSI通知时间
                        gattCharacteristic.setValue(0x01F4, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                        mBLE.writeCharacteristic(gattCharacteristic);
                    }
                }
            }
        }
    }
}
