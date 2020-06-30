package com.example.weatherapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class CitiesSavedActivity extends AppCompatActivity {

    ArrayList<String> citySavedList = new ArrayList<>();
    ListView cityListView;

    private static final String FILE_NAME = "city_saved.json";
    File file;
    FileReader fileReader= null;
    FileWriter fileWriter = null;
    BufferedReader bufferedReader = null;
    BufferedWriter bufferedWriter = null;
    String response = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cities_saved);

        file = new File(this.getFilesDir(), FILE_NAME);
        cityListView = findViewById(R.id.citySavedListView);

        //LETTURA DA FILE CON INSERIMENTO ELEMENTI NELLA LISTA
        try{
            //LETTURA FILE
            StringBuffer output = new StringBuffer();
            fileReader = new FileReader(file.getAbsolutePath());
            bufferedReader = new BufferedReader(fileReader);
            String line = "";

            while((line = bufferedReader.readLine()) != null){
                output.append(line + "\n");

            }
            response = output.toString();
            bufferedReader.close();

            JSONObject jsonObject = new JSONObject(response);
            boolean isCitySavedExists = jsonObject.has("citiesStarred");

            if(!isCitySavedExists) {

            }
            else{
                JSONArray citySaved = (JSONArray) jsonObject.get("citiesStarred");
                for(int i= 0; i < citySaved.length(); i++) {
                    citySavedList.add(citySaved.get(i).toString());
                    Log.i("Aggiunta alla lista", citySaved.get(i).toString());

                }

            }
        } catch (Exception e){
            e.printStackTrace();
        }


        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1 , citySavedList);
        cityListView.setAdapter(arrayAdapter);

        cityListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(CitiesSavedActivity.this, citySavedList.get(position), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(CitiesSavedActivity.this, HistoryActivity.class);
                intent.putExtra("CityTapped", citySavedList.get(position));
                startActivity(intent);
                return true;

            }
        });

        //gestisco il tocco di un elemento nella lista
        cityListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Intent intent = new Intent(CitiesSavedActivity.this, MainActivity.class);
                Log.i("CITY SELECTED", citySavedList.get(position));

                Intent returnIntent = new Intent();
                returnIntent.putExtra("CITY",citySavedList.get(position));
                setResult(RESULT_OK,returnIntent);
                finish(); //chiudo l'activity
            }
        });



    }




}
