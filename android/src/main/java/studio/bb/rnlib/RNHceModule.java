package studio.bb.rnlib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ComponentName;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.cardemulation.CardEmulation;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

import studio.bb.rnlib.utils.ArrayUtils;

public class RNHceModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private final ReactApplicationContext reactContext;

    public RNHceModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        reactContext.addLifecycleEventListener(this);
        IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
        this.reactContext.registerReceiver(mReceiver, filter);
    }

    @Override
    public String getName() {
        return "RNHce";
    }

    private WritableMap supportNFC() {
        NfcManager manager = (NfcManager) this.reactContext.getSystemService(this.reactContext.NFC_SERVICE);
        NfcAdapter adapter = manager.getDefaultAdapter();
        WritableMap map = Arguments.createMap();
        if (adapter != null) {
            map.putBoolean("support", true);
            if (adapter.isEnabled()) {
                map.putBoolean("enabled", true);
            } else {
                map.putBoolean("enabled", false);
            }
        } else {
            map.putBoolean("support", false);
            map.putBoolean("enabled", false);
        }

        return map;
    }

    private void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap payload) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, payload);
    }

    @ReactMethod
    public void setCardContent(String content) {
        IDWarehouse.setID(this.reactContext, content);
    }

    @ReactMethod
    public void setSuccessToast(String content) {
        ToastWarehouse.setSuccessToast(this.reactContext, content);
    }

    @ReactMethod
    public void setErrorToast(String content) {
        ToastWarehouse.setErrorToast(this.reactContext, content);
    }

    @ReactMethod
    public void registerAids(ReadableArray aids, Promise promise) {
        try {
            Object[] objectArray = ArrayUtils.toArray(aids);
            String[] stringArray = Arrays.copyOf(objectArray, objectArray.length, String[].class);
            NfcManager manager = (NfcManager) this.reactContext.getSystemService(this.reactContext.NFC_SERVICE);
            NfcAdapter adapter = manager.getDefaultAdapter();
            if (adapter != null) {
                CardEmulation cardEmulation = CardEmulation.getInstance(adapter);
                ComponentName serviceComponent = new ComponentName(this.reactContext, CardService.class); 
                List<String> dynamicAIDs = Arrays.asList(stringArray);
                boolean aidsRegistered = cardEmulation.registerAidsForService(serviceComponent, "other", dynamicAIDs);
                promise.resolve(aidsRegistered);
            } else {
                throw new IllegalStateException("No NFC adapted found");
            }
        } catch (Exception e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void removeAids(Promise promise) {
        try {
            NfcManager manager = (NfcManager) this.reactContext.getSystemService(this.reactContext.NFC_SERVICE);
            NfcAdapter adapter = manager.getDefaultAdapter();
            if (adapter != null) {
                CardEmulation cardEmulation = CardEmulation.getInstance(adapter);
                ComponentName serviceComponent = new ComponentName(this.reactContext, CardService.class); 
                boolean aidsRemoved = cardEmulation.removeAidsForService(serviceComponent, "other");
                promise.resolve(aidsRemoved);
            } else {
                throw new IllegalStateException("No NFC adapted found");
            }
        } catch (Exception e) {
            promise.reject(e);
        }
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("supportNFC", supportNFC());
        return constants;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)) {
                final int state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE,
                        NfcAdapter.STATE_OFF);
                WritableMap payload = Arguments.createMap();
                switch (state) {
                    case NfcAdapter.STATE_OFF:
                        payload.putBoolean("status", false);
                        sendEvent(reactContext, "listenNFCStatus", payload);
                        break;
                    case NfcAdapter.STATE_TURNING_OFF:
                        break;
                    case NfcAdapter.STATE_ON:
                        payload.putBoolean("status", true);
                        sendEvent(reactContext, "listenNFCStatus", payload);
                        break;
                    case NfcAdapter.STATE_TURNING_ON:
                        break;
                }
            }
        }
    };

    @Override
    public void onHostResume() {

    }

    @Override
    public void onHostPause() {

    }

    @Override
    public void onHostDestroy() {
        this.reactContext.unregisterReceiver(mReceiver);
    }

}