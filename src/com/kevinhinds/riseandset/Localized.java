package com.kevinhinds.riseandset;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import android.content.Context;
import android.text.format.DateFormat;

public class Localized {

	/**
	 * get current date and time, additionally get the locally formatted date and time to display
	 */
	public static String getCurrentDateTime(Context context) {

		java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
		java.text.DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);

		String rightNow = (DateFormat.format("MM-dd-yyyy HH:mm", new java.util.Date()).toString());
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm", java.util.Locale.getDefault());
		Date rightNowDateTime = null;
		try {
			rightNowDateTime = sdf.parse(rightNow);
		} catch (Exception e) {
		}
		return dateFormat.format(rightNowDateTime) + " " + timeFormat.format(rightNowDateTime);
	}

	/**
	 * convert a raw time from USNO to a localized time per the user's personal locale settings
	 * 
	 * @param rawTime
	 * @return
	 */
	public static String localizedTime(String rawTime, Context context, String rightNowDate) {
		java.text.DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);
		String rawTimeConverted = rightNowDate + " " + rawTime;
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm", java.util.Locale.getDefault());
		Date rightNowDateTime = null;
		try {
			rightNowDateTime = sdf.parse(rawTimeConverted);
		} catch (Exception e) {
		}
		rawTime = timeFormat.format(rightNowDateTime);
		String[] timeDetails = rawTime.split(":");
		String timeDetailsHour = timeDetails[0];
		if (timeDetailsHour.length() == 1) {
			rawTime = " " + rawTime;
		}
		return rawTime;
	}

	/**
	 * use celsius for all countries execpt for the last hold outs by country code
	 * 
	 * @param currentCountryCode
	 * @return
	 */
	public static boolean useCelsius(String currentCountryCode) {
		String[] nonCelsiusCountries = { "BS", "BZ", "KY", "PW", "US", "PR", "GU", "VI" };
		if (Arrays.asList(nonCelsiusCountries).contains(currentCountryCode)) {
			return false;
		}
		return true;
	}

	// Converts to celcius unless the flag is set otherwise
	public static String convertMPHtoCelcius(Double mph, boolean useCelsius) {
		if (!useCelsius) {
			return Double.toString(Math.round(mph)) + " mph";
		}
		return Double.toString(Math.round(mph * 1.609344)) + " kph";
	}

	// Converts to celcius unless the flag is set otherwise
	public static String convertFahrenheitToCelcius(Double fahrenheit, boolean useCelsius) {
		if (!useCelsius) {
			return Double.toString(Math.round(fahrenheit)) + " *F";
		}
		return Double.toString(Math.round(((fahrenheit - 32) * 5 / 9))) + " *C";
	}

	/**
	 * convert the current bearning angle to compass direction
	 * 
	 * @param windBearing
	 * @return
	 */
	public static String convertDegreesToDirection(Double windBearing) {
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
}