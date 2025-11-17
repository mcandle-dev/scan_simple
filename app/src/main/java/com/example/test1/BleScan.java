package com.example.test1;

import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import vpos.apipackage.At;

public class BleScan {
    private boolean isScanning = false;
    private static final String TAG = "BLEScan";
    private boolean isMaster;

    // ✅ 인터페이스를 BLEScan 클래스 내부에 정의
    public interface ScanResultListener {
        void onScanResult(JSONArray scanData);
    }

    // 콜백 인터페이스 정의
    public interface DataReceiveListener {
        void onDataReceived(String buff);
    }

    private DataReceiveListener dataReceiveListener;

    // 콜백 등록 메서드
    public void setDataReceiveListener(DataReceiveListener listener) {
        this.dataReceiveListener = listener;
    }

    public int enableMasterMode(boolean enable) {
        if (isMaster == enable) {
            Log.d("BLE_MANAGER", "Already in the requested mode. No changes made.");
            return 0; // ✅ 동일한 상태면 변경 없음
        } else {
            int ret = At.Lib_EnableMaster(enable);
            if (!enable) {
                isMaster = true; // false에서 true로 변경하여 이후 호출 방지
            } else {
                isMaster = enable;
            }
            Log.d("BLE_MANAGER", "Master mode updated successfully, Result: " + ret);
            return ret; // ✅ 변경 적용
        }
    }

    public String getDeviceMacAddress() {
        String[] macAddress = new String[1];
        int ret = At.Lib_GetAtMac(macAddress);
        return (ret == 0) ? macAddress[0] : null;
    }

    public int startNewScan(String macAddress,
                            String broadcastName,
                            int rssi,
                            String manufacturerId,
                            String data) {
        int ret = At.Lib_AtStartNewScan(macAddress, broadcastName, -rssi, manufacturerId, data);

        Log.e("BLE_SCAN", "BLE Scan Started with result: " + ret);
        return ret;
    }

    public void startScanAsync(SharedPreferences sp, ScanResultListener listener) {
        if (isScanning) return;
        isScanning = true;

        Log.e(TAG, "Starting BLE scan async...");

        int ret = At.Lib_EnableMaster(true);
        if (ret != 0) {
            Log.e(TAG, "Failed to enable master mode: " + ret);
            isScanning = false;
            return;
        }

        ret = At.Lib_AtStartNewScan(
                sp.getString("macAddress", ""),
                sp.getString("broadcastName", ""),
                -Integer.parseInt(sp.getString("rssi", "0")),
                sp.getString("manufacturerId", ""),
                sp.getString("data", "")
        );

        if (ret == 0) {
            recvScanData(listener);  // Run in current Coroutine thread
        } else {
            isScanning = false;
            Log.e(TAG, "Failed to start BLE scan");
        }
    }

    public void stopScan() {
        if (!isScanning) return;
        isScanning = false;
        At.Lib_AtStopScan();
        Log.e(TAG, "BLE scan stopped");
    }

    public void ComRecvAT() {  // 테스트 용도
        byte[] recvData = new byte[2048];
        int[] recvDataLen = new int[2];

        Log.e("TAG", "ComRecvAT 시작");

        isScanning = true;
        while (isScanning) {

            int ret = At.Lib_ComRecvAT(recvData, recvDataLen, 20, 1000);
            Log.e("TAG", "runLib_ComRecvAT: recvDataLen " + recvDataLen[0]);
            Log.e("TAG", "Lib_ComRecvAT recvData: " + bytesToHex(recvData, recvDataLen[0]));
            String buff= new String(recvData, 0, recvDataLen[0]);

            // 콜백이 등록되어 있으면 MainActivity로 데이터 전달
            if (dataReceiveListener != null) {
                dataReceiveListener.onDataReceived(buff);
            }

            try {
                Thread.sleep(2000); // 2초 대기
            } catch (InterruptedException e) {
                Log.e("TAG", "Sleep interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }
    public void recvScanData(ScanResultListener listener) {
        byte[] recvData =new byte[2048];
        int[] recvDataLen =new int[2];
        String lineLeft="";


        while (isScanning) {
            int ret = At.Lib_ComRecvAT(recvData, recvDataLen, 20, 1000);
            Log.e("TAG", "runLib_ComRecvAT: recvDataLen"+recvDataLen[0] );
            Log.e("TAG", "Lib_ComRecvAT recvData: "+bytesToHex(recvData,recvDataLen[0]));
            Map<String, JSONObject> deviceMap = new ConcurrentHashMap<>();
            boolean startProcessing = false;

            // String buff= lineLeft+new String(recvData);
            String buff= lineLeft+new String(recvData, 0, recvDataLen[0]);
            // String []data=buff.split("\r\n|\r|\n");
            String []data=buff.split("\\r\\n|\\r|\\n", -1); // ����λ�������ַ���
            //Log.e("TAG", "debug crash position:echo21" );
            int lineCount=data.length;
            // if(lineCount>0)//each time response data left last line ,for maybe data not recv all.
            //     lineLeft = data[lineCount-1];
            // else
            //     lineLeft="";
            // �������һ��δ�������
            lineLeft = (data.length > 0) ? data[data.length-1] : "";
            //for (String line : data)
            for (int i=0;i<lineCount-1;i++)
            {
                String line =data[i];
//                    Log.e("TAG", "debug crash position:echo22" );
                if (line.startsWith("MAC:")) {
                    startProcessing = true;
                    String[] parts = line.split(",",3);
                    if(parts.length < 3) {
                        continue;
                    }

                    String mac = parts[0].split(":",2)[1].trim();
                    String rssi = parts[1].split(":")[1].trim();
                    int irssi =0;
                    try {
                        irssi = Integer.parseInt(rssi); // ��֤ RSSI �Ƿ�Ϊ��Ч����
                    } catch (NumberFormatException e) {
                        Log.e("TAG", "Invalid RSSI value: " + rssi);
                        continue;
                    }
                    String payload = parts[2].split(":",2)[1].trim();
                    if((payload.length()>62)||(payload.length()%2!=0))
                        continue;
//                        Log.e("TAG", "debug crash position:echo20" );
                    JSONObject device;
                    if (deviceMap.containsKey(mac)) {
                        device = deviceMap.get(mac);
                    } else {
                        device = new JSONObject();
                        try {
                            device.put("MAC", mac);
                        } catch (JSONException e) {
                            Log.e("TAG", "Handler runLib_ComRecvAT mac 0000: JSONException"+e );
                            //throw new RuntimeException(e);
                            continue;
                        }
                        deviceMap.put(mac, device);
                    }
//                        Log.e("TAG", "debug crash position:echo19" );
                    if (parts[2].startsWith("RSP")) {

                        try {
                            assert device != null;
                            device.put("RSP_org", payload);
                            device.put("RSP", parseAdvertisementData(hexStringToByteArray(payload)));
                        } catch (JSONException e) {
                            Log.e("TAG", "Runnable 444: JSONException"+e );
//                                throw new RuntimeException(e);
                            continue;
                        }

                    } else if (parts[2].startsWith("ADV")) {
                        //device.put("ADV", parsePayload(payload));
                        try {
                            assert device != null;
                            device.put("ADV_org", payload);
                            device.put("ADV", parseAdvertisementData(hexStringToByteArray(payload)));
                        } catch (JSONException e) {
                            Log.e("TAG", "Runnable 333: JSONException"+e );
//                                throw new RuntimeException(e);
                            continue;
                        }
                    }
                    //Log.e("TAG", "debug crash position:echo18" );
                    try {
                        assert device != null;
                        // Log.e("TAG", "debug crash position:echo18"+rssi );
                        device.put("RSSI", irssi);
                    } catch (JSONException e) {
                        Log.e("TAG", "Runnable 222: JSONException"+e.getMessage() );
//                            throw new RuntimeException(e);
                        continue;
                    }
//                        Log.e("TAG", "debug crash position:echo17" );
                    // ���ʱ����ֶ�
                    try {
//                                long curr_time=System.currentTimeMillis();
                        device.put("Timestamp", System.currentTimeMillis());
                    } catch (JSONException e) {
                        //Log.e("TAG", "Runnable 000: JSONException"+e );
//                            throw new RuntimeException(e);
                        continue;
                    }
//                        Log.e("TAG", "debug crash position:echo16" );
                } else if (startProcessing) {
                    // ����Ѿ���ʼ����MAC���ݣ���������MAC��ͷ�����ݣ�������
                    continue;
                }

                synchronized (deviceMap) {
                    Map<String, JSONObject> snapshot = new HashMap<>(deviceMap); // 복사본 생성
                    JSONArray resultArray = new JSONArray(snapshot.values());
                    if (listener != null) {
                        listener.onScanResult(resultArray);
                    }
                }
            }
        }
    }
    private static byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        if(len%2==1)
            len--;
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i+1), 16));
        }
        return data;
    }
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }
    private static String bytesToHex(byte[] bytes,int len) {
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<len;i++) {
            sb.append(String.format("%02X", bytes[i]));
        }
        return sb.toString();
    }
    public static JSONObject parseAdvertisementData(byte[] advertisementData) throws JSONException {
//        Map<String, String> parsedData = new HashMap<>();
//        byte[] advertisementData =new byte[advertiseData.length()/2];
        JSONObject parsedData = new JSONObject();
        int offset = 0;
        while (offset < advertisementData.length) {
            int length = advertisementData[offset++] & 0xFF;
            if (length == 0) break;

            int type = advertisementData[offset] & 0xFF;
            offset++;

            byte[] data = new byte[length - 1];
            if(length-1>advertisementData.length-offset)//data format issue.
            {
                return null;
            }
            System.arraycopy(advertisementData, offset, data, 0, length - 1);
            offset += length - 1;

            switch (type) {
                case 0x01: // Flags
                    parsedData.put("Flags", bytesToHex(data));
                    break;
                case 0x02: // Incomplete List of 16-bit Service Class UUIDs
                case 0x03: // Complete List of 16-bit Service Class UUIDs
                    parsedData.put("Service UUIDs", bytesToHex(data));
                    break;
                case 0x04: // Incomplete List of 32-bit Service Class UUIDs
                case 0x05: // Complete List of 32-bit Service Class UUIDs
                    parsedData.put("Service UUIDs", bytesToHex(data));
                    break;
                case 0x06: // Incomplete List of 128-bit Service Class UUIDs
                case 0x07: // Complete List of 128-bit Service Class UUIDs
                    parsedData.put("Service UUIDs", bytesToHex(data));
                    break;
                case 0x08: // Shortened Local Name
                case 0x09: // Complete Local Name
                    parsedData.put("Device Name", new String(data));
                    break;
                case 0x0A: // Complete Local Name
//                    byte [] tx_power=hexStringToByteArray(new String(data));
                    parsedData.put("TX Power Level", data[0]);
                    break;
                case 0xFF: // Manufacturer Specific Data
                    parsedData.put("Manufacturer Data", bytesToHex(data));
                    break;

                case 0x16: // Service Data - 16-bit UUID
                    if (data.length >= 2) {
                        String uuid16 = String.format("%04X", ((data[1] & 0xFF) << 8) | (data[0] & 0xFF));
                        byte[] serviceData = Arrays.copyOfRange(data, 2, data.length);
                        addServiceData(parsedData, uuid16, serviceData);
                    }
                    break;
                case 0x20: // Service Data - 32-bit UUID
                    if (data.length >= 4) {
                        String uuid32 = String.format("%08X",
                                ((data[3] & 0xFF) << 24) | ((data[2] & 0xFF) << 16) | ((data[1] & 0xFF) << 8) | (data[0] & 0xFF));
                        byte[] serviceData = Arrays.copyOfRange(data, 4, data.length);
                        addServiceData(parsedData, uuid32, serviceData);
                    }
                    break;
                case 0x21: // Service Data - 128-bit UUID
                    if (data.length >= 16) {
                        String uuid128 = bytesToHex(Arrays.copyOfRange(data, 0, 16));
                        byte[] serviceData = Arrays.copyOfRange(data, 16, data.length);
                        addServiceData(parsedData, uuid128, serviceData);
                    }

                default:
                    parsedData.put("Unknown Data (" + type + ")", bytesToHex(data));
                    break;
            }
        }

        return parsedData;
    }

    private static void addServiceData(JSONObject parsedData, String uuid, byte[] serviceData) throws JSONException {
        String key = "Service Data UUID " + uuid;
        JSONArray serviceArray;

        if (parsedData.has(key)) {
            serviceArray = parsedData.getJSONArray(key);
        } else {
            serviceArray = new JSONArray();
            parsedData.put(key, serviceArray);
        }

        serviceArray.put(bytesToHex(serviceData));
    }
}

