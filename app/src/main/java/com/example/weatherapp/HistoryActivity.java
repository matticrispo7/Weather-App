package com.example.weatherapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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


public class HistoryActivity extends AppCompatActivity {
    String cityTapped;
    String dayTapped;
    TextView cityTextView;
    ListView forecastSavedListView;

    private static final String FILE_NAME_HISTORY = "history.json";
    File fileHistory;
    FileReader fileReader= null;
    FileWriter fileWriter = null;
    BufferedReader bufferedReader = null;
    BufferedWriter bufferedWriter = null;
    String response = null;

    final ArrayList<String> forecastSaved = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        cityTextView = findViewById(R.id.cityTextView);
        forecastSavedListView = findViewById(R.id.forecastSavedListView);
        fileHistory = new File(this.getFilesDir(), FILE_NAME_HISTORY);

        //recupero i valori passati dalla CitiesSavedActivity
        cityTapped =  getIntent().getExtras().getString("CityTapped");
        cityTextView.setText(cityTapped);

        /* Leggo il file history, seleziono l'oggetto JSON inerente alla città selezionata,
        recupero l'array e mostro le informazioni nella ListView */
        //LETTURA DA FILE CON INSERIMENTO ELEMENTI NELLA LISTA
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
            boolean isCitySavedExists = jsonObject.has(cityTapped);

            if(!isCitySavedExists) {

            }
            else{
                JSONArray citySaved = (JSONArray) jsonObject.get(cityTapped);
                for(int i= 0; i < citySaved.length(); i++) {
                    //inserisco il giorno nella lista se questo non è presente.
                    String[] values = citySaved.getString(i).split("\\+");
                    dayTapped = values[0];
                    Log.i("DATA", values[0]);

                    if(forecastSaved.contains(values[0])){
                        Log.i("GIORNO", "PRESENTE");
                    }
                    else{
                        forecastSaved.add(values[0]);
                        Log.i("Aggiunta alla lista", values[0]);
                    }



                }

            }
        } catch (Exception e){
            e.printStackTrace();
        }

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1 , forecastSaved);
        forecastSavedListView.setAdapter(arrayAdapter);
        forecastSavedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i("PASSING CITY", cityTapped);

                Intent intent = new Intent(HistoryActivity.this, ForecastActivity.class);
                intent.putExtra("CityPassed", cityTapped);
                intent.putExtra("DayTapped", forecastSaved.get(position));
                intent.putExtra("startedFromHistoryActivity", "true");
                intent.putExtra("startedFromMainActivity", "false");
                startActivity(intent);
                finish();

            }
        });






    }




}
