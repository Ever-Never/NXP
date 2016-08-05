package com.nxp.nxpwalletconndev.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nxp.nxpwalletconndev.R;
import com.nxp.nxpwalletconndev.storage.MyPreferences;

public class LoginActivity extends BaseActivity {
	public static final int USER_REGISTERED = 0;
	
	private EditText userName;
	private EditText userSurname;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		
		userName = (EditText) findViewById(R.id.user_name);
		userSurname = (EditText) findViewById(R.id.user_surname);
		
		if(MyPreferences.isLogued(getApplicationContext()))
		{
			userName.setText(MyPreferences.getUserName(getApplicationContext()), TextView.BufferType.EDITABLE);
			userSurname.setText(MyPreferences.getUserSurname(getApplicationContext()), TextView.BufferType.EDITABLE);
		}
		
		((Button) findViewById(R.id.login)).setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				loginUser(userName.getText().toString(), userSurname.getText().toString());
				
			}
		});
			
	}
		
	private void loginUser(String name, String surname) {
		if(name.equals("") == true || surname.equals("") == true) {
			Toast.makeText(getApplicationContext(), "Invalid data", Toast.LENGTH_LONG).show();
		} else {
			MyPreferences.setUserName(getApplicationContext(), name);
			MyPreferences.setUserSurname(getApplicationContext(), surname);
			
			// We can save the login status
			MyPreferences.setLogued(getApplicationContext(), true);			
			
			Toast.makeText(getApplicationContext(), "Successful login", Toast.LENGTH_LONG).show();
			finish();
		}
	}
}
