package karthik.WeatherApp;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

class ImageDownloaderTask extends AsyncTask<Void, Void, Void> {
	
	Map<String, Bitmap> iconCache;
	Map<String, List<ImageView>> iconURLMap;
	
	public ImageDownloaderTask(String url, Map<String, Bitmap> cache, ImageView imgView) {
		iconURLMap = new HashMap<String, List<ImageView>>();
		List<ImageView> ivList = new ArrayList<ImageView>();
		ivList.add(imgView);
		iconURLMap.put(url, ivList);
		iconCache = cache;
	}
	
	public ImageDownloaderTask(Map<String, List<ImageView>> iconURLMap, Map<String, Bitmap> cache) {
		this.iconURLMap = iconURLMap;
		iconCache = cache;
	}
	
	public Void doInBackground(Void... params){
        Bitmap weatherIcon;
        HttpURLConnection con;
        
        try{
        	for (String iconURL:iconURLMap.keySet()){
		        con = (HttpURLConnection) (new URL(iconURL).openConnection());
		        con.connect(); // optional line
		        BufferedInputStream in = new BufferedInputStream(con.getInputStream());
		        weatherIcon = BitmapFactory.decodeStream(in);
		
		        in.close();
		        con.disconnect();
		        iconCache.put(iconURL, weatherIcon);	
        	}
        }
        catch(IOException ioe){
        	Log.e("WeatherApp - ImageDownloaderTask", "IOException when downloading icon image " + ioe.getMessage());
        }
        return null;
	}
	
	public void onPostExecute(Void v){
		// set image view in UI to icon if a bitmap obj was downloaded successfully
		for (String iconURL:iconURLMap.keySet()){
			List<ImageView> ivObjs = iconURLMap.get(iconURL);
			for (ImageView iv:ivObjs)
				if (iv != null)
					iv.setImageBitmap(iconCache.get(iconURL));
		}
/*		if (bmp != null && imgViewObj != null){
			imgViewObj.setImageBitmap(bmp);
	}*/
	}
}
