package com.kevinhinds.riseandset;

import android.support.v7.app.ActionBarActivity;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.pollfish.main.PollFish;
import com.pollfish.constants.Position;
import com.kevinhinds.riseandset.marketplace.MarketPlace;
import com.kevinhinds.riseandset.updates.LatestUpdates;
import com.google.android.gms.ads.*;
import com.appjolt.winback.Winback;

import android.os.Bundle;
import android.content.res.Configuration;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * main application activity
 * 
 * @author khinds
 */
public class MainActivity extends ActionBarActivity {

	protected Location currentLocation;
	protected String completeForecast;
	protected Drawable earthViewImage;
	protected Drawable sunViewImage;
	protected Drawable moonViewImage;
	public ArrayList<String> astroData;
	protected String finalAddress;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// handle the spinner for the earth view changer
		Spinner earthViewsSpinner = (Spinner) findViewById(R.id.earthViewsSpinner);
		earthViewsSpinner.setOnItemSelectedListener(new CustomOnItemSelectedListenerEarth());

		// handle the spinner for the earth view changer
		Spinner sunViewsSpinner = (Spinner) findViewById(R.id.sunViewsSpinner);
		sunViewsSpinner.setOnItemSelectedListener(new CustomOnItemSelectedListenerSun());

		// update the date time field on the UI
		TextView currentDateTime = (TextView) findViewById(R.id.currentDateTime);
		currentDateTime.setText(Localized.getCurrentDateTime(this));

		// get the current moon
		try {
			new GetMoonImageTask().execute(new URL("http://space.kevinhinds.net/moon.jpg?" + Long.toString(new java.util.Date().getTime())));
		} catch (Exception e) {
		}
		LocationManager locationManager = (LocationManager) this.getSystemService(MainActivity.LOCATION_SERVICE);
		currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		updateUIWithLocationFound();

		// Appjolt - Init SDK if it's Enabled
		if (Boolean.parseBoolean(getResources().getString(R.string.appjolt_enabled))) {
			Winback.init(this);
		}

		// show the latest update notes if the application was just installed
		LatestUpdates.showFirstInstalledNotes(this);

		// Look up the AdView as a resource and load a request
		if (Boolean.parseBoolean(getResources().getString(R.string.admob_enabled))) {

			// setup the adMob Ad 
			AdView adView = new AdView(this);
			adView.setAdUnitId(getResources().getString(R.string.admob_ads_id));
			adView.setAdSize(AdSize.BANNER);

			// apply it to the bottom of the screen
			RelativeLayout layout = (RelativeLayout) findViewById(R.id.adViewContainer);
			layout.addView(adView);
			AdRequest adRequest = new AdRequest.Builder().build();
			adView.loadAd(adRequest);
		}

	}

	/**
	 * update the UI with location found by querying USNO website
	 */
	protected void updateUIWithLocationFound() {

		new GetAstroDataTask().execute(APIs.getUSNOURLByLocation(currentLocation));
		new GetWeatherDataTask().execute(APIs.getForecastURLByLocation(currentLocation, getResources().getConfiguration().locale.getLanguage()));

		try {
			// get current user address
			Geocoder geoCoder = new Geocoder(this, Locale.getDefault());
			StringBuilder builder = new StringBuilder();
			List<Address> address = geoCoder.getFromLocation(currentLocation.getLatitude(), currentLocation.getLongitude(), 1);
			int maxLines = address.get(0).getMaxAddressLineIndex();
			for (int i = 0; i < maxLines; i++) {
				String addressStr = address.get(0).getAddressLine(i);
				builder.append(addressStr);
				builder.append(" ");
			}
			finalAddress = builder.toString();
			TextView sunAddressLine = (TextView) findViewById(R.id.sunAddressLine);
			sunAddressLine.setText(finalAddress);

			TextView earthAddressLine = (TextView) findViewById(R.id.earthAddressLine);
			earthAddressLine.setText(finalAddress);

			TextView moonAddressLine = (TextView) findViewById(R.id.moonAddressLine);
			moonAddressLine.setText(finalAddress);
		} catch (Exception e1) {
		}
	}

	/**
	 * get the earth and sun
	 * 
	 * @author khinds
	 */
	private class GetWeatherDataTask extends AsyncTask<URL, Integer, Long> {

		private String weatherOutput;

		protected void onProgressUpdate(Integer... progress) {
		}

		protected void onPostExecute(Long result) {
			TextView currentForecast = (TextView) findViewById(R.id.currentForecast);
			currentForecast.setText(completeForecast);
		}

		@Override
		protected Long doInBackground(URL... params) {
			try {
				URLConnection conn = params[0].openConnection();
				// open the stream and put it into BufferedReader
				BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String inputLine;
				weatherOutput = "";
				while ((inputLine = br.readLine()) != null) {
					weatherOutput = weatherOutput + inputLine;
				}
				br.close();
			} catch (Exception e) {
			}
			try {
				completeForecast = APIs.getForecastFromAPIData(weatherOutput, Localized.useCelsius(getResources().getConfiguration().locale.getCountry()));
			} catch (Exception e) {
			}
			return null;
		}
	}

	/**
	 * spinner onclick listener for the sun views
	 * 
	 * @author khinds
	 */
	public class CustomOnItemSelectedListenerSun implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
			String[] sunViews = getResources().getStringArray(R.array.sun_views_array_urls);
			URL sunURL = null;
			try {
				sunURL = new URL(sunViews[pos]);
			} catch (Exception e) {
			}
			new GetSunImagesTask().execute(sunURL);
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
		}
	}

	/**
	 * spinner onclick listener for the earth views with position zero being the NASA Epic camera
	 * image
	 * 
	 * @author khinds
	 */
	public class CustomOnItemSelectedListenerEarth implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
			ImageView earthView = (ImageView) findViewById(R.id.earthView);
			if (pos == 0) {
				URL epicURL = null;
				try {
					epicURL = new URL("http://epic.gsfc.nasa.gov/api/images.php");
					new GetEpicImagesDataTask().execute(epicURL);

					// enlarge the earth image because of black margin difference
					int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 275, getResources().getDisplayMetrics());
					earthView.getLayoutParams().height = height;

				} catch (Exception e) {
				}
				new GetEpicImagesDataTask().execute(epicURL);
			} else {

				// resize the earth image because of black margin difference
				int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 275, getResources().getDisplayMetrics());
				earthView.getLayoutParams().height = height;

				String whichEarthViewSelected = parent.getItemAtPosition(pos).toString();
				String imageName = "earth_sun";
				if (whichEarthViewSelected.equals("Earth from the Sun")) {
					imageName = "earth_sun";
				} else if (whichEarthViewSelected.equals("Earth from the Moon")) {
					imageName = "earth_moon";
				} else if (whichEarthViewSelected.equals("Day and Night across the Earth")) {
					imageName = "day_night";
				} else if (whichEarthViewSelected.equals("Earth from the North Pole")) {
					imageName = "earth_north";
				} else if (whichEarthViewSelected.equals("Earth from the South Pole")) {
					imageName = "earth_south";
				}
				getPlanetImageByName(imageName);
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
		}
	}

	/**
	 * get the most recent NASA Epic Image and display it for the earth image
	 * 
	 * @author khinds
	 */
	private class GetEpicImagesDataTask extends AsyncTask<URL, Integer, Long> {

		private String epicOutput;

		protected void onProgressUpdate(Integer... progress) {
		}

		protected void onPostExecute(Long result) {
			TextView currentForecast = (TextView) findViewById(R.id.currentForecast);
			currentForecast.setText(completeForecast);
		}

		@Override
		protected Long doInBackground(URL... params) {
			try {
				URLConnection conn = params[0].openConnection();
				BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String inputLine;
				epicOutput = "";
				while ((inputLine = br.readLine()) != null) {
					epicOutput = epicOutput + inputLine;
				}
				br.close();
			} catch (Exception e) {
			}
			try {
				// get NASA DSCOVR::EPIC Image from available list by webservice call
				JSONArray epicImageImages = new JSONArray(epicOutput);
				JSONObject epicImage = (JSONObject) epicImageImages.get(epicImageImages.length() - 2);
				String recentEpicImage = (String) epicImage.get("image");
				URL epicImageURL = null;
				try {
					epicImageURL = new URL("http://epic.gsfc.nasa.gov/epic-archive/jpg/" + recentEpicImage + ".jpg");
				} catch (Exception e) {
				}
				new GetEarthImagesTask().execute(epicImageURL);
			} catch (Exception e) {
			}
			return null;
		}
	}

	/**
	 * get planet image by name to show on the UI
	 * 
	 * @param name
	 */
	protected void getPlanetImageByName(String name) {
		Date currentDate = new java.util.Date();
		long timestamp = currentDate.getTime();
		URL planetURL = null;
		try {
			planetURL = new URL("http://space.kevinhinds.net/" + name + ".jpg?" + Long.toString(timestamp));
		} catch (Exception e) {
		}
		new GetEarthImagesTask().execute(planetURL);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_bitstreet:
			MarketPlace.viewAllPublisherApps(this);
			break;
		case R.id.menu_fullversion:
			MarketPlace.viewPremiumApp(this);
			break;
		case R.id.menu_suggested:
			MarketPlace.viewSuggestedApp(this);
			break;
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.findItem(R.id.menu_fullversion).setVisible(!Boolean.parseBoolean(getResources().getString(R.string.is_full_version)));
		menu.findItem(R.id.menu_suggested).setVisible(Boolean.parseBoolean(getResources().getString(R.string.has_suggested_app)));
		return true;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (Boolean.parseBoolean(getResources().getString(R.string.pollfish_enabled))) {
			PollFish.init(this, getString(R.string.pollfish_api_key), Position.BOTTOM_RIGHT, 5);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (Boolean.parseBoolean(getResources().getString(R.string.pollfish_enabled))) {
			PollFish.init(this, getString(R.string.pollfish_api_key), Position.BOTTOM_LEFT, 5);
		}
	}

	/**
	 * get the moon's current image
	 * 
	 * @author khinds
	 */
	private class GetMoonImageTask extends AsyncTask<URL, Integer, Long> {

		protected void onProgressUpdate(Integer... progress) {
		}

		protected void onPostExecute(Long result) {
			ImageView moonView = (ImageView) findViewById(R.id.moonView);
			moonView.setImageDrawable(moonViewImage);
		}

		@Override
		protected Long doInBackground(URL... params) {
			try {
				moonViewImage = Drawable.createFromStream(params[0].openConnection().getInputStream(), "Moon View");
			} catch (Exception e) {
			}
			return null;
		}
	}

	/**
	 * get the sun's image by value selected
	 * 
	 * @author khinds
	 */
	private class GetSunImagesTask extends AsyncTask<URL, Integer, Long> {

		protected void onProgressUpdate(Integer... progress) {
		}

		protected void onPostExecute(Long result) {
			ImageView sunView = (ImageView) findViewById(R.id.sunView);
			sunView.setImageDrawable(sunViewImage);
		}

		@Override
		protected Long doInBackground(URL... params) {
			try {
				sunViewImage = Drawable.createFromStream(params[0].openConnection().getInputStream(), "Sun View");
			} catch (Exception e) {
			}
			return null;
		}
	}

	/**
	 * get the earth's image by value selected
	 * 
	 * @author khinds
	 */
	private class GetEarthImagesTask extends AsyncTask<URL, Integer, Long> {

		protected void onProgressUpdate(Integer... progress) {
		}

		protected void onPostExecute(Long result) {
			ImageView earthView = (ImageView) findViewById(R.id.earthView);
			earthView.setImageDrawable(earthViewImage);
		}

		@Override
		protected Long doInBackground(URL... params) {
			try {
				earthViewImage = Drawable.createFromStream(params[0].openConnection().getInputStream(), "Earth View");
			} catch (Exception e) {
			}
			return null;
		}
	}

	/**
	 * get data from the USNO as a background thread
	 * 
	 * @author khinds
	 */
	private class GetAstroDataTask extends AsyncTask<URL, Integer, Long> {

		protected void onProgressUpdate(Integer... progress) {
		}

		protected void onPostExecute(Long result) {
			try {
				updateUSNOFields();
			} catch (Exception e) {
			}
		}

		@Override
		protected Long doInBackground(URL... params) {
			// get USNO data from Jsoup and create astroData array
			astroData = APIs.parseUSNODataByURL(params);
			return null;
		}
	}

	/**
	 * This method converts dp unit to equivalent pixels, depending on device density.
	 * 
	 * @param dp
	 *            A value in dp (density independent pixels) unit. Which we need to convert into
	 *            pixels
	 * @param context
	 *            Context to get resources and device specific display metrics
	 * @return A float value to represent px equivalent to dp depending on device density
	 */
	public static float convertDpToPixel(float dp, Context context) {
		Resources resources = context.getResources();
		DisplayMetrics metrics = resources.getDisplayMetrics();
		float px = dp * (metrics.densityDpi / 160f);
		return px;
	}

	/**
	 * update all the time fields on the UI with the newly found USNO data
	 */
	public void updateUSNOFields() {

		// get current date time / location and update the UI with it
		String rightNowDate = (DateFormat.format("MM-dd-yyyy", new java.util.Date()).toString());

		TextView sunBeginCivilTwilightTextTime = (TextView) findViewById(R.id.sunBeginCivilTwilightTextTime);
		sunBeginCivilTwilightTextTime.setText(Localized.localizedTime(astroData.get(0), MainActivity.this, rightNowDate));

		TextView sunriseTextTime = (TextView) findViewById(R.id.sunriseTextTime);
		sunriseTextTime.setText(Localized.localizedTime(astroData.get(1), MainActivity.this, rightNowDate));

		TextView sunTransitTextTime = (TextView) findViewById(R.id.sunTransitTextTime);
		sunTransitTextTime.setText(Localized.localizedTime(astroData.get(2), MainActivity.this, rightNowDate));

		TextView sunsetTextTime = (TextView) findViewById(R.id.sunsetTextTime);
		sunsetTextTime.setText(Localized.localizedTime(astroData.get(3), MainActivity.this, rightNowDate));

		TextView civilTwilightTextTime = (TextView) findViewById(R.id.civilTwilightTextTime);
		civilTwilightTextTime.setText(Localized.localizedTime(astroData.get(4), MainActivity.this, rightNowDate));

		TextView moonRiseTime = (TextView) findViewById(R.id.moonRiseTime);
		moonRiseTime.setText(Localized.localizedTime(astroData.get(5), MainActivity.this, rightNowDate));

		TextView moonTransitTextTime = (TextView) findViewById(R.id.moonTransitTextTime);
		moonTransitTextTime.setText(Localized.localizedTime(astroData.get(6), MainActivity.this, rightNowDate));

		TextView moonsetTextTime = (TextView) findViewById(R.id.moonsetTextTime);
		moonsetTextTime.setText(Localized.localizedTime(astroData.get(7), MainActivity.this, rightNowDate));

		// try catch the next astroData 8 and 9 indexes, they throw exceptions if null
		try {
			TextView moonDescription1 = (TextView) findViewById(R.id.moonDescription1);
			if (astroData.get(8) != null && !astroData.get(8).contains("<a href=")) {
				moonDescription1.setText(astroData.get(8));
			} else {
				moonDescription1.setText("");
			}
			TextView moonDescription2 = (TextView) findViewById(R.id.moonDescription2);
			if (astroData.get(9) != null && !astroData.get(9).contains("<a href=")) {
				moonDescription2.setText(astroData.get(9));
			} else {
				moonDescription2.setText("");
			}
		} catch (Exception e) {
		}

		// update location provider details
		TextView locationProviderValue = (TextView) findViewById(R.id.locationProviderValue);
		locationProviderValue.setText(currentLocation.getProvider());

		TextView locationLatValue = (TextView) findViewById(R.id.locationLatValue);
		Double lat = currentLocation.getLatitude();
		locationLatValue.setText(Double.toString(lat));

		TextView locationLonValue = (TextView) findViewById(R.id.locationLonValue);
		Double lon = currentLocation.getLongitude();
		locationLonValue.setText(Double.toString(lon));

		TextView locationAccuracyValue = (TextView) findViewById(R.id.locationAccuracyValue);
		Float meters = currentLocation.getAccuracy();
		locationAccuracyValue.setText(Float.toString(meters));

		// update amount of daylight / darkness time
		SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yyyy HH:mm", java.util.Locale.getDefault());
		String endTwilight = rightNowDate + " " + astroData.get(4);
		Date endTwilightDate = null;
		long endTwilightTimeStamp = 0;
		try {
			endTwilightDate = (Date) formatter.parse(endTwilight);
			endTwilightTimeStamp = endTwilightDate.getTime();
		} catch (ParseException e) {
		}
		String beginTwilight = rightNowDate + " " + astroData.get(0);
		Date beginTwilightDate = null;
		long beginTwilightTimeStamp = 0;
		try {
			beginTwilightDate = (Date) formatter.parse(beginTwilight);
			beginTwilightTimeStamp = beginTwilightDate.getTime();
		} catch (ParseException e) {
		}

		// get the hours and minutes of daylight and darkness for the current day
		try {
			long daylightTime = endTwilightTimeStamp - beginTwilightTimeStamp;
			int dayLightHours = (int) ((daylightTime / (1000 * 60 * 60)) % 24);
			int dayLightMinutes = (int) ((daylightTime / (1000 * 60)) % 60);
			TextView amountLightValue = (TextView) findViewById(R.id.amountLightValue);
			amountLightValue.setText(Integer.toString(dayLightHours) + " hours " + Integer.toString(dayLightMinutes) + " min");
			long darknessTime = 86400000 - daylightTime;
			int darknessHours = (int) ((darknessTime / (1000 * 60 * 60)) % 24);
			int darknessMinutes = (int) ((darknessTime / (1000 * 60)) % 60);
			TextView amountDarknessValue = (TextView) findViewById(R.id.amountDarknessValue);
			amountDarknessValue.setText(Integer.toString(darknessHours) + " hours " + Integer.toString(darknessMinutes) + " min");
		} catch (Exception e) {
		}
	}
}
