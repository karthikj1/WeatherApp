package karthik.WeatherApp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.w3c.dom.Text;

/**
 * Created by karthik on 4/8/2014.
 */
class HourlyItemLayout extends LinearLayout {

     LinearLayout ll1;
    LinearLayout tempView;
    LayoutParams llParams;
    TextView time, weather, temp, pop;
    ImageView raindrop, icon;

     HourlyItemLayout(Context con, LinearLayout hourlyItem)
    {
        super(con);

        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(hourlyItem.getLayoutParams());
        itemParams.setMargins(0,10,0,5);
        setLayoutParams(itemParams);
        setOrientation(LinearLayout.VERTICAL);


        ll1 = new LinearLayout(con);
        tempView = (LinearLayout) hourlyItem.getChildAt(0);

        llParams = (LinearLayout.LayoutParams) tempView.getLayoutParams();
        ll1.setLayoutParams(llParams);

        time = new TextView(con);
        weather = new TextView(con);
        temp = new TextView(con);

        time.setLayoutParams(tempView.getChildAt(0).getLayoutParams());
        weather.setLayoutParams(tempView.getChildAt(2).getLayoutParams());
        temp.setLayoutParams(tempView.getChildAt(3).getLayoutParams());
        setTextStyle(con,time);
        setTextStyle(con, temp);
        setTextStyle(con, weather);


        pop = new TextView(con);
        pop.setLayoutParams(tempView.getChildAt(5).getLayoutParams());
        setTextStyle(con, pop);

        raindrop = new ImageView(con);
        icon = new ImageView(con);

        raindrop.setLayoutParams(tempView.getChildAt(4).getLayoutParams());
        raindrop.setImageResource(R.drawable.raindrop);
        icon.setLayoutParams(tempView.getChildAt(1).getLayoutParams());


        ll1.addView(time);
        ll1.addView(icon);
        ll1.addView(weather);
        ll1.addView(temp);
        ll1.addView(raindrop);
        ll1.addView(pop);

        addView(ll1);

        View lineSeparator = new View(con);
        lineSeparator.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
        lineSeparator.setBackgroundColor(Color.DKGRAY);
        addView(lineSeparator);
    }

    private void setTextStyle(Context con, TextView tv)
    {
        tv.setTextAppearance(con, R.style.HourlyForecastTextStyle);

    }

   void setTime(String timeString)
   {

       time.setText(timeString + " ");
   }


    void setTemp(String temperature)
    {
     temp.setText(temperature + " ");
    }


    void setWeather(String weatherString)
    {

        weather.setText(weatherString + " ");
    }

    void setHourlyForecastImage(Bitmap bmp){
            icon.setImageBitmap(bmp);
        }

    void setRainProb(String rainProb){
        pop.setText(rainProb + " ");
    }
    
    ImageView getIconIV(){
    	return icon;
    }
}
