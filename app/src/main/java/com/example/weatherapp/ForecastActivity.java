package com.example.weatherapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ForecastActivity extends AppCompatActivity {

    String valuesPassed, daySelected;
    String cityPassedFromHistoryActivity, dayPassedFromHistoryActivity;
    String[] forecastTotalArray;
    String[] forecastArray;
    ListView forecastListView;
    Boolean startedFromHistoryActivity = false;
    Boolean startedFromMainActivity = false;
    final ArrayList<String> forecastList = new ArrayList<>();

    private static final String FILE_NAME_HISTORY = "history.json";
    File fileHistory;
    FileReader fileReader= null;
    FileWriter fileWriter = null;
    BufferedReader bufferedReader = null;
    BufferedWriter bufferedWriter = null;
    String response = null;

    private ArrayList<Forecast> forecastCustomObject = new ArrayList<>();
    private ArrayList<Forecast> historyForecastCustomObject = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast);

        forecastListView = findViewById(R.id.customForecastListView);
        TextView displayDaySelected = findViewById(R.id.displayDaySelectedTextView);
        fileHistory = new File(this.getFilesDir(), FILE_NAME_HISTORY);

        if(((getIntent().getExtras().getString("startedFromHistoryActivity")).equals("true")) && (getIntent().getExtras().getString("startedFromMainActivity")).equals("false")){
            cityPassedFromHistoryActivity = getIntent().getExtras().getString("CityPassed");
            dayPassedFromHistoryActivity = getIntent().getExtras().getString("DayTapped");
            Log.i("DAY", dayPassedFromHistoryActivity + " " + cityPassedFromHistoryActivity);
            try{
                //LETTURA FILE
                StringBuffer output = new StringBuffer();
                fileReader = new FileReader(fileHistory.getAbsolutePath());
                bufferedReader = new BufferedReader(fileReader);
                String line = "";

                while((line = bufferedReader.readLine()) != null){
                    output.append(line + "\n");

                }
                response = output.toString();
                bufferedReader.close();

                JSONObject jsonObject = new JSONObject(response);
                boolean isCitySavedExists = jsonObject.has(cityPassedFromHistoryActivity);

                if(!isCitySavedExists) {

                }
                else{
                    JSONArray citySaved = (JSONArray) jsonObject.get(cityPassedFromHistoryActivity);
                    for(int i= 0; i < citySaved.length(); i++) {
                        String[] values = citySaved.getString(i).split("\\+");
                        if(values[0].equals(dayPassedFromHistoryActivity)){
                            Log.i("GIORNO UGUALE", dayPassedFromHistoryActivity);

                            /**L'orario delle previsioni è UTC 0000. Al momento in Italia c'è l'ora legale quindi converto l'ora a UTC +2 */

                            //creo oggetto Forecast
                            historyForecastCustomObject.add(new Forecast(values[0], values[1], values[3], values[2], values[4]));
                            Log.i("PREVISIONE", values[0] + " " + values[1] + " " + values[2] + " " + values[3] + " " + values[4]);
                        }
                        else{

                            Log.i("GIORNO DIVERSO",dayPassedFromHistoryActivity);

                        }

                    }

                    displayDaySelected.setText(dayPassedFromHistoryActivity);
                    ArrayAdapter<Forecast> adapterHistory = new ForecastArrayAdapter(this, 0, historyForecastCustomObject);
                    ListView customListView = findViewById(R.id.customForecastListView);
                    customListView.setAdapter(adapterHistory);

                }
            } catch (Exception e){
                e.printStackTrace();
            }

            Log.i("HISTORY", "SI");
        }
        else if(((getIntent().getExtras().getString("startedFromMainActivity")).equals("true")) && (getIntent().getExtras().getString("startedFromHistoryActivity")).equals("false")){

            //recupero i valori passati dalla MainActivity
            valuesPassed =  getIntent().getExtras().getString("VALUE");
            Log.i("VALUES", valuesPassed.toString());

            //array contenente la stringa con la previsione trioraria
            forecastTotalArray = valuesPassed.split("---");
            Log.i("VALUESFORECAST", forecastTotalArray.toString());

            Log.i("SIZE", Integer.toString(forecastTotalArray.length));

            for(int i=0; i < forecastTotalArray.length; i++){
                forecastArray = forecastTotalArray[i].split("/");
                //imposto il giorno selezionato
                daySelected = forecastArray[0];

                /** convertire la stringa added in oggetto della classe Forecast (da creare) in cui gli passiamo
                 * come valori forecastArray[1], [2], ...
                 */

                /**L'orario delle previsioni è UTC 0000. Al momento in Italia c'è l'ora legale quindi converto l'ora a UTC +2 */
                //Log.i("FORECASTTIME", forecastArray[1].toString());
                //String[] valuesTime = forecastArray[1].split(":");
                //Log.i("FORECASTTIMENORMAL", valuesTime[0]);
                //int hours = Integer.parseInt(valuesTime[0]) + 2;
                //Log.i("FORECASTTIME+2", Integer.toString(hours));

                //forecastArray[1] = Integer.toString(hours)+":00:00";

                // AGGIUNGO L'OGGETTO FORECAST ALLA LISTA
                forecastCustomObject.add(new Forecast(forecastArray[0], forecastArray[1], forecastArray[2], forecastArray[3], forecastArray[4]));


            }

            //imposto il giorno selezionato per mostrarlo sopra la listView
            displayDaySelected.setText(daySelected);

            ArrayAdapter<Forecast> adapter = new ForecastArrayAdapter(this, 0, forecastCustomObject);
            ListView customListView = findViewById(R.id.customForecastListView);
            customListView.setAdapter(adapter);



            Log.i("MAIN", "SI");
        }



        /** Leggo il file recuperando le previsioni del giorno passato dalla HistoryActivity per inserirle
         * nella lista historyForecastCustomObject, creando un oggetto Forecast e mostrando nella ListView
         * modificata dall'Adapter
         */


        /**L'orario delle previsioni è UTC 0000. Al momento in Italia c'è l'ora legale quindi converto l'ora a UTC +2 */


    }


    //creo ArrayAdapter class personalizzato
    class ForecastArrayAdapter extends ArrayAdapter<Forecast>{

        private Context context;
        private List<Forecast> forecastCustomObject;

        //constructor
        public ForecastArrayAdapter(Context context, int resource, ArrayList<Forecast> objects){
            super(context, resource, objects);

            this.context = context;
            this.forecastCustomObject = objects;
        }

        //called when rendering the list
        public View getView(int position, View convertView, ViewGroup parent){

            //get the property we are displaying
            Forecast forecastObj = forecastCustomObject.get(position);

            //get the inflater and inflate the XML layout for each item
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.forecast_layout, null);

            //identify views in forecast_layout
            TextView hour = view.findViewById(R.id.hourTextView);
            ImageView iconImage = view.findViewById(R.id.iconImageView);
            TextView weatherCondition = view.findViewById(R.id.weatherConditionsTextView);

            //set hour and weatherCondition
            hour.setText(String.valueOf(forecastObj.getTime()));
            weatherCondition.setText(forecastObj.getTemperature() + "   " + forecastObj.getWeatherCondition());

            Log.i("TIME", forecastObj.getIcon() + " " + forecastObj.getWeatherCondition() + " " + forecastObj.getTime());


            int imageID = context.getResources().getIdentifier("icon"+forecastObj.getIcon(), "drawable", context.getPackageName());
            iconImage.setImageResource(imageID);

            return view;
        }

    }
}
