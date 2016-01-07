package com.example.zhangyoujin.myapplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zhangyoujin.myapplication.BluetoothLeClass.OnDataAvailableListener;
import com.example.zhangyoujin.myapplication.BluetoothLeClass.OnServiceDiscoverListener;

import java.util.UUID;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends Activity {
	private final static String TAG = DeviceScanActivity.class.getSimpleName();
    //声明BLE设备的通信UUID号 0设备收发共用一个UUID 1设备使用两个
	private final static String UUID_KEY_DATA_0R = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private final static String UUID_KEY_DATA_0T = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private final static String UUID_KEY_DATA_1R = "0000ffe4-0000-1000-8000-00805f9b34fb";
    private final static String UUID_KEY_DATA_1T = "0000ffe9-0000-1000-8000-00805f9b34fb";
    private final static String UUID_KEY_DATA_2R = "0000ffe4-0000-1000-8000-00805f9b34fb";
    private final static String UUID_KEY_DATA_2T = "00002902-0000-1000-8000-00805f9b34fb";
    private static final int BLE_MESSAGE = 8001;
    private static final int BLE_UUID    = 8002;

    private LeDeviceListAdapter mLeDeviceListAdapter;
    /**搜索BLE终端*/
    private BluetoothAdapter mBluetoothAdapter;
    /**读写BLE终端*/
    private BluetoothLeClass mBLE;
    private boolean mScanning;
    private Handler mHandler;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 30000;
    private BluetoothReceiver receiver;

    private ListView lv_devices;
    private TextView tv_heart_rate,tv_msg;

    private Handler handler=new Handler()
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
                case BLE_MESSAGE:
                    int data_rate = Integer.parseInt(new String((byte[]) msg.obj),16);
                    data_rate = (int)(60.000/(0.000256*data_rate + 0.100));
                    tv_heart_rate.setText(String.valueOf(data_rate));
                    break;
            }
        }

    };

    public static int bytesToInt(byte[] src, int offset) {
        int value;
        value = (int) ((src[offset] & 0xFF) | ((src[offset+1] & 0xFF)<<8));
        return value;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        lv_devices = (ListView)findViewById(R.id.listView_devices);
        tv_heart_rate = (TextView)findViewById(R.id.textView_heart_rate);
        tv_msg = (TextView)findViewById(R.id.textView_msg);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        receiver = new BluetoothReceiver();
        registerReceiver(receiver, filter);
        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        //开启蓝牙
        mBluetoothAdapter.enable();
        
        mBLE = new BluetoothLeClass(this);
        if (!mBLE.initialize()) {
            Log.e(TAG, "Unable to initialize Bluetooth");
            finish();
        }
        //发现BLE终端的Service时回调
        mBLE.setOnServiceDiscoverListener(mOnServiceDiscover);
        //收到BLE终端数据交互的事件
        mBLE.setOnDataAvailableListener(mOnDataAvailable);

        /************************* device 列表 **************************/
        lv_devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(arg2);
                if (device == null) return;
                if (mScanning) {
                    mBluetoothAdapter.cancelDiscovery();
                    mScanning = false;
                }
                mBLE.connect(device.getAddress());
                tv_msg.setText("正在链接");
            }
        });

        tv_msg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanLeDevice(true);
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        lv_devices.setAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mBLE.disconnect();
        mBLE.close();
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(!mLeDeviceListAdapter.isEmpty()) {
                        mScanning = false;
                        mBluetoothAdapter.cancelDiscovery();
                        tv_msg.setText("搜索停止,点击重新搜索");
                        invalidateOptionsMenu();
                    }
                }
            }, SCAN_PERIOD);

            tv_msg.setText("正在搜索");
            mScanning = true;
            mBluetoothAdapter.startDiscovery();
        } else {
            mScanning = false;
            tv_msg.setText("搜索停止");
            mBluetoothAdapter.cancelDiscovery();
        }
        invalidateOptionsMenu();
    }

    /**
     * 搜索到BLE终端服务的事件
     */
    private BluetoothLeClass.OnServiceDiscoverListener mOnServiceDiscover = new OnServiceDiscoverListener(){

		@Override
		public void onServiceDiscover(BluetoothGatt gatt) {
            //成功链接上BLE设备
            tv_msg.setText("已成功连接设备");
            mBLE.setCharacteristicNotification(gatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")).getCharacteristic(UUID.fromString("0000ffe4-0000-1000-8000-00805f9b34fb")), true);
		}
    };

    /**
     * 收到BLE终端数据交互的事件
     */
    private BluetoothLeClass.OnDataAvailableListener mOnDataAvailable = new OnDataAvailableListener(){

    	/**
    	 * BLE终端数据被读的事件
    	 */
		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS)
				Log.i(TAG, "onCharRead " + gatt.getDevice().getName()
                        + " read "
                        + characteristic.getUuid().toString()
                        + " -> "
                        + Utils.bytesToHexString(characteristic.getValue()));
		}
		
	    /**
	     * 收到BLE终端写入数据回调
	     */
		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Message message=new Message();
            message.what=BLE_MESSAGE;
            message.obj=characteristic.getValue();
            handler.sendMessage(message);
		}
    };

    private class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("TAG", "onReceive");
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mLeDeviceListAdapter.addDevice(device);
                mLeDeviceListAdapter.notifyDataSetChanged();
            }
        }
    }

}