package com.kevinhinds.riseandset;

import android.support.v7.app.ActionBarActivity;
import android.text.format.DateFormat;
import android.util.TypedValue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class MainActivity extends ActionBarActivity {

	protected Location currentLocation;
	protected String lat_deg;
	protected String lat_min;
	protected String lon_deg;
	protected String lon_min;
	protected String lat_sign;
	protected String lon_sign;
	protected String tz_sign;
	protected String tz;
	protected int year;
	protected int month;
	protected int day;
	protected java.text.DateFormat dateFormat;
	protected java.text.DateFormat timeFormat;
	protected String dateFormattedLocally;
	protected String timeFormattedLocally;
	protected String rightNowDate;
	protected String completeForecast;
	protected Drawable earthViewImage;
	protected Drawable sunViewImage;
	protected Drawable moonViewImage;
	protected Document html;
	public ArrayList<String> astroData;
	private URL usnoURL;
	private URL forcastURL;
	protected String currentLanguageCode;
	protected String currentCountryCode;
	protected boolean useCelsius = true;
	protected String finalAddress;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// get current language code
		currentLanguageCode = getResources().getConfiguration().locale.getLanguage();
		currentCountryCode = getResources().getConfiguration().locale.getCountry();

		// set the variable if we need to NOT use celsius
		String[] nonCelsiusCountries = { "BS", "BZ", "KY", "PW", "US", "PR", "GU", "VI" };
		if (Arrays.asList(nonCelsiusCountries).contains(currentCountryCode)) {
			useCelsius = false;
		}

		// handle the spinner for the earth view changer
		Spinner earthViewsSpinner = (Spinner) findViewById(R.id.earthViewsSpinner);
		earthViewsSpinner.setOnItemSelectedListener(new CustomOnItemSelectedListenerEarth());

		// handle the spinner for the earth view changer
		Spinner sunViewsSpinner = (Spinner) findViewById(R.id.sunViewsSpinner);
		sunViewsSpinner.setOnItemSelectedListener(new CustomOnItemSelectedListenerSun());

		// get current date time / location and update the UI with it
		getCurrentDateTime();
		getCurrentLocation();

		// get the current moon
		Date currentDate = new java.util.Date();
		long timestamp = currentDate.getTime();
		URL moonURL = null;
		try {
			moonURL = new URL("http://space.kevinhinds.net/moon.jpg?" + Long.toString(timestamp));
		} catch (Exception e) {
		}
		new GetMoonImageTask().execute(moonURL);
	}

	/**
	 * get current date and time, additionally get the locally formatted date and time to display
	 */
	protected void getCurrentDateTime() {

		dateFormat = android.text.format.DateFormat.getDateFormat(this);
		timeFormat = android.text.format.DateFormat.getTimeFormat(this);
		rightNowDate = (DateFormat.format("MM-dd-yyyy", new java.util.Date()).toString());

		String rightNow = (DateFormat.format("MM-dd-yyyy HH:mm", new java.util.Date()).toString());
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm", java.util.Locale.getDefault());
		Date rightNowDateTime = null;
		try {
			rightNowDateTime = sdf.parse(rightNow);
		} catch (Exception e) {
		}
		dateFormattedLocally = dateFormat.format(rightNowDateTime);
		timeFormattedLocally = timeFormat.format(rightNowDateTime);

		// update the date time field on the UI
		TextView currentDateTime = (TextView) findViewById(R.id.currentDateTime);
		currentDateTime.setText(dateFormattedLocally + " " + timeFormattedLocally);
	}

	/**
	 * get current location of the user
	 */
	protected void getCurrentLocation() {

		// Acquire a reference to the system Location Manager
		LocationManager locationManager = (LocationManager) this.getSystemService(MainActivity.LOCATION_SERVICE);

		// Define a listener that responds to location updates
		LocationListener locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				currentLocation = location;
				try {
					updateUIWithLocationFound();
				} catch (Exception e) {
				}
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onProviderDisabled(String provider) {
			}
		};

		// Register the listener with the Location Manager to receive location updates
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
		currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		locationManager.removeUpdates(locationListener);
		try {
			updateUIWithLocationFound();
		} catch (Exception e) {
		}
	}

	/**
	 * update the UI with location found by querying USNO website
	 */
	protected void updateUIWithLocationFound() {

		String latitudeInfo = Location.convert(currentLocation.getLatitude(), Location.FORMAT_MINUTES);
		String longitudeInfo = Location.convert(currentLocation.getLongitude(), Location.FORMAT_MINUTES);
		String[] latitudeInfoParts = latitudeInfo.split(":");
		String[] longitudeInfoParts = longitudeInfo.split(":");

		// get latitude degrees with sign
		lat_sign = "1";
		int latDegrees = Integer.parseInt(latitudeInfoParts[0]);
		if (latDegrees < 0) {
			lat_sign = "-1";
			latDegrees = Math.abs(latDegrees);
		}
		lat_deg = Integer.toString(latDegrees);

		// get latitude minutes
		int latMinutes = (int) Math.floor(Double.parseDouble(latitudeInfoParts[1]));
		lat_min = Integer.toString(latMinutes);

		// get longitude degrees with sign
		lon_sign = "1";
		int lonDegrees = Integer.parseInt(longitudeInfoParts[0]);
		if (lonDegrees < 0) {
			lon_sign = "-1";
			lonDegrees = Math.abs(lonDegrees);
		}
		lon_deg = Integer.toString(lonDegrees);

		// get longitude minutes
		int lonMinutes = (int) Math.floor(Double.parseDouble(longitudeInfoParts[1]));
		lon_min = Integer.toString(lonMinutes);

		TimeZone tzone = TimeZone.getDefault();
		Calendar cal = GregorianCalendar.getInstance(tzone);
		int offsetInMillis = tzone.getOffset(cal.getTimeInMillis());

		String offset = String.format("%02d", Math.abs(offsetInMillis / 3600000), Math.abs((offsetInMillis / 60000) % 60));
		int offsetAmount = Integer.parseInt(offset);
		tz = String.valueOf(offsetAmount);
		tz_sign = (offsetInMillis >= 0 ? "+" : "-");
		tz_sign = tz_sign + "1";

		// get current date
		year = cal.get(Calendar.YEAR);
		month = cal.get(Calendar.MONTH);
		month = month + 1;
		day = cal.get(Calendar.DAY_OF_MONTH);

		// get USNO URL and background task to obtain the data
		usnoURL = null;
		try {
			usnoURL = new URL(
					"http://aa.usno.navy.mil/rstt/onedaytable?ID=AA&year=" + Integer.toString(year) + "&month=" + Integer.toString(month) + "&day=" + Integer.toString(day) + "&place=&lon_sign="
							+ lon_sign + "&lon_deg=" + lon_deg + "&lon_min=" + lon_min + "&lat_sign=" + lat_sign + "&lat_deg=" + lat_deg + "&lat_min=" + lat_min + "&tz=" + tz + "&tz_sign=" + tz_sign);
		} catch (Exception e1) {
		}
		new GetAstroDataTask().execute(usnoURL);

		// get current forecast and estimated location
		forcastURL = null;
		try {

			forcastURL = new URL("http://weather.api.kevinhinds.net/?lat=" + currentLocation.getLatitude() + "&lon=" + currentLocation.getLongitude() + "&lang=" + currentLanguageCode);

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
		new GetWeatherDataTask().execute(forcastURL);

		// setup the spinner to have an onclick listener for the earth views selection
		getPlanetImageByName("moon");
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
				JSONObject weatherReader = new JSONObject(weatherOutput);
				JSONObject currently = (JSONObject) weatherReader.get("currently");
				String currentlySummary = (String) currently.get("summary");

				// temp and apparent temp
				completeForecast = "Temp: " + convertFahrenheitToCelcius(Double.parseDouble(currently.get("temperature").toString()));
				completeForecast = completeForecast + " / Feels Like: " + convertFahrenheitToCelcius(Double.parseDouble(currently.get("apparentTemperature").toString()));

				// humidity and dew point
				Double humidity = (double) Math.round(Double.parseDouble(currently.get("humidity").toString()) * 100);
				completeForecast = completeForecast + "\n\nHumidity: " + Double.toString(humidity) + "%";
				completeForecast = completeForecast + " / Dew Point: " + convertFahrenheitToCelcius(Double.parseDouble(currently.get("dewPoint").toString()));

				// wind speed and bearing
				completeForecast = completeForecast + "\n\nWind Speed: " + convertMPHtoCelcius(Double.parseDouble(currently.get("windSpeed").toString()));
				completeForecast = completeForecast + " / Bearing: " + convertDegreesToDirection(Double.parseDouble(currently.get("windBearing").toString()));

				// cloud cover
				Double cloudCover = (double) Math.round(Double.parseDouble(currently.get("cloudCover").toString()) * 100);
				completeForecast = completeForecast + " \n\nCloud Cover: " + Double.toString(cloudCover) + "%";

				// pressure and ozone
				completeForecast = completeForecast + "\n\nAir Pressure: " + Double.parseDouble(currently.get("pressure").toString());
				completeForecast = completeForecast + " / Ozone (O3): " + Double.parseDouble(currently.get("ozone").toString());

				// get minutely summary, catch if missing
				String minutelySummary = "";
				try {
					JSONObject minutely = (JSONObject) weatherReader.get("minutely");
					minutelySummary = (String) minutely.get("summary");
				} catch (Exception e) {
				}

				// get hourly summary, catch if missing
				String hourlySummary = "";
				try {
					JSONObject hourly = (JSONObject) weatherReader.get("hourly");
					hourlySummary = (String) hourly.get("summary");
				} catch (Exception e) {
				}

				// get daily summary, catch if missing
				String dailySummary = "";
				try {
					JSONObject daily = (JSONObject) weatherReader.get("daily");
					dailySummary = (String) daily.get("summary");
				} catch (Exception e) {
				}
				completeForecast = completeForecast + "\n\n" + currentlySummary + " \n\n" + minutelySummary + " \n\n" + hourlySummary + " \n\n" + dailySummary;

			} catch (Exception e) {
				String kevin = "kevin";
			}
			return null;
		}

		/**
		 * convert the current bearning angle to compass direction
		 * 
		 * @param windBearing
		 * @return
		 */
		private String convertDegreesToDirection(Double windBearing) {
			if (windBearing > 348.75 || windBearing <= 11.25) {
				return "N";
			}
			if (windBearing > 11.25 && windBearing <= 33.75) {
				return "NNE";
			}
			if (windBearing > 33.75 && windBearing <= 56.25) {
				return "NE";
			}
			if (windBearing > 56.25 && windBearing <= 78.75) {
				return "ENE";
			}
			if (windBearing > 78.75 && windBearing <= 101.25) {
				return "E";
			}
			if (windBearing > 101.25 && windBearing <= 123.75) {
				return "ESE";
			}
			if (windBearing > 123.75 && windBearing <= 146.25) {
				return "SE";
			}
			if (windBearing > 146.25 && windBearing <= 168.75) {
				return "SSE";
			}
			if (windBearing > 168.75 && windBearing <= 191.25) {
				return "S";
			}
			if (windBearing > 191.25 && windBearing <= 213.75) {
				return "SSW";
			}
			if (windBearing > 213.75 && windBearing <= 236.25) {
				return "SW";
			}
			if (windBearing > 236.25 && windBearing <= 258.75) {
				return "WSW";
			}
			if (windBearing > 258.75 && windBearing <= 281.25) {
				return "W";
			}
			if (windBearing > 281.25 && windBearing <= 303.75) {
				return "WNW";
			}
			if (windBearing > 303.75 && windBearing <= 326.25) {
				return "NW";
			}
			if (windBearing > 326.25 && windBearing <= 348.75) {
				return "NNW";
			}
			return null;
		}

		// Converts to celcius unless the flag is set otherwise
		private String convertMPHtoCelcius(Double mph) {
			if (!useCelsius) {
				return Double.toString(Math.round(mph)) + " mph";
			}
			return Double.toString(Math.round(mph * 1.609344)) + " kph";
		}

		// Converts to celcius unless the flag is set otherwise
		private String convertFahrenheitToCelcius(Double fahrenheit) {
			if (!useCelsius) {
				return Double.toString(Math.round(fahrenheit)) + " *F";
			}
			return Double.toString(Math.round(((fahrenheit - 32) * 5 / 9))) + " *C";
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
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
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

			// get USNO data from Jsoup
			try {
				html = Jsoup.connect(params[0].toString()).get();
			} catch (Exception e) {
				e.printStackTrace();
			}

			// create array list of astroData for
			astroData = new ArrayList<String>();
			Element table = html.select("table").get(1);
			Elements rows = table.select("tr");
			for (int i = 1; i < rows.size(); i++) {
				Element row = rows.get(i);
				Elements cols = row.select("td");
				try {
					astroData.add(cols.get(1).html());
				} catch (Exception e) {
				}
			}

			Elements paragraphTag = html.select("p");
			astroData.add(paragraphTag.get(1).html());
			astroData.add(paragraphTag.get(2).html());
			return null;
		}
	}

	/**
	 * convert a raw time from USNO to a localized time per the user's personal locale settings
	 * 
	 * @param rawTime
	 * @return
	 */
	public String localizedTime(String rawTime) {
		String rawTimeConverted = rightNowDate + " " + rawTime;
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm", java.util.Locale.getDefault());
		Date rightNowDateTime = null;
		try {
			rightNowDateTime = sdf.parse(rawTimeConverted);
		} catch (Exception e) {
		}
		rawTime = timeFormat.format(rightNowDateTime);
		return rawTime;
	}

	/**
	 * update all the time fields on the UI with the newly found USNO data
	 */
	public void updateUSNOFields() {

		TextView sunBeginCivilTwilightTextTime = (TextView) findViewById(R.id.sunBeginCivilTwilightTextTime);
		sunBeginCivilTwilightTextTime.setText(localizedTime(astroData.get(0)));

		TextView sunriseTextTime = (TextView) findViewById(R.id.sunriseTextTime);
		sunriseTextTime.setText(localizedTime(astroData.get(1)));

		TextView sunTransitTextTime = (TextView) findViewById(R.id.sunTransitTextTime);
		sunTransitTextTime.setText(localizedTime(astroData.get(2)));

		TextView sunsetTextTime = (TextView) findViewById(R.id.sunsetTextTime);
		sunsetTextTime.setText(localizedTime(astroData.get(3)));

		TextView civilTwilightTextTime = (TextView) findViewById(R.id.civilTwilightTextTime);
		civilTwilightTextTime.setText(localizedTime(astroData.get(4)));

		TextView moonRiseTime = (TextView) findViewById(R.id.moonRiseTime);
		moonRiseTime.setText(localizedTime(astroData.get(5)));

		TextView moonTransitTextTime = (TextView) findViewById(R.id.moonTransitTextTime);
		moonTransitTextTime.setText(localizedTime(astroData.get(6)));

		TextView moonsetTextTime = (TextView) findViewById(R.id.moonsetTextTime);
		moonsetTextTime.setText(localizedTime(astroData.get(7)));

		// try catch the next astroData 9 and 10 indexes, they throw exceptions if null
		try {
			TextView moonDescription1 = (TextView) findViewById(R.id.moonDescription1);
			if (astroData.get(9) != null && !astroData.get(9).contains("<a href=")) {
				moonDescription1.setText(astroData.get(9));
			} else {
				moonDescription1.setText("");
			}
			TextView moonDescription2 = (TextView) findViewById(R.id.moonDescription2);
			if (astroData.get(10) != null && !astroData.get(10).contains("<a href=")) {
				moonDescription2.setText(astroData.get(10));
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