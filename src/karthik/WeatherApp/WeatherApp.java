package karthik.WeatherApp;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import karthik.json.KJ_JSONArray;
import karthik.json.KJ_JSONException;
import karthik.json.KJ_JSONObject;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

public class WeatherApp extends Activity implements
		ConnectionCallbacks, OnConnectionFailedListener {

	private final String TAG = "WeatherApp";
	protected final String forecastImageName = "forecast";
	private final String minForecastTempName = "minTempForecast";
	private final String maxForecastTempName = "maxTempForecast";
	private final String weatherForecastDescName = "weatherViewForecast";
	private final String ForecastDowName = "ForecastDow";
	private final String rainProbName = "rainProb";

	final String degree = "\u00b0";

	private TextView timeView, tempView, cityView, coordView, FeelsLike,
			humidityView, mainWeatherDesc, updateTime;
	private LinearLayout citySelectLayout, headerLayout, splash;
			
	private LinearLayout hourlyList, hourlyItem;
	private RelativeLayout mainScreen, HourlyScreen, currentWeatherLayout;

	private Thread dtThread, weatherThread;
	long lastUpdate;

	private ImageView refreshImage;
	private EditText citySelect;
	InputMethodManager keyboard;

	final int numDaysForecast = 5;
	final String defaultCity = "NewYork";
	private String displayCity = "";

	MainWeatherObject weatherData = new MainWeatherObject("", 0);
	MainWeatherObject[] forecastArray = new MainWeatherObject[numDaysForecast];
	MainWeatherObject[][] hourlyForecastArray = new MainWeatherObject[24][numDaysForecast];

	private ImageView weatherIcon;
	private HashMap<String, View> forecastImageViews;
	private HashMap<String, View> forecastMinTemperatureViews;
	private HashMap<String, View> forecastMaxTemperatureViews;
	private HashMap<String, View> weatherForecastViews;
	private HashMap<String, View> forecastDowViews;
	private HashMap<String, View> rainProbViews;

	// used for location services using new Location API
	private GoogleApiClient mGoogleApiClient;
	private Location mLocation;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		getResIDs();

		 mGoogleApiClient = new GoogleApiClient.Builder(this)
		 .addConnectionCallbacks(this)
		 .addOnConnectionFailedListener(this)
		 .addApi(LocationServices.API)
		 .build();
	}

	protected void onStart() {
		super.onStart();
		// Connect the client.
		mGoogleApiClient.connect();
	}

    protected void onStop() {
        super.onStop();
        
        // Disconnecting the client invalidates it.
    	if (mGoogleApiClient.isConnected()) {
    		mGoogleApiClient.disconnect();
    	}
    }
    
	protected void onPause() {
		super.onPause();
		dtThread.interrupt();
		dtThread = null;
		weatherThread.interrupt();
		weatherThread = null;
	}

	protected void onResume() {
		super.onResume();
		getWeatherAndForecast();
		dtThread = new DateTimeThread(this, timeView, updateTime);
		dtThread.start();
	}

	private void getResIDs() {
		// gets ID's for layout View elements
		// uses reflections so is a bit slow when app is created

		timeView = (TextView) findViewById(R.id.timeView);
		cityView = (TextView) findViewById(R.id.cityView);
		coordView = (TextView) findViewById(R.id.coordView);
		tempView = (TextView) findViewById(R.id.tempView);
		FeelsLike = (TextView) findViewById(R.id.FeelsLike);
		humidityView = (TextView) findViewById(R.id.humidity);
		updateTime = (TextView) findViewById(R.id.UpdateTime);
		citySelect = (EditText) findViewById(R.id.citySelect);
		cityView = (TextView) findViewById(R.id.cityView);

		headerLayout = (LinearLayout) findViewById(R.id.headerLayout);
		citySelectLayout = (LinearLayout) findViewById(R.id.citySelectLayout);
		splash = (LinearLayout) findViewById(R.id.splash);
		mainScreen = (RelativeLayout) findViewById(R.id.MainScreen);
		currentWeatherLayout = (RelativeLayout) findViewById(R.id.CurrentWeatherLayout);

		mainWeatherDesc = (TextView) findViewById(R.id.weatherView);
		weatherIcon = (ImageView) findViewById(R.id.weatherIcon);

		// below are a bit slow since they use reflection
		forecastImageViews = populateHashMap(forecastImageName);
		forecastMinTemperatureViews = populateHashMap(minForecastTempName);
		forecastMaxTemperatureViews = populateHashMap(maxForecastTempName);
		weatherForecastViews = populateHashMap(weatherForecastDescName);
		forecastDowViews = populateHashMap(ForecastDowName);
		rainProbViews = populateHashMap(rainProbName);

		hourlyList = (LinearLayout) findViewById(R.id.HourlyList);
		hourlyItem = (LinearLayout) findViewById(R.id.hourlyItem);
		HourlyScreen = (RelativeLayout) findViewById(R.id.HourlyScreen);

		refreshImage = (ImageView) findViewById(R.id.refresh);

		currentWeatherLayout.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				getWeatherAndForecast();
			}
		});

		citySelect
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) {
						if (actionId == EditorInfo.IME_ACTION_DONE) {
							refreshButtonClicked(v);
						}
						return false;
					}
				});

		splash.setOnClickListener(new View.OnClickListener() {
			// button press on splash screen takes user to main screen
			// useful if there is some problem with the input city and the data
			// cannot load
			public void onClick(View v) {
				showMainScreen();
			}
		});
	}

	public void ForecastLayoutClicked(View v) {
		// called from onClick parameter of ForecastLayout elements in main xml
		// file

		if (weatherThread.isAlive())
			showAlertDialog("Please wait ...",
					"Hourly Forecast data still loading. Please wait ...", this);
		else {
			String resName = getResources().getResourceName(v.getId()).trim();
			int dayIndex = Integer
					.parseInt(resName.substring(resName.length() - 1)) - 1;
			HourlyScreen.setVisibility(View.VISIBLE);
			mainScreen.setVisibility(View.GONE);
			try {
				createHourlyLayout(dayIndex);
			} catch (IOException ioe) {
				Log.i("WeatherApp/updateForecastImages", ioe.getMessage());
				showAlertDialog("I/O Error", ioe.getMessage(), this);
			}
			HourlyScreen.invalidate();
		}
	}

	public void onBackPressed() {
		if (HourlyScreen.getVisibility() == View.VISIBLE)
			onHourlyScreenClick(null);
		else
			super.onBackPressed();
	}

	public void onHourlyScreenClick(View v) {
		// button press on hourly forecast screen takes user back to main screen

		mainScreen.setVisibility(View.VISIBLE);
		HourlyScreen.setVisibility(View.GONE);
	}

	private void showMainScreen() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				splash.setVisibility(View.GONE);
				mainScreen.setVisibility(View.VISIBLE);
			}
		});
	}

	public void refreshButtonClicked(View v) {
		Log.i("ButtonClick", "Refresh button was clicked or DONE key pressed");
		keyboard.hideSoftInputFromWindow(citySelect.getWindowToken(), 0);
		getWeatherAndForecast();
		switchHeader();
	}

	private void switchHeader() {

		headerLayout.setVisibility(View.VISIBLE);
		citySelectLayout.setVisibility(View.INVISIBLE);
	}

	public void switchCity(View v) {
		headerLayout.setVisibility(View.INVISIBLE);
		citySelectLayout.setVisibility(View.VISIBLE);
		citySelect.setText("");
		citySelect.requestFocus();
		keyboard.showSoftInput(citySelect, InputMethodManager.SHOW_IMPLICIT);
	}

	private HashMap<String, View> populateHashMap(String title) {
		int resID, i;
		HashMap<String, View> m = new HashMap<String, View>();

		for (i = 1; i <= numDaysForecast; i++) {
			resID = getResources().getIdentifier(
					title.concat(String.valueOf(i)), "id", getPackageName());
			m.put(title + i, findViewById(resID));
		}
		return m;

	}

	private void createHourlyLayout(int day) throws IOException {
		String timeString;
		// first clear out HourlyList except for the first one
		if (hourlyList.getChildCount() > 1)
			hourlyList.removeViews(1, hourlyList.getChildCount() - 1);
		int hourCtr = 0;

		List<ImageView> ivObjs = new ArrayList<ImageView>();
		List<MainWeatherObject> hourlyForecastObjs = new ArrayList<MainWeatherObject>();

		while ((hourlyForecastArray[hourCtr++][day] == null) && (hourCtr < 24))
			;

		hourCtr--;

		for (; hourCtr < 24; hourCtr++) {
			// add views
			HourlyItemLayout newItem = new HourlyItemLayout(this, hourlyItem);
			newItem.setTemp(String.format("%.0f%s C",
					hourlyForecastArray[hourCtr][day].getTemperature(), degree));
			newItem.setRainProb(String.format("%d%% ",
					hourlyForecastArray[hourCtr][day].getRainProb()));
			newItem.setWeather(hourlyForecastArray[hourCtr][day]
					.getMainWeatherDesc());
			timeString = new SimpleDateFormat("h aa")
					.format(hourlyForecastArray[hourCtr][day].getForecastDate()
							.getTime());
			newItem.setTime(" " + timeString);
			// create List of Objects and ImageViews to display the icon images
			hourlyForecastObjs.add(hourlyForecastArray[hourCtr][day]);
			ivObjs.add(newItem.getIconIV());

			hourlyList.addView(newItem);
		}
		MainWeatherObject.showWeatherIcons(WeatherApp.this, hourlyForecastObjs,
				ivObjs);
		Log.i("createHourlyLayout(WeatherApp)",
				"num children is " + hourlyList.getChildCount());
	}

	private void getWeatherAndForecast() {
		weatherThread = new Thread(new WeatherRunnable(this));
		Toast.makeText(this, "Refreshing", Toast.LENGTH_SHORT).show();
		weatherThread.start();
	}

	protected void updateWeather() throws IOException {
		double celsius, fahr;

		lastUpdate = System.currentTimeMillis();
		cityView.setText(weatherData.fullCityName);
		coordView.setText(String.format("Lat: %.2f Long: %.2f",
				weatherData.getLatitude(), weatherData.getLongitude()));
		updateTime.setText("Updated");

		celsius = weatherData.getTemperature();
		fahr = (celsius * (9.0 / 5.0)) + 32;
		tempView.setText(String.format("%.1f%3$s C, %.1f%3$s F", celsius, fahr,
				degree));
		FeelsLike.setText("Feels Like: " + weatherData.getFeelsLike());
		humidityView.setText(String.format("Humidity: %s",
				weatherData.getHumidity()));
		mainWeatherDesc.setText(weatherData.getMainWeatherDesc());
		weatherData.showWeatherIcon(this, weatherIcon);
		switchHeader();
	}

	protected void updateForecast() throws IOException {
		int numDaysAvailable;
		TextView minTV, maxTV, forecastDesc, forecastDow, rp;

		numDaysAvailable = forecastArray.length;

		for (int i = 1; i <= numDaysAvailable; i++) {

			minTV = (TextView) forecastMinTemperatureViews
					.get(minForecastTempName + i);
			maxTV = (TextView) forecastMaxTemperatureViews
					.get(maxForecastTempName + i);
			forecastDesc = (TextView) weatherForecastViews
					.get(weatherForecastDescName + i);
			forecastDow = (TextView) forecastDowViews.get(ForecastDowName + i);
			rp = (TextView) rainProbViews.get(rainProbName + i);

			forecastDesc.setText(forecastArray[i - 1].getMainWeatherDesc());
			minTV.setText(String.format("%.1f%s C",
					forecastArray[i - 1].getMinTemperature(), degree));
			maxTV.setText(String.format("%.1f%s C",
					forecastArray[i - 1].getMaxTemperature(), degree));
			rp.setText(String.format("%d%%", forecastArray[i - 1].getRainProb()));

			forecastDow.setText(forecastArray[i - 1].getDOW());

		}

	}

	private void showAlertDialog(final String title, final String msg,
			final Context con) {
		runOnUiThread(new Runnable() {
			public void run() {
				AlertDialog.Builder dlgAlert = new AlertDialog.Builder(con);
				dlgAlert.setMessage(msg);
				dlgAlert.setTitle(title);
				dlgAlert.setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(
									DialogInterface dialogInterface, int i) {
								dialogInterface.dismiss();
							}
						});
				dlgAlert.create();
				dlgAlert.show();
			}
		});
	}

	private class WeatherRunnable implements Runnable {

		Context con;

		public WeatherRunnable(Context parentContext) {
			con = parentContext;
		}

		public void run() {
			try {
				getLocation();
				Log.i(TAG, "Making JSON request for " + displayCity);
				KJ_JSONObject rawData = WeatherReader.getWeatherData(displayCity);
				if (!rawData.has("current_observation")) // some problem with
															// city name
				{
					KJ_JSONObject tempData = rawData.getJSONObject("response");

					if (tempData.has("error")) {
						Log.i(TAG, tempData.getJSONObject("error")
								.getString("description"));
						showAlertDialog(
								"Query Error",
								tempData.getJSONObject("error").getString(
										"description"), con);
						displayCity = defaultCity;
						return;
					}
					// multiple matches for city name - show menu to select
					if (tempData.has("results")) {
						final String[] results = getCityList(tempData
								.getJSONArray("results"));
						showCityListDialog(results, con);
						return;
					}

				}

				WeatherReader.getParsedWeatherData(weatherData, rawData);
				WeatherReader.getForecastArray(forecastArray, displayCity,
						numDaysForecast);

				updateUI();
				// continue to use non-UI thread to load weather image icons
				updateForecastImages();
				// last of all, load the hourly forecast data, still in
				// background
				clearHourlyForecastArray();
				WeatherReader.getHourlyForecastArray(hourlyForecastArray,
						displayCity, numDaysForecast);
			} catch (IOException ioe) {
			} catch (KJ_JSONException jse) {
				Log.i(TAG + "WeatherRunnable/run", jse.getMessage());
				showAlertDialog("Query Error", jse.getMessage(), con);
				showMainScreen();
			}
		}

		private void clearHourlyForecastArray() {
			for (int i = 0; i < 24; i++)
				for (int j = 0; j < numDaysForecast; j++)
					hourlyForecastArray[i][j] = null;
		}

		protected void getLocation() {

			String loc = citySelect.getText().toString();

			if (!loc.equals("")) {
				displayCity = loc;
				return;
			}

			if (mGoogleApiClient.isConnected()) {
				// first try to get nearest city using Location services
				mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
				try {
					displayCity = WeatherReader.getGeoLookupCity(
							mLocation.getLatitude(), mLocation.getLongitude());
				} catch (KJ_JSONException jse) {
					Log.i(TAG + "WeatherRunnable/getLocation", jse.getMessage());
					showAlertDialog("Query Error", jse.getMessage(), con);
					showMainScreen();
				} catch (IOException ioe) {
					Log.i(TAG + "WeatherRunnable/getLocation", ioe.getMessage());
					showAlertDialog("Query Error", ioe.getMessage(), con);
					showMainScreen();
				}
			}
			if (displayCity == "")
				displayCity = defaultCity;
		}

		private void showCityListDialog(final String[] results,
				final Context con) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					AlertDialog.Builder builder = new AlertDialog.Builder(con);
					builder.setItems(results,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									citySelect.setText(results[which]);
									displayCity = results[which];
									dialog.dismiss();
									switchHeader();
									getWeatherAndForecast();
								}
							});
					builder.create().show();

				}
			});
		}

		private String[] getCityList(KJ_JSONArray results)
				throws KJ_JSONException {
			String[] cityList = new String[results.length()];

			for (int i = 0; i < results.length(); i++) {
				KJ_JSONObject temp = results.getJSONObject(i);
				cityList[i] = temp.getString("city");
				String country = temp.getString("country");
				if (country.equals("US"))
					cityList[i] = cityList[i].concat(", "
							+ temp.getString("state"));
				else if (country.equals("UK"))
					// some weirdness in Weather Underground where UK shows
					// country_iso1366 as GB
					// but URL requires UK
					cityList[i] = cityList[i].concat(",UK");
				else
					cityList[i] = cityList[i].concat(", "
							+ temp.getString("country_iso3166"));
			}
			return cityList;
		}

		private void updateUI() {
			showMainScreen();
			runOnUiThread(new Runnable() {
				public void run() {
					try {
						updateWeather();
						updateForecast();
					} catch (IOException ioe) {
					}
				}

			});
		}

		private void updateForecastImages() {
			runOnUiThread(new Runnable() {
				public void run() {
					List<ImageView> ivObjs = new ArrayList<ImageView>();
					for (int i = 1; i <= forecastArray.length; i++) {
						ImageView iv = (ImageView) forecastImageViews
								.get(forecastImageName + i);
						ivObjs.add(iv);
					}
					MainWeatherObject.showWeatherIcons(WeatherApp.this,
							Arrays.asList(forecastArray), ivObjs);
				}
			});
		}
	}

	/*
	 * Callback methods for Google Location Services
	 */

	@Override
	public void onConnected(Bundle connectionHint) {
		Log.v(TAG, "Location services connected");
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		// Refer to the javadoc for ConnectionResult to see what error codes
		// might be returned in
		// onConnectionFailed.
		Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
				+ result.getErrorCode());
	}

	/*
	 * Called by Google Play services if the connection to GoogleApiClient drops
	 * because of an error.
	 */
	public void onDisconnected() {
		Log.v(TAG, "Location services disconnected");
	}

	@Override
	public void onConnectionSuspended(int cause) {
		// The connection to Google Play services was lost for some reason. We
		// call connect() to
		// attempt to re-establish the connection.
		Log.i(TAG, "Location services connection suspended");
		mGoogleApiClient.connect();
	}
}
