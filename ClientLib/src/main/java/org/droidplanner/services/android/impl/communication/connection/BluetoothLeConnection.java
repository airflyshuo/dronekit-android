package org.droidplanner.services.android.impl.communication.connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import org.droidplanner.services.android.impl.core.MAVLink.connection.MavLinkConnectionTypes;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 *  BLE connection
 * @author jason.xie
 */
public class BluetoothLeConnection extends AndroidMavLinkConnection {
    private static final String TAG = "jason";
    private final String mBluetoothAddress;
    protected final Context mContext;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private Bundle mConnectionExtras = null;

    public BluetoothGattCharacteristic mNotifyCharacteristic;
    public final static UUID UUID_NOTIFY = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");

    public BluetoothLeConnection(Context parentContext, String btAddress) {
        super(parentContext);
        mContext = parentContext;
        this.mBluetoothAddress = btAddress;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.i(TAG, "Null adapters");
        }
    }

    @Override
    protected void openConnection(Bundle connectionExtras) throws IOException {
        Log.i(TAG, "Connect");
        mConnectionExtras = connectionExtras;
        connect(mBluetoothAddress);
    }

    byte[] readBuffer = null;
    synchronized int fillOrReadBuffer(byte[] srcOrDst, boolean fillOrRead){
        if(fillOrRead) {
            if (readBuffer == null) {
                readBuffer = new byte[srcOrDst.length];
                System.arraycopy(srcOrDst, 0, readBuffer, 0, srcOrDst.length);
            } else {
                int lastLength = readBuffer.length;
                byte[] tmp = new byte[lastLength];
                System.arraycopy(readBuffer, 0, tmp, 0, lastLength);
                readBuffer = null;
                readBuffer = new byte[lastLength + srcOrDst.length];
                System.arraycopy(tmp, 0, readBuffer, 0, lastLength);
                System.arraycopy(srcOrDst, 0, readBuffer, lastLength, srcOrDst.length);
            }
            return readBuffer.length;
        }else {
            if(readBuffer == null) {
                return -1;
            } else {
                System.arraycopy(readBuffer, 0, srcOrDst, 0, readBuffer.length);
                int len = readBuffer.length;
                readBuffer = null;
                return len;
            }
        }
    }

    @Override
    protected int readDataBlock(byte[] buffer) throws IOException {
//        if(readCharacteristic(mNotifyCharacteristic)) {
//            buffer = mNotifyCharacteristic.getValue();
//            if(buffer != null) {
//                Log.e(TAG, "readDataBlock buffer = " + bytesToHexString(buffer));
//                Log.e(TAG, "readDataBlock buffer.length=" + buffer.length);
//                return buffer.length;
//            }else{
//                return 0;
//            }
//        }else{
//            return 0;
//        }

        int bufferSize = fillOrReadBuffer(buffer, false);
        if(bufferSize > 0) {
            Log.e(TAG, "readDataBlock buffer  =   " + bytesToHexString(buffer, bufferSize));
            Log.e(TAG, "readDataBlock bufferSize=" + bufferSize);
        }
        return bufferSize;

//        byte[] tmp= new byte[]{(byte)254, 0x09, (byte)177, 0x01, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x02, 0x0C, 0x51, 0x00, 0x03, 0x03, (byte)222};
//        System.arraycopy(tmp, 0, buffer, 0, tmp.length);
//        return buffer.length;
    }

    @Override
    protected void sendBuffer(byte[] buffer) throws IOException {
//        writeValue(buffer);
//        Log.e(TAG, "sendBuffer buffer : " + bytesToHexString(buffer));
    }

    @Override
    public int getConnectionType() {
        return MavLinkConnectionTypes.MAVLINK_CONNECTION_BLUETOOTH;
    }

    @Override
    protected void closeConnection() throws IOException {
        disconnect();
        Log.i(TAG, "## BT Closed ##");
    }

    @Override
    protected void loadPreferences() {
    }

    public void findService(List<BluetoothGattService> gattServices) {
        Log.i(TAG, "Count is:" + gattServices.size());
        for (BluetoothGattService gattService : gattServices) {
            Log.i(TAG, gattService.getUuid().toString());
            Log.i(TAG, UUID_SERVICE.toString());
            if (gattService.getUuid().toString().equalsIgnoreCase(UUID_SERVICE.toString())) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService
                        .getCharacteristics();
                Log.i(TAG, "Count is:" + gattCharacteristics.size());
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    if (gattCharacteristic.getUuid().toString().equalsIgnoreCase(UUID_NOTIFY
                            .toString())) {
                        Log.i(TAG, gattCharacteristic.getUuid().toString());
                        Log.i(TAG, UUID_NOTIFY.toString());
                        mNotifyCharacteristic = gattCharacteristic;
                        setCharacteristicNotification(gattCharacteristic, true);
                        onConnectionOpened(mConnectionExtras);
//                        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                        return;
                    }
                }
            }
        }
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(TAG, "oldStatus=" + status + " NewStates=" + newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Connected to GATT server.");
                    // Attempts to discover services after successful connection.
                    Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt
                            .discoverServices());
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    close();
                    Log.i(TAG, "Disconnected from GATT server.");
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "onServicesDiscovered received: " + status);
                findService(gatt.getServices());
            } else {
                if (mBluetoothGatt.getDevice().getUuids() == null) {
                    Log.w(TAG, "onServicesDiscovered received: " + status);
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic, int status) {
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
//            }
            Log.e(TAG, "onCharacteristicRead : "+bytesToHexString(characteristic.getValue()));
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic) {
//            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            Log.e(TAG, "onCharacteristicChanged : "+bytesToHexString(characteristic.getValue()));
            fillOrReadBuffer(characteristic.getValue(), true);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic, int status) {
            Log.e(TAG, "OnCharacteristicWrite");
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor bd, int status) {
            Log.e(TAG, "onDescriptorRead");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor bd, int status) {
            Log.e(TAG, "onDescriptorWrite");
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int a, int b) {
            Log.e(TAG, "onReadRemoteRssi");
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int a) {
            Log.e(TAG, "onReliableWriteCompleted");
        }

    };

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int,
     * int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
/*
        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.i(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
*/
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
        //mBluetoothGatt.connect();

        Log.i(TAG, "Trying to create a new connection.");
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int,
     * int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "disconnect, BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android
     * .bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "readCharacteristic, BluetoothAdapter not initialized");
            return false;
        }
        return mBluetoothGatt.readCharacteristic(characteristic);
    }


    public void writeValue(byte[] value) {
        mNotifyCharacteristic.setValue(value);
        mBluetoothGatt.writeCharacteristic(mNotifyCharacteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean
            enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
/*
        // This is specific to Heart Rate Measurement.
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
        */
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) {
            return null;
        }

        return mBluetoothGatt.getServices();
    }

    /**
     * Convert byte[] to hex string.这里我们可以将byte转换成int，然后利用Integer.toHexString(int)
     *来转换成16进制字符串。
     * @param src byte[] data
     * @return hex string
     */
    public static String bytesToHexString(byte[] src){
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv + " ");
        }
        return stringBuilder.toString();
    }

    public static String bytesToHexString(byte[] src, int size){
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < size; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv + " ");
        }
        return stringBuilder.toString();
    }
}
