package karthik.WeatherApp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;

class CoordObject{
    double longitude, latitude;

    public CoordObject(){}
}


class WeatherObject
{
    String mainWeatherDesc = "Empty", detailedWeatherDesc, iconURL;
    int rainProb;

    WeatherObject()
    {}
}
class TemperatureObject {
    public double temp, temp_min = 0, temp_max = 0;
    public String humidity, feelslike;
    public long pressure;

    TemperatureObject(double temperature)
    {
        temp = temperature;
    }
}

class MainWeatherObject
{
    TemperatureObject temperatureObj;
    WeatherObject weatherObj;
    CoordObject coord;
    String fullCityName;
    String country;
    Calendar forecastDate;
    String forecastDOW;
    private static HashMap <String, Bitmap> iconCache = new HashMap<String, Bitmap>();

    public MainWeatherObject(String loc, double temp)
    {
        fullCityName = loc;
        temperatureObj = new TemperatureObject(temp);
        coord = new CoordObject();
        weatherObj = new WeatherObject();
    }

    public double getTemperature()
    {
        return temperatureObj.temp;
    }

    public double getMinTemperature()
    {
        return temperatureObj.temp_min;
    }

    public double getMaxTemperature()
    {
        return temperatureObj.temp_max;
    }

    public String getFeelsLike() {return  temperatureObj.feelslike; }

    public String getHumidity() {return temperatureObj.humidity; }

    public int getRainProb() {return weatherObj.rainProb; }

    public String getMainWeatherDesc() {
        String desc = weatherObj.mainWeatherDesc.trim();
        String temp = "";

        if(desc.length() > 0) {
            temp = desc.substring(0, 1).toUpperCase();
            return temp.concat(desc.substring(1, desc.length()));
        }
        else
            return desc;
    }

    public String getDetailedWeatherDesc() { return weatherObj.detailedWeatherDesc;}

    public Bitmap getWeatherIcon() throws IOException
    {
        if(!iconCache.containsKey(weatherObj.iconURL))
            downloadImage();

         return iconCache.get(weatherObj.iconURL);

    }

    public void loadImage() throws IOException
    {
        if(iconCache.containsKey(weatherObj.iconURL))   //icon has already been loaded into cache
            return;
        downloadImage();
    }

    private void downloadImage() throws IOException
    {
        Bitmap weatherIcon;
        HttpURLConnection con;

        con = (HttpURLConnection) (new URL(weatherObj.iconURL)).openConnection();
        con.connect(); // optional line
        BufferedInputStream in = new BufferedInputStream(con.getInputStream());
        weatherIcon = BitmapFactory.decodeStream(in);

        in.close();
        con.disconnect();
        iconCache.put(weatherObj.iconURL, weatherIcon);
    }

    public void setLocation(String location) {fullCityName = location; }

    public void setWeatherObj(String mainDesc, String iconURL, int pop)
    {
        weatherObj.mainWeatherDesc = mainDesc;
        weatherObj.iconURL = iconURL;
        weatherObj.rainProb = pop;

        return;
    }

    public void setForecastDate(int year, int month, int date, String weekday)
    {
        forecastDate = Calendar.getInstance();
        forecastDate.set(year, month, date);
        forecastDOW = weekday;
    }

    public void setForecastDate(String year, String month, String date, String weekday, String hour, String min) throws NumberFormatException
    {
        forecastDate = Calendar.getInstance();
        forecastDate.set(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(date),
                        Integer.parseInt(hour), Integer.parseInt(min));

        forecastDOW = weekday;
    }

    public String getDOW()
    { // return day of week string
        return forecastDOW;
    }

    public Calendar getForecastDate()  { return forecastDate; }


    public void setCoord(String lat, String lon)
            // used for WeatherUnderground version
    {
        coord.latitude = Double.parseDouble(lat);
        coord.longitude = Double.parseDouble(lon);

    }

    public double getLongitude()
    {
        return coord.longitude;
    }

    public double getLatitude()
    {
        return coord.latitude;
    }
} // end MainWeatherObject
