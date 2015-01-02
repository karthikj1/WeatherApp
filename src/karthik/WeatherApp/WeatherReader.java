
package karthik.WeatherApp;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
import java.util.Calendar;

import android.util.Log;
import karthik.json.*;

class WeatherReader {

	static final String APIKEY = Config.APIKEY;
	
    static final String forecastURLAddress = "http://api.wunderground.com/api/" + APIKEY + "/forecast10day/q/";
    static final String URLAddress = "http://api.wunderground.com/api/" + APIKEY + "/conditions/q/";
    static final String hourlyURLAddress = "http://api.wunderground.com/api/" + APIKEY + "/hourly10day/q/";
    static final String geoLookupAddress = "http://api.wunderground.com/api/" + APIKEY + "/geolookup/q/";
    
    static final String newLine = System.getProperty("line.separator");

    static final String TAG = "WeatherReader";
    
    public WeatherReader()
    {            }

    public static void getParsedWeatherData(MainWeatherObject ParsedJSONData, final KJ_JSONObject rawData)
            throws KJ_JSONException, IOException
    {
        // if loc is not found, returns object with fullCityName name of null, otherwise returns parsed data
        KJ_JSONObject displayLoc;

        displayLoc = rawData.getJSONObject("current_observation").getJSONObject("display_location");
        ParsedJSONData.temperatureObj.temp = rawData.getDouble("temp_c");
        ParsedJSONData.temperatureObj.humidity = rawData.getString("relative_humidity");
        ParsedJSONData.temperatureObj.feelslike = rawData.getString("feelslike_string");
        ParsedJSONData.setWeatherObj(rawData.getString("weather"), rawData.getString("icon_url"),0);
        ParsedJSONData.fullCityName = displayLoc.getString("full");
        ParsedJSONData.country = displayLoc.getString("country_iso3166");
        ParsedJSONData.setCoord(displayLoc.getString("latitude"), displayLoc.getString("longitude"));

      //  Log.i("Weather Reader", displayLoc.toString());

        return;

    }

    public static KJ_JSONObject getJSONData(URL address) throws IOException, KJ_JSONException
    {
        // gets JSON data from server and returns KJ_JSONObject with the information returned
        String line;
        HttpURLConnection con;
        StringBuilder data = new StringBuilder("");

        con = (HttpURLConnection) (address.openConnection());
        con.connect(); // optional line
        BufferedReader in = new BufferedReader(new InputStreamReader(
                con.getInputStream()));
        while((line = in.readLine()) != null)
            data.append(line + newLine);

        in.close();
        con.disconnect();
        return new KJ_JSONObject(data.toString());
    }

    public static KJ_JSONObject getWeatherData(String displayCity) throws IOException, KJ_JSONException {

        String address = URLAddress.concat(removeWhiteSpace(displayCity)).concat(".json");
        return getJSONData(new URL(address));
    }

    public static void getForecastArray(MainWeatherObject[] forecastArray, String location, int numDays)
            throws IOException, NumberFormatException, KJ_JSONException
    {
        KJ_JSONObject tempData, cdrData;
        KJ_JSONArray RawForecastDataArray;
        int ctr;

        URL tgtURL = new URL(forecastURLAddress.concat(removeWhiteSpace(location)).concat(".json"));

        Log.i("weather reader", "Making JSON forecast request for " + tgtURL.toString());

        tempData = getJSONData(tgtURL);
        tempData = tempData.getJSONObject("forecast").getJSONObject("simpleforecast");
        RawForecastDataArray =  tempData.getJSONArray("forecastday"); // get the list of forecasts
        Log.i("Weather Reader", "Loaded forecasts");

        // weather underground always returns 10 days so below is used to trim that data as requested to return numDays forecasts
        numDays = (numDays <= 10) ? numDays : 10;

        for(ctr = 0; ctr < numDays; ctr++)
        {
            forecastArray[ctr] = new MainWeatherObject(location, 0);

            tempData = RawForecastDataArray.getJSONObject(ctr);
            cdrData = tempData.getJSONObject("date");

            forecastArray[ctr].temperatureObj.temp_max = Double.parseDouble(tempData.getJSONObject("high").getString("celsius"));
            forecastArray[ctr].temperatureObj.temp_min = Double.parseDouble(tempData.getJSONObject("low").getString("celsius"));
            forecastArray[ctr].setForecastDate(cdrData.getInt("year"), cdrData.getInt("month"), cdrData.getInt("day")
                    ,cdrData.getString("weekday_short"));
            // some weird idiosyncrasy with how the DAY_OF_WEEK field works in Calendar object so we just
            // read the day of week from the JSON
            forecastArray[ctr].setWeatherObj(tempData.getString("conditions"), tempData.getString("icon_url"), tempData.getInt("pop"));

        }
        return;

    }

    public static void getHourlyForecastArray(MainWeatherObject[][] forecastArray, String location, int numDays)
            throws IOException, NumberFormatException, KJ_JSONException
    {
        KJ_JSONObject tempData;
        KJ_JSONArray RawForecastDataArray;
        int hourCtr, dayCtr, hourLoc;

        URL tgtURL = new URL(hourlyURLAddress.concat(removeWhiteSpace(location)).concat(".json"));
        Log.i("weather reader", "Making JSON hourly forecast request for " + tgtURL.toString());

        tempData = getJSONData(tgtURL);
        RawForecastDataArray =  tempData.getJSONArray("hourly_forecast"); // get the list of forecasts
        Log.i("Weather Reader", "Loaded hourly forecasts");

        // weather underground always returns 240 hours so below is used to trim that data as requested to return numDays forecasts
        numDays = (numDays <= 10) ? numDays : 10;
        dayCtr = hourCtr = 0;

        while(dayCtr < numDays)
        {
            MainWeatherObject tmpObj = getSingleHourlyObject(RawForecastDataArray, hourCtr++);
            int hourofDay = tmpObj.getForecastDate().get(Calendar.HOUR_OF_DAY);
            forecastArray[hourofDay][dayCtr] = tmpObj;

            if(hourofDay == 23)   // need to roll to next day
                dayCtr++;
        }
        return;

    }

    public static String getGeoLookupCity(double latitude, double longitude)
    	throws IOException, KJ_JSONException{
    // use WeatherUnderground API to do Geolookup and find nearest city based on 
    // latitude and longitude. Returns "" if nothing found
    	String city;
    	String address = geoLookupAddress + latitude + "," + longitude +".json";
    	
    	Log.i(TAG, "Making JSON Query to " + address);
    	KJ_JSONObject geoLookupData = getJSONData(new URL(address));
    	KJ_JSONObject locData = geoLookupData.getJSONObject("response");
    	
    	if(locData.has("error")){
			Log.i(TAG, locData.getJSONObject("error")
					.getString("description") + " while performing GeoLookup");
			return "";
    	}
    	locData = geoLookupData.getJSONObject("location");
    	// get city and state if in US, otherwise get city and country
    	if (locData.getString("country_iso3166").equalsIgnoreCase("US")) 
    		city = locData.getString("city") + "," + locData.getString("state");
    	else
    		city = locData.getString("city") + "," + locData.getString("country");
    	
    	return city;
    }
    
    private static MainWeatherObject getSingleHourlyObject(KJ_JSONArray rawArrayData, int ctr) throws KJ_JSONException
    {
        MainWeatherObject hourObj = new MainWeatherObject("",0);
        KJ_JSONObject tempData = rawArrayData.getJSONObject(ctr);
        KJ_JSONObject cdrData = tempData.getJSONObject("FCTTIME");

        hourObj.temperatureObj.temp = Double.parseDouble(tempData.getJSONObject("temp").getString("metric"));
        hourObj.setForecastDate(cdrData.getString("year"), cdrData.getString("mon"),
                cdrData.getString("mday"), cdrData.getString("weekday_name_abbrev"),
                cdrData.getString("hour_padded"), cdrData.getString("min"));
        // some weird idiosyncrasy with how the DAY_OF_WEEK field works in Calendar object so we just
        // read the day of week from the JSON

        hourObj.setWeatherObj(tempData.getString("condition"), tempData.getString("icon_url"),
                Integer.parseInt(tempData.getString("pop")));


        return hourObj;
    }

    private static String removeWhiteSpace(String loc)
    {
    // replaces whitespace with underscore since Weather Underground API 
    //	doesn't seem to like whitespace
    	
/*        StringBuffer trimmedLoc = new StringBuffer("");

        for(int i = 0; i < loc.length(); i++)
            if(!Character.isWhitespace(loc.charAt(i)))
                trimmedLoc.append(loc.charAt(i));
        return trimmedLoc.toString();
*/
    	return loc.replaceAll("\\s+", "_");
    }

}