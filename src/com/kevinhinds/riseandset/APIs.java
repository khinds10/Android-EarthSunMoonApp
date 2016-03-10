package com.kevinhinds.riseandset;

import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.location.Location;

/**
 * API library to get URLs and parse results
 * 
 * @author khinds
 */
public class APIs {

	/**
	 * by current location found get the forecast URL for it for kevinhinds.net weather api
	 * 
	 * @param currentLocation
	 * @return
	 */
	public static URL getForecastURLByLocation(Location currentLocation, String currentLanguageCode) {
		URL forcastURL = null;
		try {
			forcastURL = new URL("http://weather.api.kevinhinds.net/?lat=" + currentLocation.getLatitude() + "&lon=" + currentLocation.getLongitude() + "&lang=" + currentLanguageCode);
		} catch (Exception e) {
		}
		return forcastURL;
	}

	/**
	 * 
	 * @param weatherOutput
	 * @param useCelsius
	 * @return
	 */
	public static String getForecastFromAPIData(String weatherOutput, boolean useCelsius) {
		String completeForecast = "";
		try {
			JSONObject weatherReader = new JSONObject(weatherOutput);
			JSONObject currently = (JSONObject) weatherReader.get("currently");
			String currentlySummary = (String) currently.get("summary");

			// temp and apparent temp
			completeForecast = "Temp: " + Localized.convertFahrenheitToCelcius(Double.parseDouble(currently.get("temperature").toString()), useCelsius);
			completeForecast = completeForecast + " / Feels Like: " + Localized.convertFahrenheitToCelcius(Double.parseDouble(currently.get("apparentTemperature").toString()), useCelsius);

			// humidity and dew point
			Double humidity = (double) Math.round(Double.parseDouble(currently.get("humidity").toString()) * 100);
			completeForecast = completeForecast + "\n\nHumidity: " + Double.toString(humidity) + "%";
			completeForecast = completeForecast + " / Dew Point: " + Localized.convertFahrenheitToCelcius(Double.parseDouble(currently.get("dewPoint").toString()), useCelsius);

			// wind speed and bearing
			completeForecast = completeForecast + "\n\nWind Speed: " + Localized.convertMPHtoCelcius(Double.parseDouble(currently.get("windSpeed").toString()), useCelsius);
			completeForecast = completeForecast + " / Bearing: " + Localized.convertDegreesToDirection(Double.parseDouble(currently.get("windBearing").toString()));

			// cloud cover
			Double cloudCover = (double) Math.round(Double.parseDouble(currently.get("cloudCover").toString()) * 100);
			completeForecast = completeForecast + " \n\nCloud Cover: " + Double.toString(cloudCover) + "%";

			// pressure and ozone
			completeForecast = completeForecast + "\n\nAir Pressure: " + Double.parseDouble(currently.get("pressure").toString());
			completeForecast = completeForecast + " / Ozone (O3): " + Double.parseDouble(currently.get("ozone").toString());

			// get minutely summary, catch if missing
			String minutelySummary = "";
			JSONObject minutely = (JSONObject) weatherReader.get("minutely");
			minutelySummary = (String) minutely.get("summary");

			// get hourly summary, catch if missing
			String hourlySummary = "";
			JSONObject hourly = (JSONObject) weatherReader.get("hourly");
			hourlySummary = (String) hourly.get("summary");

			// get daily summary, catch if missing
			String dailySummary = "";
			JSONObject daily = (JSONObject) weatherReader.get("daily");
			dailySummary = (String) daily.get("summary");
			completeForecast = completeForecast + "\n\n" + currentlySummary + " - " + minutelySummary + " - " + hourlySummary + "  - " + dailySummary;
		} catch (Exception e) {
		}
		return completeForecast;
	}

	/**
	 * by current location generate a USNO url for obtaining earth / sun and moon data
	 * 
	 * @param currentLocation
	 * @return
	 */
	public static URL getUSNOURLByLocation(Location currentLocation) {

		String lat_deg, lat_min, lon_deg, lon_min, lat_sign, lon_sign, tz_sign, tz;
		int year, month, day;
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

		String offset = String.format(Locale.US, "%02d", Math.abs(offsetInMillis / 3600000), Math.abs((offsetInMillis / 60000) % 60));
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
		URL usnoURL = null;
		try {
			usnoURL = new URL(
					"http://aa.usno.navy.mil/rstt/onedaytable?ID=AA&year=" + Integer.toString(year) + "&month=" + Integer.toString(month) + "&day=" + Integer.toString(day) + "&place=&lon_sign="
							+ lon_sign + "&lon_deg=" + lon_deg + "&lon_min=" + lon_min + "&lat_sign=" + lat_sign + "&lat_deg=" + lat_deg + "&lat_min=" + lat_min + "&tz=" + tz + "&tz_sign=" + tz_sign);
		} catch (Exception e1) {
		}
		return usnoURL;
	}

	/**
	 * parse USNO data to array list by USNO URL
	 * 
	 * @param params
	 * @return
	 */
	public static ArrayList<String> parseUSNODataByURL(URL... params) {
		Document html = null;
		ArrayList<String> astroData = null;
		try {
			html = Jsoup.connect(params[0].toString()).get();
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
		} catch (Exception e) {
		}
		return astroData;
	}

}