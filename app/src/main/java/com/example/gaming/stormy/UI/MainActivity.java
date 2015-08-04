package com.example.gaming.stormy.UI;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gaming.stormy.DailyForecastActivity;
import com.example.gaming.stormy.R;
import com.example.gaming.stormy.weather.Current;
import com.example.gaming.stormy.weather.Day;
import com.example.gaming.stormy.weather.Forecast;
import com.example.gaming.stormy.weather.Hour;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.ButterKnife;
import butterknife.Bind;
import butterknife.OnClick;


public class MainActivity extends Activity {

    public static final String TAG = MainActivity.class.getSimpleName();//debugging TAG
    public static final String DAILY_FORECAST = "DAILY_FORECAST";//Tag for data packaging
    public static final String HOURLY_FORECAST = "HOURLY_FORECAST";//Tag for data packaging

    private Forecast mForecast;

    //View Injecting/ Binding data to elements in the Activity
    @Nullable @Bind(R.id.timeLabel) TextView mTimeLabel;
    @Nullable @Bind(R.id.temperatureLabel) TextView mTemperatureLabel;
    @Nullable @Bind(R.id.humidityValue) TextView mHumidityValue;
    @Nullable @Bind(R.id.precipValue) TextView mPrecipValue;
    @Nullable @Bind(R.id.summaryLabel) TextView mSummaryLabel;
    @Nullable @Bind(R.id.iconImageView) ImageView mIconImageView;
    @Nullable @Bind(R.id.refreshImageView) ImageView mRefreshImageView;
    @Nullable @Bind(R.id.progressBar) ProgressBar mProgressBar;

    private static Context context;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        context = getApplicationContext();

        mProgressBar.setVisibility(View.INVISIBLE);

        final double latitude = 21.3;
        final double longitude = -157.711;

        final GPSTracker gps = new GPSTracker(context);

        //Update button for weather info
        mRefreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getForecast(gps);
            }
        });

        getForecast(gps);
        Log.d(TAG, "Main UI code is run");

    }


    private void getForecast(GPSTracker gps) {
        //Forecast.IO implementation
        String apiKey = "7ebf6dd6ca410523ad5f1a9eaa5c8822";
        double latitude = gps.getLatitude();
        double longitude = gps.getLongitude();
        String forecastUrl = "https://api.forecast.io/forecast/" + apiKey + "/" + latitude + "," + longitude;

        if(isNetworkAvailable()) {
            toggleRefresh();
            //Data request via OKHttp
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(forecastUrl).build();

            Call call = client.newCall(request);
            call.enqueue(new Callback() {//calling in the background thread
                @Override
                public void onFailure(Request request, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    alertUserAboutError(); //Error message
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    try {
                        String jsonData = response.body().string();//Takes the JSONData from the response as a String
                        Log.v(TAG, jsonData);
                        if (response.isSuccessful()) {
                            mForecast = parseForecastDetails(jsonData); //Takes data from certain parts of the JSONArray
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();
                                }
                            });

                        } else {
                            alertUserAboutError();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    } catch (JSONException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    }
                }
            });
        }
        else
        {
            Toast.makeText(this, getString(R.string.network_unavailable_message), Toast.LENGTH_LONG).show();
        }
    }

    //Visual effect for the progress bar
    private void toggleRefresh() {
        if(mProgressBar.getVisibility() == View.INVISIBLE) {
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshImageView.setVisibility(View.INVISIBLE);
        }
        else {
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImageView.setVisibility(View.VISIBLE);
        }
    }

    //Sets variables that will be fed into the activity elements via InjectView/Bind
    private void updateDisplay() {
        Current current = mForecast.getCurrent();

        mTemperatureLabel.setText(current.getTemperature() + "");
        mTimeLabel.setText("At " + current.getFormattedTime() + " it will be");
        mHumidityValue.setText(current.getHumidity() + "");
        mPrecipValue.setText(current.getPrecipChance() + "%");
        mSummaryLabel.setText(current.getSummary() + "");
        Drawable drawable = getResources().getDrawable(current.getIconID());
        mIconImageView.setImageDrawable(drawable);
    }

    //Handles updating the current, hourly, and 7Day activities
    private Forecast parseForecastDetails(String jsonData) throws JSONException{
        Forecast forecast = new Forecast();

        forecast.setCurrent(getCurrentDetails(jsonData));
        forecast.setHourlyForecast(getHourlyForecast(jsonData));
        forecast.setDailyForecast(getDailyForecast(jsonData));


        return forecast;
    }

    //Creates an array of Day objects that each hold data for a specific day of the week
    //This is passed into multiple subViews in the 7Day forecast activity
    private Day[] getDailyForecast(String jsonData) throws JSONException {
        JSONObject forecast = new JSONObject(jsonData); //Initial pull from JSONData
        String timezone = forecast.getString("timezone");

        JSONObject daily = forecast.getJSONObject("daily"); //Under the daily category in JSONData
        JSONArray data = daily.getJSONArray("data");

        Day[] days = new Day[data.length()];

        //Populates each index with a day object
        for (int i = 0; i < data.length(); i++)
        {
            JSONObject jsonDay = data.getJSONObject(i);
            Day day = new Day();

            day.setSummary(jsonDay.getString("summary"));
            day.setIcon(jsonDay.getString("icon"));
            day.setTemperatureMax(jsonDay.getDouble("temperatureMax"));
            day.setTime(jsonDay.getLong("time"));
            day.setTimezone(timezone);

            days[i] = day;
        }
        return days;
    }

    //Creates an array of Hour objects that each hold data for a specific hour of the day
    //This is passed into multiple subviews in the hourly forecast activity
    private Hour[] getHourlyForecast(String jsonData) throws JSONException {
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");
        JSONObject hourly = forecast.getJSONObject("hourly");//Under the hourly category in JSONData
        JSONArray data = hourly.getJSONArray("data");

        Hour[] hours = new Hour[data.length()];

        //populates each index with an hour object
        for (int i = 0; i < data.length(); i++)
        {
            JSONObject jsonHour = data.getJSONObject(i);
            Hour hour = new Hour();

            hour.setSummary(jsonHour.getString("summary"));
            hour.setTemperature(jsonHour.getDouble("temperature"));
            hour.setIcon(jsonHour.getString("icon"));
            hour.setTime(jsonHour.getLong("time"));
            hour.setTimezone(timezone);

            hours[i] = hour;
        }
        return hours;
    }

    //Populates the starting activity with current data from JSON
    private Current getCurrentDetails(String jsonData) throws JSONException {
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");
        Log.i(TAG, "From JSON: " +  timezone);

        JSONObject currently = forecast.getJSONObject("currently");

        Current current = new Current();
        current.setHumidity(currently.getDouble("humidity"));
        current.setTime(currently.getLong("time"));
        current.setIcon(currently.getString("icon"));
        current.setPrecipChance(currently.getDouble("precipProbability"));
        current.setSummary(currently.getString("summary"));
        current.setTemperature(currently.getDouble("temperature"));
        current.setTimeZone(timezone);

        Log.d(TAG, current.getFormattedTime());

        return current;
    }

    //Boolean value returns true if WiFi is available
    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if(networkInfo != null && networkInfo.isConnected());
        {
            isAvailable = true;
        }
        return isAvailable;
    }

    private void alertUserAboutError() {
        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(), "error_dialog");
    }

    @OnClick (R.id.dailyButton)
    public void startDailyActivity(View view){
        Intent intent = new Intent(this, DailyForecastActivity.class);
        intent.putExtra(DAILY_FORECAST, mForecast.getDailyForecast());
        startActivity(intent);
    }
    @OnClick(R.id.hourlyButton)
    public void startHourlyActivity(View view){
        Intent intent = new Intent(this, HourlyForecastActivity.class);
        intent.putExtra(HOURLY_FORECAST, mForecast.getHourlyForecast());
        startActivity(intent);
    }
}
