package karthik.WeatherApp;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.util.Log;
import android.widget.TextView;

class DateTimeThread extends Thread {
	private static final String TAG = "WeatherAppDateTimeThread";
	
	private WeatherApp mActivity;
	private TextView timeView, updateTime;
	private static final SimpleDateFormat sdf = new SimpleDateFormat(
			"dd MMM, yyyy HH:mm:ss a");
	private Calendar cdr;
		
	DateTimeThread(WeatherApp mActivity, TextView timeView, TextView updateTime){
		this.mActivity = mActivity;
		this.timeView = timeView;
		this.updateTime = updateTime;
	}
	
	@Override
	public void run() {

		while (!Thread.currentThread().isInterrupted()) {
			try {
				update();
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Log.v(TAG, "DateTimeThread interrupted");
			}
		}
	}

	public void update() {
		mActivity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				cdr = Calendar.getInstance();
				timeView.setText(sdf.format(cdr.getTime()));
				long timeSinceLastWeatherUpdate = (System
						.currentTimeMillis() - mActivity.lastUpdate) / 1000;
				String updateString = "";
				if (timeSinceLastWeatherUpdate < 60)
					updateString = String
							.valueOf(timeSinceLastWeatherUpdate)
							+ " seconds";
				else {
					long mins = timeSinceLastWeatherUpdate / 60;
					updateString = String.valueOf(mins) + " minute"
							+ String.valueOf(mins > 1 ? "s" : "");
				}

				updateTime.setText("Updated " + updateString + " ago");
			}

		});
	}

}
