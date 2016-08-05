package com.nxp.nxpwalletconndev.activities;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nxp.nxpwalletconndev.R;
import com.nxp.ssdp.btclient.BluetoothClient.OnBluetoothConnectListener;
import com.nxp.ssdp.btclient.BluetoothClient.OnBluetoothReadListener;

public class LoyaltyActivity extends BaseActivity implements OnBluetoothConnectListener, OnBluetoothReadListener {
	public static class LoyaltyEntry {	
		public int  id;
		public int iconRes;
		public String name;
		public String description;
		
		public LoyaltyEntry(final int id, final int iconRes, final String name, final String description) {
			this.id = id;
			this.name = name;
			this.iconRes = iconRes;
			this.description = description;
		}
	};
	
	public static final int TEMP_CARD_ID = 0xF0;
	
	public static final int ACTION_CREATE_VC = 0;
    public static final int ACTION_ADD_AND_UPDATE_MDAC_VC = 1;
    
    public LoyaltyEntry mEntry = null;
    public int mAction = 0;
    
    public int mVcEntry;
	
	private List<LoyaltyEntry> mLoyalty = new ArrayList<LoyaltyEntry>();
	private ArrayAdapter<LoyaltyEntry> mLoyaltyAdapter;
	
	private ListView list;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_loyalty);
		
		mLoyalty.add(new LoyaltyEntry(0, R.drawable.seven_eleven_small, "7-Eleven", "Fast, convenient."));
		mLoyalty.add(new LoyaltyEntry(1, R.drawable.carrefour, "Carrefour", "Competitive prices."));
		
		list = (ListView) findViewById(R.id.listLoyalty);
		
		// Get database helper
//		mMyDbHelper = new MyDbHelper(this);
	}

	@Override
    protected void onResume(){        
        // Show created Virtual Cards
        displayLoyalty();
        super.onResume();
    }
    
    protected void displayLoyalty() {
		list = (ListView) findViewById(R.id.listLoyalty);	
		mLoyaltyAdapter = new ArrayAdapter<LoyaltyEntry>(this,
				R.layout.loyalty_entry, mLoyalty) {
			@Override
			public View getView(final int position, final View convertView,
					final ViewGroup parent) {
				final LoyaltyEntry entry = getItem(position);

				View root;

				if (convertView == null) {
					final LayoutInflater inflater = LayoutInflater
							.from(LoyaltyActivity.this);
					Log.d("Checkpoint", "Layout inflater executed correctly");
					root = inflater
							.inflate(R.layout.loyalty_entry,
									parent, false);
				} else {
					root = convertView;
				}		

				((ImageView) root.findViewById(R.id.loyalty_icon))
					.setImageResource(entry.iconRes);
				
				((TextView) root.findViewById(R.id.loyalty_name))
					.setText(entry.name);
				
				((TextView) root.findViewById(R.id.loyalty_description))
					.setText(entry.description);
								
				root.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						if(entry.id==0)
						{
							Intent i = new Intent(getApplicationContext(), SevenElevenActivity.class);
							i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							startActivity(i);
							finish();
						}
						else if(entry.id==1){
							Toast.makeText(getApplicationContext(), "To be available soon", Toast.LENGTH_LONG).show();
						}
					}
				});
		
				return root;
			}
		};

		list.setAdapter(mLoyaltyAdapter);
	}
    
  
    
    
}
