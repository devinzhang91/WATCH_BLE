package com.example.zhangyoujin.myapplication.OPERATION_INTERFACE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.zhangyoujin.myapplication.BLE_PACKAGE.BLEMessageServer;
import com.example.zhangyoujin.myapplication.R;

public class DataDisplayFragment extends Fragment {
    private final static String TAG = DataDisplayFragment.class.getSimpleName();

    private Context mContext;
    private UpdateReceiver mReceiver;

    private TextView tv_adc_val;
    private TextView tv_ttl_time;
    private TextView tv_rssi_val;

    public DataDisplayFragment() {
    }

    // 实现一个 BroadcastReceiver，用于接收指定的 Broadcast
    public class UpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: " + intent);
            int ble_message_type = intent.getIntExtra(BLEMessageServer.BLE_MESSAGE_TYPE, 0);
            int ble_message_data = ByteArray2Int(intent.getByteArrayExtra(BLEMessageServer.BLE_MESSAGE_DATA));
            String str_data = String.valueOf(ble_message_data);
            switch(ble_message_type){
                case BLEMessageServer.BLE_ADC_VAL:
                    tv_adc_val.setText(str_data);
                    break;
                case BLEMessageServer.BLE_TTL_VAL:
                    tv_ttl_time.setText(str_data);
                    break;
                case BLEMessageServer.BLE_RSSI_VAL:
                    tv_rssi_val.setText(str_data);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_data_display, container, false);

        mReceiver = new UpdateReceiver();   //注册广播接收
        mContext = getActivity();

        tv_adc_val = (TextView)rootView.findViewById(R.id.textView_ADC_VAL);
        tv_ttl_time = (TextView)rootView.findViewById(R.id.textView_TTL_TIME);
        tv_rssi_val = (TextView)rootView.findViewById(R.id.textView_RSSI_VAL);

        return rootView;
    }


    @Override
    public void onStart() {
        super.onStart();
        //注册广播接收
        IntentFilter filter = new IntentFilter();
        filter.addAction(BLEMessageServer.BLE_BROADCAST);
        mContext.registerReceiver(mReceiver, filter);

    }

    @Override
    public void onStop() {
        super.onStop();
        mContext.unregisterReceiver(mReceiver);     //注销广播

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private int ByteArray2Int(byte[] data) {
        int resInt = 0;
        byte bLoop;

        for (int i = 0; i < data.length; i++) {
            bLoop = data[i];
            resInt += (bLoop & 0xFF) << (8 * i);
        }
        return resInt;
    }
}
