package studio.bb.rnlib;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class ToastWarehouse {

    private static final String S_TOAST = null;
    private static final String E_TOAST = null;
    private static final String DEFAULT_S_TOAST = "Your NFC ID Tag has been communicated successfully to charger!";
    private static final String DEFAULT_E_TOAST = "esCharge: No NFC ID Tag has been configured for you. Please login first or contact support";
    private static final String TAG = "ToastWarehouse";

    public static void setSuccessToast(Context c, String s) {
        Log.i(TAG, "Setting S_TOAST: " + s);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        prefs.edit().putString("S_TOAST", s).commit();
        S_TOAST = s;
    }

    public static void setErrorToast(Context c, String s) {
        Log.i(TAG, "Setting E_TOAST: " + s);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        prefs.edit().putString("E_TOAST", s).commit();
        E_TOAST = s;
    }

    public static String getSuccessToast(Context c) {
        if(S_TOAST == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
            S_TOAST = prefs.getString("S_TOAST", DEFAULT_S_TOAST);
        }
        return S_TOAST;
    }

    public static String getErrorToast(Context c) {
        if(E_TOAST == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
            E_TOAST = prefs.getString("E_TOAST", DEFAULT_E_TOAST);
        }
        return E_TOAST;
    }
    
}
