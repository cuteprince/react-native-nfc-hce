package studio.bb.rnlib;

import android.nfc.NdefRecord;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;

import studio.bb.rnlib.utils.ByteUtil;

/**
 * Created by justin.ribeiro on 10/27/2014.
 * Modified by vishnu.das@bovlabs.com on 13/08/2020
 *
 * The following definitions are based on two things:
 *   1. NFC Forum Type 4 Tag Operation Technical Specification, version 3.0 2014-07-30
 *   2. APDU example in libnfc: http://nfc-tools.org/index.php?title=Libnfc:APDU_example
 *
 */
public class CardService extends HostApduService {

    private static final String TAG = "CardService";

    private static final byte[] APDU_SELECT_HEADER = {
        (byte)0x00, // CLA	- Class - Class of instruction
        (byte)0xA4, // INS	- Instruction - Instruction code
        (byte)0x04, // P1	- Parameter 1 - Instruction parameter 1
        (byte)0x00 // P2	- Parameter 2 - Instruction parameter 2
    };

    private static final byte[] CAPABILITY_CONTAINER = {
            (byte)0x00, // CLA	- Class - Class of instruction
            (byte)0xa4, // INS	- Instruction - Instruction code
            (byte)0x00, // P1	- Parameter 1 - Instruction parameter 1
            (byte)0x0c, // P2	- Parameter 2 - Instruction parameter 2
            (byte)0x02, // Lc field	- Number of bytes present in the data field of the command
            (byte)0xe1, (byte)0x03 // file identifier of the CC file
    };

    private static final byte[] READ_CAPABILITY_CONTAINER = {
            (byte)0x00, // CLA	- Class - Class of instruction
            (byte)0xb0, // INS	- Instruction - Instruction code
            (byte)0x00, // P1	- Parameter 1 - Instruction parameter 1
            (byte)0x00, // P2	- Parameter 2 - Instruction parameter 2
            (byte)0x0f  // Le field	- Number of bytes expected in the data field of the response
    };

    // In the scenario that we have done a CC read, the same byte[] match
    // for ReadBinary would trigger and we don't want that in succession
    private boolean READ_CAPABILITY_CONTAINER_CHECK = false;

    private static final byte[] READ_CAPABILITY_CONTAINER_RESPONSE = {
            (byte)0x00, (byte)0x0F, // CCLEN length of the CC file
            (byte)0x20, // Mapping Version 2.0
            (byte)0x00, (byte)0x3B, // MLe maximum 59 bytes R-APDU data size
            (byte)0x00, (byte)0x34, // MLc maximum 52 bytes C-APDU data size
            (byte)0x04, // T field of the NDEF File Control TLV
            (byte)0x06, // L field of the NDEF File Control TLV
            (byte)0xE1, (byte)0x04, // File Identifier of NDEF file
            (byte)0x00, (byte)0x32, // Maximum NDEF file size of 50 bytes
            (byte)0x00, // Read access without any security
            (byte)0x00, // Write access without any security
            (byte)0x90, (byte)0x00 // A_OKAY
    };

    private static final byte[] NDEF_SELECT = {
            (byte)0x00, // CLA	- Class - Class of instruction
            (byte)0xa4, // Instruction byte (INS) for Select command
            (byte)0x00, // Parameter byte (P1), select by identifier
            (byte)0x0c, // Parameter byte (P1), select by identifier
            (byte)0x02, // Lc field	- Number of bytes present in the data field of the command
            (byte)0xE1, (byte)0x04 // file identifier of the NDEF file retrieved from the CC file
    };

    private static final byte[] NDEF_READ_BINARY_NLEN = {
            (byte)0x00, // Class byte (CLA)
            (byte)0xb0, // Instruction byte (INS) for ReadBinary command
            (byte)0x00, (byte)0x00, // Parameter byte (P1, P2), offset inside the CC file
            (byte)0x02  // Le field
    };

    private static final byte[] NDEF_READ_BINARY_GET_NDEF = {
            (byte)0x00, // Class byte (CLA)
            (byte)0xb0, // Instruction byte (INS) for ReadBinary command
            (byte)0x00, (byte)0x00, // Parameter byte (P1, P2), offset inside the CC file
            (byte)0x0f  //  Le field
    };

    private static final byte[] A_OKAY = {
            (byte)0x90,  // SW1	Status byte 1 - Command processing status
            (byte)0x00   // SW2	Status byte 2 - Command processing qualifier
    };

    private static final byte[] NDEF_ID = {
            (byte)0xE1,
            (byte)0x04
    };

    private NdefRecord NDEF_URI = new NdefRecord(
            NdefRecord.TNF_WELL_KNOWN,
            NdefRecord.RTD_TEXT,
            NDEF_ID,
            "Hello world!".getBytes(Charset.forName("UTF-8"))
    );
    private byte[] NDEF_URI_BYTES = NDEF_URI.toByteArray();
    private byte[] NDEF_URI_LEN = BigInteger.valueOf(NDEF_URI_BYTES.length).toByteArray();

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {

        //
        // The following flow is based on Appendix E "Example of Mapping Version 2.0 Command Flow"
        // in the NFC Forum specification
        //
        Log.i(TAG, "processCommandApdu() | incoming commandApdu: " + ByteUtil.bytesToHex(commandApdu));

        //
        // First command: NDEF Tag Application select (Section 5.5.2 in NFC Forum spec)
        // For ByteUtil.startsWith method first argument is the source byte array i.e.,
        // commandApdu and second argument is the match i.e., APDU_SELECT_HEADER
        //
        if (ByteUtil.startsWith(commandApdu, APDU_SELECT_HEADER)) {
            Log.i(TAG, "APDU_SELECT triggered. Our Response: " + ByteUtil.bytesToHex(A_OKAY));
            return A_OKAY;
        }

        //
        // Second command: Capability Container select (Section 5.5.3 in NFC Forum spec)
        //
        if (ByteUtil.equals(CAPABILITY_CONTAINER, commandApdu)) {
            Log.i(TAG, "CAPABILITY_CONTAINER triggered. Our Response: " + ByteUtil.bytesToHex(A_OKAY));
            return A_OKAY;
        }

        //
        // Third command: ReadBinary data from CC file (Section 5.5.4 in NFC Forum spec)
        //
        if (ByteUtil.equals(READ_CAPABILITY_CONTAINER, commandApdu) && !READ_CAPABILITY_CONTAINER_CHECK) {
            Log.i(TAG, "READ_CAPABILITY_CONTAINER triggered. Our Response: " + ByteUtil.bytesToHex(READ_CAPABILITY_CONTAINER_RESPONSE));
            READ_CAPABILITY_CONTAINER_CHECK = true;
            return READ_CAPABILITY_CONTAINER_RESPONSE;
        }

        //
        // Fourth command: NDEF Select command (Section 5.5.5 in NFC Forum spec)
        //
        if (ByteUtil.equals(NDEF_SELECT, commandApdu)) {
            Log.i(TAG, "NDEF_SELECT triggered. Our Response: " + ByteUtil.bytesToHex(A_OKAY));
            return A_OKAY;
        }

        // Update NDEF URI using latest NDEF message from SharedPreferences
        String ndefMessage = IDWarehouse.GetID(this.getApplicationContext());
        NDEF_URI = new NdefRecord(
                    NdefRecord.TNF_WELL_KNOWN,
                    NdefRecord.RTD_TEXT,
                    NDEF_ID,
                    ndefMessage.getBytes(Charset.forName("UTF-8"))
            );

        NDEF_URI_BYTES = NDEF_URI.toByteArray();
        NDEF_URI_LEN = BigInteger.valueOf(NDEF_URI_BYTES.length).toByteArray();

        //
        // Fifth command:  ReadBinary, read NLEN field
        //
        if (ByteUtil.equals(NDEF_READ_BINARY_NLEN, commandApdu)) {

            byte[] start = {
                    (byte)0x00
            };

            // Build our response
            byte[] response = new byte[start.length + NDEF_URI_LEN.length + A_OKAY.length];

            System.arraycopy(start, 0, response, 0, start.length);
            System.arraycopy(NDEF_URI_LEN, 0, response, start.length, NDEF_URI_LEN.length);
            System.arraycopy(A_OKAY, 0, response, start.length + NDEF_URI_LEN.length, A_OKAY.length);

            Log.i(TAG, response.toString());
            Log.i(TAG, "NDEF_READ_BINARY_NLEN triggered. Our Response: " + ByteUtil.bytesToHex(response));

            return response;
        }

        //
        // Sixth command: ReadBinary, get NDEF data
        //
        if (ByteUtil.equals(NDEF_READ_BINARY_GET_NDEF, commandApdu)) {
            Log.i(TAG, "processCommandApdu() | NDEF_READ_BINARY_GET_NDEF triggered");

            byte[] start = {
                    (byte)0x00
            };

            // Build our response
            byte[] response = new byte[start.length + NDEF_URI_LEN.length + NDEF_URI_BYTES.length + A_OKAY.length];

            System.arraycopy(start, 0, response, 0, start.length);
            System.arraycopy(NDEF_URI_LEN, 0, response, start.length, NDEF_URI_LEN.length);
            System.arraycopy(NDEF_URI_BYTES, 0, response, start.length + NDEF_URI_LEN.length, NDEF_URI_BYTES.length);
            System.arraycopy(A_OKAY, 0, response, start.length + NDEF_URI_LEN.length + NDEF_URI_BYTES.length, A_OKAY.length);

            Log.i(TAG, NDEF_URI.toString());
            Log.i(TAG, "NDEF_READ_BINARY_GET_NDEF triggered. Our Response: " + ByteUtil.bytesToHex(response));

            READ_CAPABILITY_CONTAINER_CHECK = false;
            return response;
        }

        //
        // We're doing something outside our scope
        //
        Log.wtf(TAG, "processCommandApdu() | I don't know what's going on!!!.");
        return "Can I help you?".getBytes(Charset.forName("UTF-8"));
    }

    @Override
    public void onDeactivated(int reason) {
        Log.i(TAG, "onDeactivated() Fired! Reason: " + reason);
    }
    
}