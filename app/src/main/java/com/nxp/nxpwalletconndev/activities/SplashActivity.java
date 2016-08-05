package com.nxp.nxpwalletconndev.activities;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;

import com.nxp.nxpwalletconndev.R;

public class SplashActivity extends Activity {
	public static final String TAG = "ActivitySplash";
	private CountDownTimer contador;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		contador = new CountDownTimer(2000, 1000) {
			@Override
			public void onFinish() {
				Intent intent = new Intent(getBaseContext(), MainActivity.class);
				startActivity(intent);
				finish();
			}

			@Override
			public void onTick(long millisUntilFinished) {

			}
		}.start();
	}

	@Override
	public void onBackPressed() {
		contador.cancel();
		super.onBackPressed();
	}

	@Override
	protected void onStop() {
		contador.cancel();
		super.onStop();
	}
}
