package studio.bb.rnlib;

import android.content.Context;
import android.nfc.NdefRecord;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.math.BigInteger;
import java.nio.charset.Charset;

import studio.bb.rnlib.utils.ByteUtils;

/**
 * Created by justin.ribeiro on 10/27/2014. 
 * Modified by vishnu.das@bovlabs.com on 13/08/2020
 * Modified by jithin.paul@bovlabs.com on 14/08/2020
 * <p>
 * The following definitions are based on two things: 1. NFC Forum Type 4 Tag
 * Operation Technical Specification, version 3.0 2014-07-30 2. APDU example in
 * libnfc: http://nfc-tools.org/index.php?title=Libnfc:APDU_example
 */
public class CardService extends HostApduService {

    private static final String TAG = "CardService";

    private static final byte[] APDU_SELECT = {
            (byte) 0x00, // CLA - Class - Class of instruction
            (byte) 0xA4, // INS - Instruction - Instruction code
            (byte) 0x04, // P1 - Parameter 1 - Instruction parameter 1
            (byte) 0x00 // P2 - Parameter 2 - Instruction parameter 2
    };

    private static final byte[] CAPABILITY_CONTAINER_OK = {
            (byte) 0x00, // CLA - Class - Class of instruction
            (byte) 0xa4, // INS - Instruction - Instruction code
            (byte) 0x00, // P1 - Parameter 1 - Instruction parameter 1
            (byte) 0x0c, // P2 - Parameter 2 - Instruction parameter 2
            (byte) 0x02, // Lc field - Number of bytes present in the data field of the command
            (byte) 0xe1, (byte) 0x03 // file identifier of the CC file
    };

    private static final byte[] READ_CAPABILITY_CONTAINER = {
            (byte) 0x00, // CLA - Class - Class of instruction
            (byte) 0xb0, // INS - Instruction - Instruction code
            (byte) 0x00, // P1 - Parameter 1 - Instruction parameter 1
            (byte) 0x00, // P2 - Parameter 2 - Instruction parameter 2
            (byte) 0x0f // Le field - Number of bytes expected in the data field of the response
    };

    private static final byte[] READ_CAPABILITY_CONTAINER_RESPONSE = {
            (byte) 0x00, (byte) 0x0F, // CCLEN length of the CC file
            (byte) 0x20, // Mapping Version 2.0
            (byte) 0xFF, (byte) 0xFF, // MLe maximum R-APDU data size allowed
            (byte) 0xFF, (byte) 0xFF, // MLc maximum C-APDU data size allowed
            (byte) 0x04, // T field of the NDEF File Control TLV
            (byte) 0x06, // L field of the NDEF File Control TLV
            (byte) 0xE1, (byte) 0x04, // File Identifier of NDEF file
            (byte) 0xFF, (byte) 0xFE, // Maximum NDEF file size of 65534 bytes
            (byte) 0x00, // Read access without any security
            (byte) 0xFF, // Write access not allowed
            (byte) 0x90, (byte) 0x00 // A_OKAY
    };

    private static final byte[] NDEF_SELECT_OK = {
            (byte) 0x00, // CLA - Class - Class of instruction
            (byte) 0xa4, // Instruction byte (INS) for Select command
            (byte) 0x00, // Parameter byte (P1), select by identifier
            (byte) 0x0c, // Parameter byte (P1), select by identifier
            (byte) 0x02, // Lc field - Number of bytes present in the data field of the command
            (byte) 0xE1, (byte) 0x04 // file identifier of the NDEF file retrieved from the CC file
    };

    private static final byte[] NDEF_READ_BINARY = {
            (byte) 0x00, // Class byte (CLA)
            (byte) 0xb0 // Instruction byte (INS) for ReadBinary command
    };

    private static final byte[] NDEF_READ_BINARY_NLEN = {
            (byte) 0x00, // Class byte (CLA)
            (byte) 0xb0, // Instruction byte (INS) for ReadBinary command
            (byte) 0x00, (byte) 0x00, // Parameter byte (P1, P2), offset inside the CC file
            (byte) 0x02 // Le field
    };

    private static final byte[] A_OKAY = {
            (byte) 0x90, // SW1 Status byte 1 - Command processing status
            (byte) 0x00 // SW2 Status byte 2 - Command processing qualifier
    };

    private static final byte[] A_ERROR = {
            (byte) 0x6A, // SW1 Status byte 1 - Command processing status
            (byte) 0x82 // SW2 Status byte 2 - Command processing qualifier
    };

    private static final byte[] NDEF_ID = {
            (byte) 0xE1, 
            (byte) 0x04
    };

    // In the scenario that we have done a CC read, the same byte[] match
    // for ReadBinary would trigger and we don't want that in succession
    private boolean READ_CAPABILITY_CONTAINER_CHECK = false;

    private NdefRecord NDEF_URI = null;
    private byte[] NDEF_URI_BYTES = null;
    private byte[] NDEF_URI_LEN = null;
    private String idTag = null;

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {

        //
        // The following flow is based on Appendix E "Example of Mapping Version 2.0
        // Command Flow"
        // in the NFC Forum specification
        //
        Log.i(TAG, "processCommandApdu() | incoming commandApdu: " + ByteUtils.bytesToHex(commandApdu));

        //
        // First command: NDEF Tag Application select (Section 5.5.2 in NFC Forum spec)
        // For ByteUtils.startsWith method first argument is the source byte array i.e.,
        // commandApdu and second argument is the match i.e., APDU_SELECT
        //
        if (ByteUtils.startsWith(commandApdu, APDU_SELECT)) {
            Log.i(TAG, "APDU_SELECT triggered. Our Response: " + ByteUtils.bytesToHex(A_OKAY));
            return A_OKAY;
        }

        //
        // Second command: Capability Container select (Section 5.5.3 in NFC Forum spec)
        //
        if (ByteUtils.equals(CAPABILITY_CONTAINER_OK, commandApdu)) {
            Log.i(TAG, "CAPABILITY_CONTAINER_OK triggered. Our Response: " + ByteUtils.bytesToHex(A_OKAY));
            return A_OKAY;
        }

        //
        // Third command: ReadBinary data from CC file (Section 5.5.4 in NFC Forum spec)
        //
        if (ByteUtils.equals(READ_CAPABILITY_CONTAINER, commandApdu) && !READ_CAPABILITY_CONTAINER_CHECK) {
            Log.i(TAG, "READ_CAPABILITY_CONTAINER triggered. Our Response: "
                    + ByteUtils.bytesToHex(READ_CAPABILITY_CONTAINER_RESPONSE));
            READ_CAPABILITY_CONTAINER_CHECK = true;

            if (IDWarehouse.isEmptyID(getApplicationContext())) {
                showToast("No NFC ID Tag has been configured for you. Please contact support");
                Log.wtf(TAG, "processCommandApdu() | No Idtag set for user or retrieved from context!!!");
                return A_ERROR;
            }

            String currentIdTag = IDWarehouse.GetID(getApplicationContext());
            if (!TextUtils.equals(idTag, currentIdTag)) {
                idTag = currentIdTag;
                Log.i(TAG, "idTag reset: " + idTag);
                NDEF_URI = createTextRecord("en", idTag, NDEF_ID);
                NDEF_URI_BYTES = NDEF_URI.toByteArray();
                NDEF_URI_LEN = ByteUtils.fillByteArrayToFixedDimension(BigInteger.valueOf(NDEF_URI_BYTES.length).toByteArray(), 2);
            }

            return READ_CAPABILITY_CONTAINER_RESPONSE;
        }

        //
        // Fourth command: NDEF Select command (Section 5.5.5 in NFC Forum spec)
        //
        if (ByteUtils.equals(NDEF_SELECT_OK, commandApdu)) {
            Log.i(TAG, "NDEF_SELECT_OK triggered. Our Response: " + ByteUtils.bytesToHex(A_OKAY));
            return A_OKAY;
        }

        //
        // Fifth command: ReadBinary, read NLEN field
        //
        if (ByteUtils.equals(NDEF_READ_BINARY_NLEN, commandApdu)) {
            // Build our response
            byte[] response = new byte[NDEF_URI_LEN.length + A_OKAY.length];
            System.arraycopy(NDEF_URI_LEN, 0, response, 0, NDEF_URI_LEN.length);
            System.arraycopy(A_OKAY, 0, response, NDEF_URI_LEN.length, A_OKAY.length);

            Log.i(TAG, "NDEF_READ_BINARY_NLEN triggered. Our Response: " + ByteUtils.bytesToHex(response));
            return response;
        }

        //
        // Sixth command: ReadBinary, get NDEF data
        //
        if (ByteUtils.equals(NDEF_READ_BINARY, ByteUtils.subbytes(commandApdu, 0, 2))) {
            int offset = Integer.parseInt(ByteUtils.bytesToHex(ByteUtils.subbytes(commandApdu, 2, 4)), 16);
            int length = Integer.parseInt(ByteUtils.bytesToHex(ByteUtils.subbytes(commandApdu, 4, 5)), 16);

            // Build our response
            byte[] fullResponse = new byte[NDEF_URI_LEN.length + NDEF_URI_BYTES.length];
            System.arraycopy(NDEF_URI_LEN, 0, fullResponse, 0, NDEF_URI_LEN.length);
            System.arraycopy(NDEF_URI_BYTES, 0, fullResponse, NDEF_URI_LEN.length, NDEF_URI_BYTES.length);

            Log.i(TAG, "NDEF_READ_BINARY triggered");
            Log.d(TAG, "NDEF URI: " + NDEF_URI.toString());
            Log.d(TAG, "NDEF_READ_BINARY - Full bytes: " + ByteUtils.bytesToHex(fullResponse));
            Log.d(TAG, "NDEF_READ_BINARY - Offset: " + offset + ", Length: " + length);

            byte[] slicedResponse = ByteUtils.subbytes(fullResponse, offset, fullResponse.length);
            int realLength = slicedResponse.length <= length ? slicedResponse.length : length;
            byte[] response = new byte[realLength + A_OKAY.length];

            System.arraycopy(slicedResponse, 0, response, 0, realLength);
            System.arraycopy(A_OKAY, 0, response, realLength, A_OKAY.length);

            Log.i(TAG, "Our Response: " + ByteUtils.bytesToHex(response));
            READ_CAPABILITY_CONTAINER_CHECK = false;

            showToast("Your NFC ID Tag has been communicated successfully to charger!");

            return response;
        }

        //
        // We're doing something outside our scope
        //
        Log.wtf(TAG, "processCommandApdu() | Invalid command!");
        return A_ERROR;
    }

    @Override
    public void onDeactivated(int reason) {
        Log.i(TAG, "onDeactivated() Fired! Reason: " + reason);
    }

    public static NdefRecord createTextRecord(String language, String text, byte[] id) {
        byte[] languageBytes;
        byte[] textBytes;
        try {
            languageBytes = language.getBytes(Charset.forName("US-ASCII"));
            textBytes = text.getBytes(Charset.forName("UTF-8"));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
  
        byte[] recordPayload = new byte[1 + (languageBytes.length & 0x03F) + textBytes.length];
        recordPayload[0] = (byte) (languageBytes.length & 0x03F);
        System.arraycopy(languageBytes, 0, recordPayload, 1, languageBytes.length & 0x03F);
        System.arraycopy(textBytes, 0, recordPayload, 1 + (languageBytes.length & 0x03F), textBytes.length);
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, id, recordPayload);
    }

    private void showToast(CharSequence text) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
    }

}