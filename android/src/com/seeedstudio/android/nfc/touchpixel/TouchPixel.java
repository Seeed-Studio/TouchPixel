package com.seeedstudio.android.nfc.touchpixel;

import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.SVBar;

public class TouchPixel extends Activity {
	private static final String TAG = "Touch Pixel";
	
	private NfcAdapter mNfcAdapter;
	private PendingIntent mPendingIntent;
	private IntentFilter[] mIntentFilters;
	private String[][] mTechList;
	
	private ColorPicker mPicker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.touch_pixel);
		
		SVBar svbar = (SVBar) findViewById(R.id.svbar);	
		mPicker = (ColorPicker) findViewById(R.id.picker);
		mPicker.addSVBar(svbar);
		
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (mNfcAdapter == null) {
			Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG)
					.show();
			return;
		}
		
		mPendingIntent = PendingIntent.getActivity(
			    this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
	    try {
	        ndef.addDataType("*/*");    // all MIME type
	    }
	    catch (MalformedMimeTypeException e) {
	        Log.v(TAG, e.getMessage());
	    } 

	    mIntentFilters = new IntentFilter[] {ndef};
	    
	    String[][] techListsArray = new String[][] { 
	    		new String[] { IsoDep.class.getName() }, 
	    		new String[] { Ndef.class.getName() } };
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.v(TAG, "onResume");
		mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, mIntentFilters, mTechList);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		Log.v(TAG, "onPause");
		mNfcAdapter.disableForegroundDispatch(this);

	}
	
	public void onNewIntent(Intent intent) {
	    Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
	    Ndef ndef = Ndef.get(tag);
	    if (ndef == null) {
	    	Log.v(TAG, "Tag doesnt support NDEF");
	    	return;
	    }
	    
	    if (!ndef.isConnected()) {
	    	try {
	    		ndef.connect();
	    	} catch (IOException e) {
	    		Log.v(TAG, e.getMessage());
	    		return;
	    	}
	    }
	    
	    try {
	    	int color = mPicker.getColor();
			mPicker.setOldCenterColor(color);
			byte[] textBytes = Integer.toHexString(color).getBytes();
			NdefRecord arrRecord = NdefRecord.createExternal("android.com", 
					"pkg", "com.seeedstudio.android.nfc.touchpixel".getBytes());
			NdefRecord textRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
					"text/c".getBytes(), new byte[] {}, textBytes);
			NdefMessage ndefMessage = new NdefMessage(new NdefRecord[] { textRecord, arrRecord });
	    	ndef.writeNdefMessage(ndefMessage);
	    } catch (Exception e) {
//	    	Log.v(TAG, e.getMessage());
	    }
	    
	    try {
	    	ndef.close();
	    } catch (IOException e) {
	    	Log.v(TAG, e.getMessage());
	    }
	}
}
