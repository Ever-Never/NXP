package com.nxp.nxpwalletconndev.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nxp.nxpwalletconndev.R;
import com.nxp.nxpwalletconndev.databases.MyDbHelper;
import com.nxp.nxpwalletconndev.listeners.OnCloseListener;
import com.nxp.nxpwalletconndev.listeners.OnOperationListener;
import com.nxp.nxpwalletconndev.storage.MyPreferences;
import com.nxp.nxpwalletconndev.utils.Parsers;
import com.nxp.ssdp.btclient.BluetoothClient;
import com.nxp.ssdp.btclient.BluetoothClient.OnBluetoothConnectListener;
import com.nxp.ssdp.btclient.BluetoothClient.OnBluetoothConnectionPendingListener;
import com.nxp.ssdp.btclient.BluetoothClient.OnBluetoothReadListener;
import com.nxp.ssdp.btclient.BluetoothClient.OnBluetoothWriteListener;
import com.nxp.ssdp.btclient.BluetoothDiscovery;
import com.quintic.libqpp.QppApi;

public class BaseActivity extends Activity implements OnBluetoothConnectListener, OnBluetoothConnectionPendingListener, OnBluetoothReadListener, OnBluetoothWriteListener  {
	public final static String TAG = "NXPWallet BaseActivity";
	
	public final static boolean warningInactivity = false;
	
	//To select JCOP Version 3.3 (true) or 3.1 (false)
	public static boolean versionJCOP3dot3 = true;
	public static boolean forAmotech = true;
	
	// Timer to wait for the ACK when the whole TLV has been transmitted
	public static final int TIMER_BLE_ACK_RX = 6000;
	
	// Timer to wait for an operation to be completed (operation specific)
	public static int TIMER_BLE_OP;
	
	public static final int REQUEST_ENABLE_BT = 0x100;
	public static final int REQUEST_CONNECT_DEVICE = 0x101;
	
	private static AsyncTask<?, ?, ?> runningTask;

	// Reference to menu item for icon changing
	private MenuItem mBtMenu;
	
	private static OnOperationListener mDelegateOperation;
	private static OnCloseListener mCloseListener;
	
	// Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter;
	
	// Make the Bluetooth Client static so that all activities can call it
	private static BluetoothClient mBluetooth;
	
	private MyDbHelper mMyDbHelper;
	
	// Buffer where I get all the response script
	private byte[] mBufferDataCmd = null;
	
	private int mBufferOffset = 0;
	private int mExpectedSize = 0;
	
	// Values of the connected device
	private static String mAddress;
	private static String mName;
	private static int mId;
	
	public static boolean showDiscoveryDialog;
	public static boolean showDisconnectionDialog;
	
	// Countdown used to wait for the ACK when the whole TLV is transmitted
	private static CountDownTimer counterAck;
	
	// Countdown used to wait for an operation to be completed (operation specific)
	private static CountDownTimer counterOp;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Setup the Bluetotoh adapter
 		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
 		if (mBluetoothAdapter == null) {
 		    showBluetoothNotSupportedDialog();
 		}
 		
 		if (!mBluetoothAdapter.isEnabled()) {
 		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
 		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
 		} else {
 			if(mBluetooth == null)
 				mBluetooth = new BluetoothClient(mBluetoothAdapter, getApplicationContext(), BaseActivity.this);
	 				
	 		// Prepare the delegates for the callback interfaces
 			mBluetooth.mDelegateConnectionPending = this;
 			mBluetooth.mDelegateConnected = this;
	        mBluetooth.mDelegateRead = this;
	        mBluetooth.mDelegateWrite = this;
	        
	        // Launch the DeviceDiscoveryActivity to see devices and do scan
	        if(mBluetooth.isConnected() == false && showDiscoveryDialog == true) {
		        Intent serverIntent = new Intent(getApplicationContext(), BluetoothDiscovery.class);
		        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
		        
		        // Don't show the dialog anymore
		        showDiscoveryDialog = false;
	        }
 		} 
 		
 		// Get database helper
 		mMyDbHelper = new MyDbHelper(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// Let the user know about the device
//		if(mDelegateDeviceInfo != null)
//        	mDelegateDeviceInfo.onShowDeviceInfo(mAddress);
		
		showDeviceInfo();
		
		// Let the user know about the connection status
		if(mBtMenu != null) {
			if(mBluetoothAdapter.isEnabled())
			{	
				if(mBluetooth != null && mBluetooth.isConnected() && mBluetoothAdapter.isEnabled())
					mBtMenu.setIcon(getResources().getDrawable(R.drawable.ic_action_bluetooth_connected));
				else 
				    mBtMenu.setIcon(getResources().getDrawable(R.drawable.ic_action_bluetooth));
			}
			else 
			    mBtMenu.setIcon(getResources().getDrawable(R.drawable.ic_action_bluetooth));
		}
		
		// Prepare the delegates for the callback interfaces
		if(mBluetooth != null) {
			mBluetooth.mDelegateConnectionPending = this;
			mBluetooth.mDelegateConnected = this;
	        mBluetooth.mDelegateRead = this;
	        mBluetooth.mDelegateWrite = this;
		}
	}
	
	@Override
	protected void onDestroy() {	
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		
		// Get the reference to the menu item
		mBtMenu = menu.findItem(R.id.action_bluetooth);
		
		// Let the user know about the connection status
		if(mBluetooth != null && mBluetooth.isConnected())
			mBtMenu.setIcon(getResources().getDrawable(R.drawable.ic_action_bluetooth_connected));
		else 
	    	mBtMenu.setIcon(getResources().getDrawable(R.drawable.ic_action_bluetooth));
	    	
		return true;
	}
  
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
	    switch(item.getItemId()){
	    case R.id.action_bluetooth:
	    	if (!mBluetoothAdapter.isEnabled()) {
	 		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	 		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
	 		} else {
		    	if(mBluetooth != null && mBluetooth.isConnected() == false) {
			    	// Launch the DeviceDiscoveryActivity to see devices and do scan
			        Intent serverIntent = new Intent(getApplicationContext(), BluetoothDiscovery.class);
			        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
		    	} else {
		    		showDisconnectionDialog();
		    	}
	 		}
	    	
	        return true;
	    case R.id.action_about:
	        int id = item.getItemId();
			if (id == R.id.action_about) {
				Intent i = new Intent(getApplicationContext(), AboutActivity.class);
				i.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
				startActivity(i);
				
				return true;
			}
	    }
	    
	    return false;
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
        	if(resultCode == Activity.RESULT_OK) {
        		if(mBluetooth == null)
        			mBluetooth = new BluetoothClient(mBluetoothAdapter, getApplicationContext(), BaseActivity.this);
 				
    	 		// Prepare the delegates for the callback interfaces
        		mBluetooth.mDelegateConnectionPending = this;
        		mBluetooth.mDelegateConnected = this;
    	        mBluetooth.mDelegateRead = this;
    	        mBluetooth.mDelegateWrite = this;
    	        
    	        // Launch the DeviceDiscoveryActivity to see devices and do scan
    	        if(mBluetooth.isConnected() == false && showDiscoveryDialog == true) {
	    	        Intent serverIntent = new Intent(getApplicationContext(), BluetoothDiscovery.class);
	    	        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	    	        
	    	        // Don't show the dialog anymore
			        showDiscoveryDialog = false;
    	        }
        	} else {
        		Toast.makeText(BaseActivity.this, "Error enabling Bluetooth", Toast.LENGTH_LONG).show();
//        		finish();
        	}
        } else if (requestCode == REQUEST_CONNECT_DEVICE && resultCode == Activity.RESULT_OK) { 
            String address = data.getExtras().getString(BluetoothDiscovery.EXTRA_DEVICE_ADDRESS);
            String name = data.getExtras().getString(BluetoothDiscovery.EXTRA_DEVICE_NAME);
            
            initConnection(address, name);
        }
	}
	
	public void initConnection(String address, String name) {
		 mAddress = address;
		 mName = name;
		 
		 Log.d(TAG, "Launch new connection " + mAddress);
		
		 if(mBluetooth != null)
			 mBluetooth.connect(mAddress);
	}
	
	private void showBluetoothNotSupportedDialog() {
		// Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.this);
        builder.setTitle("Bluetooth not supported");
        builder.setMessage("This application requires a valid Bluetooth adapter to run");
        
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       dialog.dismiss();
                       finish();
                   }
               });

        // Create the AlertDialog object and return it
        builder.create();
        builder.show();
	}
	
	private void showDisconnectionDialog() {
		// Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.this);
        builder.setTitle("Bluetooth disconnection");
        builder.setMessage("Are you sure you want to disconnect the ongoing connection?");
        
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   	dialog.dismiss();
                	   	
                       	// Close the ongoing connection
                       	closeConnection();
                   }
               });
         
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int id) {
	                dialog.dismiss();
	            }
        	});

        // Create the AlertDialog object and return it
        builder.create();
        builder.show();
	}
	
	public void writeBluetooth(byte[] data, int timerOp) {
		if(mBluetooth.isConnected()) {			
			// Set the timeout associated to this operation
			TIMER_BLE_OP = timerOp;
			
			Log.d(TAG, "Write Bluetooth: " + Parsers.arrayToHex(data));
			mBluetooth.sendData(data);
		} else
			mDelegateOperation.processOperationResult(null);
	}
		
	public void closeConnection() {
		if(mBluetooth != null && mBluetooth.isConnected())
			mBluetooth.close();
		else {
			if(mCloseListener != null)
        		mCloseListener.onClose();
		}
		
		if(mBtMenu != null)
			mBtMenu.setIcon(getResources().getDrawable(R.drawable.ic_action_bluetooth));
		
		showDeviceInfo();
		
		// it is the user who wants to start the disconnection so don't show the dialog
		showDisconnectionDialog = false;
		
		// We are no longer completing NFC Operations
		if(MyPreferences.isCardOperationOngoing(getApplicationContext()) == true) {
			// Consider the operation as no completed
			mDelegateOperation.processOperationNotCompleted();
		} 
	
		// Close running task
		if(runningTask != null)
			runningTask.cancel(true);
		
		// There are no operations pending
		MyPreferences.setCardOperationOngoing(getApplicationContext(), false);
		
		// Cancel the timer if exists
		if(counterAck != null)
			counterAck.cancel();
		
		// Cancel the timer if exists
		if(counterOp != null)
			counterOp.cancel();
		
		// Consider the option that the VC Card is creating
		mMyDbHelper.clearCardsOperating(getDeviceId());
	}
	
	public void closeBluetoothClient() {
		if(mBluetooth != null)
			mBluetooth.closeBluetoothClient();
		
		mBluetooth = null;
				
		// Close running task
		if(runningTask != null) {
			runningTask.cancel(true);
		}
	}
	
	@Override
	public void onRead(final byte[] status) {
		if(status == null) {
			Toast.makeText(getApplicationContext(), "Error reading data in the Bluetooth connection", Toast.LENGTH_LONG).show();
		} else {
			runOnUiThread(new Runnable() {
			    public void run() {		
			    	int bytesToCopy = 0;
 	
			    	// This is the first message that I receive for this command
					if(mBufferOffset == 0) {
						// This is the total length of the command that I will receive
						mExpectedSize = (status[1] & 0xff) + ((status[2] & 0xff) * 0x100);	
								
						// I prepare the buffer size
						mBufferDataCmd = new byte[mExpectedSize];
						
						if(mExpectedSize > QppApi.qppServerBufferSize - 3)
							bytesToCopy = QppApi.qppServerBufferSize - 3;
						else
							bytesToCopy = mExpectedSize;
						
						// Copy the data in the first position
						System.arraycopy(status, 3, mBufferDataCmd, 0, bytesToCopy);
						mBufferOffset = bytesToCopy;
					} else {
						if(mExpectedSize - mBufferOffset > QppApi.qppServerBufferSize)
							bytesToCopy = QppApi.qppServerBufferSize;
						else
							bytesToCopy = mExpectedSize - mBufferOffset;
						
						System.arraycopy(status, 0, mBufferDataCmd, mBufferOffset, bytesToCopy);
						mBufferOffset = mBufferOffset + bytesToCopy;
					}
					
					Log.d(TAG, "BLE Message received " + mBufferOffset + " / " + mExpectedSize);
					
					// If we have received the whole command we can proceed
					if(mBufferOffset == mExpectedSize) {
						if(mBufferDataCmd[0] == 'a' && mBufferDataCmd[1] == 'c' && mBufferDataCmd[2] == 'k') {
							Log.d(TAG, "ACK Received");

							// ACK Received, we can cancel the timer
							if(counterAck != null) {
								counterAck.cancel();
								
								// Start the operation specific counter
								runOnUiThread(new Runnable() {
								    public void run() {
								    	if(counterOp != null)
								    		counterOp.cancel();
								    	
								       	counterOp = new CountDownTimer(TIMER_BLE_OP, 1000) {
											@Override
											public void onFinish() {
												Log.d(TAG, "Operation Counter fired");
												
												// Operation completed on the Connected Device
								    			MyPreferences.setCardOperationOngoing(getApplicationContext(), false);
												
												// By sending a null we inform the main app about the BLE Channel error
												mDelegateOperation.processOperationNotCompleted();
											}
								
											@Override
											public void onTick(long millisUntilFinished) {
											}
										};
										
										// Start the counter
										counterOp.start();
								    }
								});	
							}		    		
				    	} else {
				    		Log.d(TAG, "Received Data: " + Parsers.arrayToHex(mBufferDataCmd));
				    		
				    		// Operation completed on the Connected Device
			    			MyPreferences.setCardOperationOngoing(getApplicationContext(), false);
			    			
			    			// Response received, we can cancel the timer
							if(counterOp != null) {
								counterOp.cancel();
								
								if(counterAck != null)
									counterAck.cancel();
							}
				    		
				    		if(mDelegateOperation != null) {
				    			mDelegateOperation.processOperationResult(mBufferDataCmd);
				    		} else
				    			Toast.makeText(getApplicationContext(), "DELEGATE NULL", Toast.LENGTH_LONG).show();
				    	}						
						
						mBufferDataCmd = new byte[0];
						mBufferOffset = 0;
						mExpectedSize = 0;
					} else {
						// Send ACK?

					}
			    }
			});
		}
	}
	
	@Override
	public void onWrite() {
		runOnUiThread(new Runnable() {
		    public void run() {
		    	if(counterAck != null)
		    		counterAck.cancel();
		    		
		    	counterAck = new CountDownTimer(TIMER_BLE_ACK_RX, 1000) {
					@Override
					public void onFinish() {
						Log.d(TAG, "ACK Counter fired");
						
						// Operation completed on the Connected Device
		    			MyPreferences.setCardOperationOngoing(getApplicationContext(), false);
						
						// By sending a null we inform the main app about the BLE Channel error
						mDelegateOperation.processOperationNotCompleted();
					}
		
					@Override
					public void onTick(long millisUntilFinished) {
					}
				};
				
				// Start the counter
				counterAck.start();
		    }
		});	
	}
	
	@Override
	public void onConnect(final boolean connected) {
		// I run on the UI Thread in order to be able to show the dialog
		runOnUiThread(new Runnable() {
		    public void run() {
		    	if(connected == true) {
		    		if(warningInactivity == false)
		    			Toast.makeText(getApplicationContext(), getResources().getString(R.string.bt_conn_established), Toast.LENGTH_LONG).show();
		    		else
		    			Toast.makeText(getApplicationContext(), getResources().getString(R.string.bt_conn_established_inact), Toast.LENGTH_LONG).show();
		    			
		    		// Store the new detected device
		    		mId = mMyDbHelper.addDevice(mName, mAddress);
		    		    		
		    		// Make the user know the connection status
		    		mBtMenu.setIcon(getResources().getDrawable(R.drawable.ic_action_bluetooth_connected));
		    		
		    		// By default we want to 
		    		showDisconnectionDialog = true;
		    	} else {
//		    		Toast.makeText(getApplicationContext(), "Bluetooth connection closed", Toast.LENGTH_LONG).show();
		    		
		    		Log.d(TAG, "OnConnect FALSE");

		    		// We are no longer completing NFC Operations
		    		if(MyPreferences.isCardOperationOngoing(getApplicationContext()) == true) {
		    			// Consider the operation as no completed
		    			mDelegateOperation.processOperationNotCompleted();
		    		} else {
		    			Log.d(TAG, "There is no operation right now");
		    		}
		    		
		    		// There are no operations ongoing
		    		MyPreferences.setCardOperationOngoing(getApplicationContext(), false);
		
		    		// Close running task
					if(runningTask != null) {
						runningTask.cancel(true);
					}
		    		
		    		// We are not connected anymore
		    		mId = 0;
		    		
		    		if(showDisconnectionDialog == true)
		    			showConnectionLostDialog();
		    	
			    	// Make the user know the connection status
			    	mBtMenu.setIcon(getResources().getDrawable(R.drawable.ic_action_bluetooth));

			    	// Close the connection
	            	if(mBluetooth != null)
	            		mBluetooth.close();
	            	
	            	if(mCloseListener != null)
	            		mCloseListener.onClose();
		    	}
		    	
		    	showDeviceInfo();
		    }
		});			
	}
	
	private void showConnectionLostDialog() {
		// Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.this);
        builder.setTitle("Bluetooth disconnection");
        builder.setMessage("The connection with your Connected Device has been lost");
        
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   	dialog.dismiss();
                       
                   }
               });


        // Create the AlertDialog object and return it
        builder.setCancelable(false);
        builder.create();
        builder.show();
	}
	
	public void showDeviceInfo() {
		LinearLayout lConnected = (LinearLayout) findViewById(R.id.l_conn_device);
		TextView t = (TextView) findViewById(R.id.t_conn_device);
		lConnected.setVisibility(View.VISIBLE);
		if(isDeviceConnected()) {
			t.setText("Connected to: " + mName);
		} else {
			t.setText("No device connected");
		}	
	}
	
	public static void setOperationDelegate(OnOperationListener listener) {
		mDelegateOperation = listener;
	}
	
	public static void setCloseListener(OnCloseListener listener) {
		mCloseListener = listener;
	}
	
	public static void setWaitingForConnResp(boolean wait) {
		if(mBluetooth != null)
			mBluetooth.setWaitingForConnResp(wait);
	}
	
	public static boolean isDeviceConnected() {
		if(mBluetooth == null)
			return false;
			
		return mBluetooth.isConnected();
	}

	public String getDeviceAddress() {
		return mAddress;
	}
	
	public String getDeviceName() {
		return mName;
	}
	
	public static int getDeviceId() {
		return mId;
	}
	
	public static void setRunningAsyncTask(AsyncTask<?, ?, ?> task) {
		runningTask = task;
	}

	@Override
	public void onConnectionPending() {
		Toast.makeText(BaseActivity.this, "Pending connection to be solved. Please wait a few seconds... (30 secs max)", Toast.LENGTH_LONG).show();
		
	}
}
