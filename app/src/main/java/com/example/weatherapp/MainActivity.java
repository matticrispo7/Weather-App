package com.example.weatherapp;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class MainActivity extends AppCompatActivity {

    TextView currentWeatherTextView, citySelectedTextView;
    ImageView currentWeatherImageView;
    ImageButton openCitySavedActivity, searchImageButton;
    AutoCompleteTextView autoCompleteTextView;
    ListView listView;
    ImageButton favoriteImageButton;
    String urlCurrentWeatherAPI = "http://api.openweathermap.org/data/2.5/weather?q=";
    String urlForecastAPI = "http://api.openweathermap.org/data/2.5/forecast?q=";
    // ---------------- INSERT API KEY HERE ----------------
    String apiKey = "&APPID=";
    String options = "&units=metric&lang=it";
    String citySelected = "";
    String citySavedPassed="";
    Boolean cityAddedToFavourite = false;
    Boolean showSearchTextView = false;
    Boolean isCitySelectedAddedToFavourites = false;

    private static final String FILE_NAME = "city_saved.json";
    private static final String FILE_NAME_HISTORY = "history.json";
    private static final String CSV_FILE = "./cityStarred.csv";
    File file, fileHistory, fileCSV;
    FileReader fileReader= null, fileReaderCSV = null;
    FileWriter fileWriter = null;
    BufferedReader bufferedReader = null, bufferedReaderCSV = null;
    BufferedWriter bufferedWriter = null;
    String response = null;

    HashMap<String, String> mapDayTime = new HashMap<>(); //memorizza le previsioni triorarie dei giorni (presi singolarmente) della listView


    final ArrayList<String> forecast = new ArrayList<String>();

    public void showSearchText(View view){

        //ad una pressione mostro la view, ad una seconda la rendo invisibile
        if(!showSearchTextView){
            autoCompleteTextView.setVisibility(View.VISIBLE);
            showSearchTextView = true;
            //abbasso la tastiera una volta selezionata la città tramite un InputManager
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);

        } else  {
            autoCompleteTextView.setVisibility(View.INVISIBLE);
            showSearchTextView = false;
            //abbasso la tastiera una volta selezionata la città tramite un InputManager
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(autoCompleteTextView.getWindowToken(), 0);

        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //identifico view del layout
        currentWeatherTextView = findViewById(R.id.currentWeatherTextView);
        currentWeatherImageView = findViewById(R.id.weatherImageView);
        searchImageButton = findViewById(R.id.searchImageButton);
        autoCompleteTextView = findViewById(R.id.autoCompleteTextView);
        listView = findViewById(R.id.listView);
        favoriteImageButton = findViewById(R.id.buttonSaveCity);
        openCitySavedActivity = findViewById(R.id.buttonCurrentLocation);
        citySelectedTextView = findViewById(R.id.citySelectedTextView);

        //imposto invisible l'autoCompleteTextView per poi mostrarla alla pressione del ImageButton con la lente
        autoCompleteTextView.setVisibility(View.INVISIBLE);


        /** RICHIAMO L'ACTIVITY CON LA LISTA DELLE CITTA' PREFERITE ASPETTANDOMI UN VALORE
         * DI RITORNO (LA CITTA' SELEZIONATA DALLA LISTA) IN MODO DA POTER ESEGUIRE LA
         * CHIAMATA ALL'API CON QUELLA CITTA'
         */
        openCitySavedActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CitiesSavedActivity.class);
                startActivityForResult(intent, 1);
            }
        });


        //FILE PER LA SCRITTURA/LETTURA DELLE CITTA' SALVATE E DELLA HISTORY
        file = new File(this.getFilesDir(), FILE_NAME);
        fileHistory = new File(this.getFilesDir(), FILE_NAME_HISTORY);
        fileCSV = new File(this.getFilesDir(), CSV_FILE);

        //collego l'array in values/strings.xml a citiesArray per poi passarlo nell'adapter
        final String[] citiesArray = getResources().getStringArray(R.array.citiesArray);
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, citiesArray);
        autoCompleteTextView.setAdapter(arrayAdapter);

        //metodo eseguito una volta selezionata una città dal menù a tendina
        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                try{
                    //elimino gli elementi della lista
                    forecast.clear();

                    //recupero la stringa selezionata e sostituisco gli spazi con un %20
                    citySelected = arrayAdapter.getItem(position);
                    String city = arrayAdapter.getItem(position).replace(" ", "%20");
                    Log.i("CITY", city);

                    //aggiono l'icona del cuore a seconda che la città sia stata salvata o meno
                    checkCity();

                    //definisco gli url da passare come oggetto per la connessione
                    String totalCurrentWeatherlURL = urlCurrentWeatherAPI + city + apiKey + options;
                    String totalForecastURL = urlForecastAPI + city + apiKey + options + "&cnt=40";
                    //loggo l'URL totale
                    Log.i("URL CURRENT WEATHER", totalCurrentWeatherlURL);
                    Log.i("URL FORECAST", totalForecastURL);


                    //creo oggetto per iniziare il download del JSON
                    DownloadCity cityObj = new DownloadCity();
                    String result = null;
                    String resultCurrentWeather = null;


                    //invio i 2 url all'asynctask per eseguire la chiamata all'API
                    result = cityObj.execute(totalForecastURL, totalCurrentWeatherlURL).get();

                    //mostro il nome della città nella textView e nascondo l'autoCompleteTextView
                    citySelectedTextView.setText(citySelected);
                    autoCompleteTextView.setVisibility(View.INVISIBLE);


                    //abbasso la tastiera una volta selezionata la città tramite un InputManager
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(autoCompleteTextView.getWindowToken(), 0);



                } catch (Exception e){
                    e.printStackTrace();
                }

            }
        });

        ArrayAdapter<String> weatherArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, forecast);
        listView.setAdapter(weatherArrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getApplicationContext(), forecast.get(position), Toast.LENGTH_SHORT).show();
                //Log.i("HASHMAP", mapDayTime.get(forecast.get(position)));

                Intent intent = new Intent(MainActivity.this, ForecastActivity.class);
                Log.i("DAY SELECTED", forecast.get(position));
                intent.putExtra("VALUE", mapDayTime.get(forecast.get(position)));
                intent.putExtra("startedFromHistoryActivity", "false");
                intent.putExtra("startedFromMainActivity", "true");
                startActivity(intent);

            }
        });

    }



    //This method is called when the CitiesSavedActivity finishes
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1) {
            if(resultCode == Activity.RESULT_OK){
                try {
                    //svuoto la lista
                    forecast.clear();

                    /** QUESTO FLAG LO UTILIZZO COME VERIFICA NELL'ASYNCTASK
                     * Se l'utente ha selezionato una città che aveva salvato, allora imposto a TRUE il flag
                     * e salvo le previsioni nell'asynctask, in caso contrario imposto a FALSE e non vengono
                     * memorizzate le previsioni.
                     * */
                    isCitySelectedAddedToFavourites = true;

                    //rendo invisibile l'autoCompleteTextView
                    autoCompleteTextView.setVisibility(View.INVISIBLE);


                    //recupero la stringa della città dall'activity
                    String result = data.getStringExtra("CITY");
                    citySelected = result;
                    Log.i("INTENT RESULT", result);

                    //sostituisco gli spazi con %20
                    String cityPassed = result.replace(" ", "%20");
                    //definisco gli url da passare come oggetto per la connessione
                    String totalCurrentWeatherlURL = urlCurrentWeatherAPI + cityPassed + apiKey + options;
                    String totalForecastURL = urlForecastAPI + cityPassed + apiKey + options + "&cnt=40";
                    //loggo l'URL totale
                    Log.i("CURRENT WEATHER INTENT", totalCurrentWeatherlURL);
                    Log.i("URL FORECAST INTENT", totalForecastURL);

                    //creo oggetto per iniziare download JSON
                    DownloadCity cityObj = new DownloadCity();
                    String resultJSON = null;
                    String resultCurrentWeather = null;

                    //invio i 2 URL all'asynctask
                    resultJSON = cityObj.execute(totalForecastURL, totalCurrentWeatherlURL).get();

                    //mostro il nome della città nella textView
                    citySelectedTextView.setText(result);

                    //aggiono l'icona del cuore a seconda che la città sia stata salvata o meno
                    checkCity();




                } catch (Exception e){
                    e.printStackTrace();
                }

            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
                Log.i("INTENT RESULT", "NOOOOOOOOO");
            }
        }
    }

    public void addToFavorite(View view) throws IOException, JSONException {
        //sostituisco l'immagine col cuore pieno
        if(!cityAddedToFavourite){
            //controllo che l'utente abbia selezionato la città
            if(!(citySelected.equals(""))){
                favoriteImageButton.setImageResource(R.drawable.baseline_favorite_black_24dp);
                cityAddedToFavourite = true;
                Toast.makeText(getApplicationContext(), citySelected + " added to favourites", Toast.LENGTH_SHORT).show();


                //VERIFICO SE IL FILE ESISTE GIA', IN CASO CONTRARIO LO CREO
                if(!file.exists()){
                    Log.i("FILE ESISTENTE", "NO");
                    try{

                        file.createNewFile();
                        Log.i("FILE CREATO", "YES");
                        fileWriter = new FileWriter(file.getAbsoluteFile());
                        bufferedWriter = new BufferedWriter(fileWriter);
                        bufferedWriter.write("{}");
                        bufferedWriter.close();

                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }

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
                Log.i("PRIMOJSON", jsonObject.toString());
                boolean isCitySavedExists = jsonObject.has("citiesStarred");
                Log.i("SECJSON", Boolean.toString(isCitySavedExists));

                if(!isCitySavedExists) {
                    JSONArray newCitySaved = new JSONArray();
                    newCitySaved.put(citySelected);
                    jsonObject.put("citiesStarred", newCitySaved);
                }
                else{
                    JSONArray citySaved = (JSONArray) jsonObject.get("citiesStarred");
                    if(citySaved.toString().contains(citySelected)){
                        Log.i("CITTA", "PRESENTE");
                    }
                    else{
                        Log.i("CITTA AGGIUNTA", citySelected);
                        citySaved.put(citySelected);
                    }
                }
                //SCRITTURA FILE
                fileWriter = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fileWriter);
                bw.write(jsonObject.toString());
                Log.i("SCRITTO", jsonObject.toString());
                bw.close();
            }



        } else if(cityAddedToFavourite){
            favoriteImageButton.setImageResource(R.drawable.baseline_favorite_border_black_24dp);
            cityAddedToFavourite = false;
            Toast.makeText(getApplicationContext(), citySelected + " removed from favourites", Toast.LENGTH_SHORT).show();


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
                //scorro l'array e rimuovo la città selezionata
                JSONArray citySaved = (JSONArray) jsonObject.get("citiesStarred");
                for(int i= 0; i < citySaved.length(); i++) {
                    if (citySaved.get(i).equals(citySelected)) {
                        citySaved.remove(i);
                        Log.i("REMOVED CITY", citySelected);
                        Log.i("SCRITTO", jsonObject.toString());
                    }
                }

            }

            //salvo le modifiche sul file
            fileWriter = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fileWriter);
            bw.write(jsonObject.toString());
            Log.i("SCRITTO", jsonObject.toString());
            bw.close();



        }

    }



    public class DownloadCity extends AsyncTask<String, Void, String>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(MainActivity.this,"Updating info",Toast.LENGTH_LONG).show();


        }

        @Override
        protected String doInBackground(String... strings) {

            String resultForecast = "";
            String resultCurrentWeather = "";

            URL url;
            HttpURLConnection urlConnection = null;

            try{

                Log.i("STRINGS[0]", strings[0]);
                Log.i("STRINGS[1]", strings[1]);
                Log.i("DOWNLOAD FORECAST", "started");
                url = new URL(strings[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();

                while (data != -1) {
                    char current = (char) data;

                    resultForecast += current;

                    data = reader.read();
                }

                Log.i("DOWNLOAD FORECAST", "finished");

                Log.i("DOWNLOAD CURRENT", "started");
                url = new URL(strings[1]);
                Log.i("URL CURRENT", url.toString());
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in2 = urlConnection.getInputStream();
                InputStreamReader reader2 = new InputStreamReader(in2);
                int data2 = reader2.read();

                while (data2 != -1) {
                    char current = (char) data2;

                    resultCurrentWeather += current;

                    data2 = reader2.read();
                }

                Log.i("DOWNLOAD FORECAST", "finished");
                Log.i("RESULT CURRENT WEATHER", resultCurrentWeather);
                Log.i("RESULT FORECAST", resultForecast);



                return resultForecast + "--" + resultCurrentWeather;


            } catch (Exception e){
                e.printStackTrace();
                return("Download failed");

            }

        }


        @Override

        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            /** Divido la stringa s in due sottostringhe:
             * resultJSON[0] = json di resultForecast
             * rsultJSON[1] = json di resultCurrentWeather
             */
            String[] resultJSON = s.split("--");

            try {

                /** CODICE PER LISTVIEW UTILIZZANDO IL JSON DI resultForecast
                 */
                //creo l'oggetto JSON
                JSONObject jsonObject = new JSONObject(resultJSON[0]);
                //memorizzo l'array del campo 'list'
                JSONArray list = new JSONArray(jsonObject.getString("list"));
                String valuesDayTime = "";
                String[] date;
                String weatherDescription = "";
                String weatherMain = "";
                String temperature = "";
                String weatherIcon = "";
                Boolean isCitySelectdInArray = false;
                JSONArray forecastCitySelected = new JSONArray();
                //forecastCitySelected.clear();


                /** CODICE PER LA GESTIONE DEL FILE */
                //VERIFICA PER LA SCRITTURA SU FILE DELLE PREVISIONI SE LA CITTA' CERCATA E' STATA AGGIUNTA AI PREFERITI

                if(isCitySelectedAddedToFavourites) {
                    Log.i("PREFERITI", " SIIIII");
                    isCitySelectdInArray = true;


                    //VERIFICO SE IL FILE ESISTE GIA', IN CASO CONTRARIO LO CREO
                    if (!fileHistory.exists()) {
                        Log.i("FILE ESISTENTE", "NO");
                        try {

                            fileHistory.createNewFile();
                            Log.i("FILE CREATO", "YES");
                            fileWriter = new FileWriter(fileHistory.getAbsoluteFile());
                            bufferedWriter = new BufferedWriter(fileWriter);
                            bufferedWriter.write("{}");
                            bufferedWriter.close();


                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Log.i("PREFERITI", "NOOOOOO");
                    isCitySelectdInArray = false;

                    //VERIFICO SE IL FILE ESISTE GIA', IN CASO CONTRARIO LO CREO
                    if (!fileHistory.exists()) {
                        Log.i("FILE ESISTENTE", "NO");
                        try {

                            fileHistory.createNewFile();
                            Log.i("FILE CREATO", "YES");
                            fileWriter = new FileWriter(fileHistory.getAbsoluteFile());
                            bufferedWriter = new BufferedWriter(fileWriter);
                            bufferedWriter.write("{}");
                            bufferedWriter.close();


                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }



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
                Log.i("RESPONSe", response);

                JSONObject jsonObject1 = new JSONObject(response);
                Log.i("JSONOB", jsonObject1.toString());
                Boolean isCityStarredExistsInFile = jsonObject1.has(citySelected);
                Log.i("ISCITYSTARRED", isCityStarredExistsInFile.toString());

                JSONArray readingForecastCitySelected = new JSONArray();

                //verifico se la città (che è salvata nei preferiti) è presente nel file sennò la inserisco
                if(!isCityStarredExistsInFile){
                    if(isCitySelectdInArray) {
                        JSONArray newCityStarred = new JSONArray();
                        jsonObject1.put(citySelected, newCityStarred);
                        Log.i("CITYADDEDJSON", jsonObject1.toString());

                        readingForecastCitySelected = (JSONArray) jsonObject1.get(citySelected);
                        for(int i=0; i< readingForecastCitySelected.length(); i++){
                            Log.i("PREVIOUS FORECAST", readingForecastCitySelected.get(i).toString());
                        }

                    } else{
                        Log.i("CITYADDEDJSON", "NO");

                    }
                } else if(isCitySelectdInArray) {
                    readingForecastCitySelected = (JSONArray) jsonObject1.get(citySelected);
                    for (int i = 0; i < readingForecastCitySelected.length(); i++) {
                        Log.i("PREVIOUS FORECAST", readingForecastCitySelected.get(i).toString());
                    }
                }



                /**
                 Memorizzo il contenuto dell'array (ovvero un insieme di oggetti JSON) del campo "list", scorro l'array e per ogni iterazione creo un oggetto "temporaneo"
                 (jsonPart) che contiene l'oggetto JSON corrispondente a quell'indice.
                 Dato che anche il campo weather è un array (contenente un solo elemento però), faccio la stessa cosa per accedere ai suoi valori
                 **/
                for(int i=0; i < list.length(); i++){
                    //creo un oggetto JSON che corrisponde all'oggetto JSON con indice i nell'array list
                    JSONObject jsonPart = list.getJSONObject(i);
                    JSONObject jsonMain = new JSONObject(jsonPart.getString("main"));
                    JSONArray weather = new JSONArray(jsonPart.getString("weather"));
                    Log.i("SYS_TXT", jsonPart.getString("dt_txt"));

                    temperature = jsonMain.getString("temp");

                    //splitto la stringa per recuperare solo la data (la uso come key dell'hashmap per inserire i valori
                    date = jsonPart.getString("dt_txt").split(" ");


                    //  date[0] = data,     date[1] = orario

                    //scorro l'array weather
                    for(int j=0; j < weather.length(); j++){
                        JSONObject weatherPart = weather.getJSONObject(j);
                        weatherMain = weatherPart.getString("main");
                        weatherDescription = weatherPart.getString("description");
                        weatherIcon = weatherPart.getString("icon");
                        Log.i("WEATHER PART", weatherPart.getString("main"));

                    }


                    /**converto l'orario col fuso orario corrente in italia (+2) **/
                    String[] valuesTime = date[1].split(":");
                    int hours = Integer.parseInt(valuesTime[0]) + 2;
                    date[1] = hours +":00:00";

                    //Se la città è salvata nei preferiti allora scrivo su file le previsioni
                    if(isCitySelectdInArray){
                        JSONArray cityStarred = (JSONArray) jsonObject1.get(citySelected);
                        Log.i("CITYLENGTH", Integer.toString(readingForecastCitySelected.length()));
                        Log.i("FORECASTLENGHT", Integer.toString(forecastCitySelected.length()));

                        //Log.i("HOURS",date[1]);
                        Log.i("FORECASTINSERT AT INDEX", Integer.toString(i) +"     " + date[0] + "+" + date[1] + "+" + jsonMain.getString("temp") + "+" + weatherMain + "+" + weatherIcon );
                        forecastCitySelected.put(i,date[0] + "+" + date[1] + "+" + jsonMain.getString("temp") + "+" + weatherMain + "+" + weatherIcon);
                        //Log.i("READINGFORECAST INSERT",forecastCitySelected.get(i).toString());

                    }

                    /*Controllo che la lista non sia vuota: se lo è aggiungo la stringa altrimenti controllo che la stringa dell'elemento precedente sia != dalla stringa
                     attuale e, in caso affermativo, inserisco tale stringa nella lista per mostrarla nella ListView
                     AGGIUNTA ELEMENTI NELLA LISTA DELLA LISTVIEW
                     */

                    if(forecast.isEmpty() == true){
                        forecast.add(date[0]);
                        valuesDayTime = date[0] + "/" + date[1] + "/" + weatherDescription + "/" + temperature + "/" + weatherIcon;

                    } else {

                        if(forecast.contains(date[0])){
                            valuesDayTime += "---" + date[0] + "/" + date[1] + "/" + weatherDescription + "/" + temperature + "/" + weatherIcon;
                            mapDayTime.put(date[0], valuesDayTime);

                        }
                        else{
                            //Log.i("DATA DIVERSA", date[0]);
                            forecast.add(date[0]);
                            //cancello la stringa per inserire i valori del nuovo giorno
                            valuesDayTime = "";
                            valuesDayTime = date[0] + "/" + date[1] + "/" + weatherDescription + "/" + temperature + "/" + weatherIcon;
                            mapDayTime.put(date[0], valuesDayTime);
                        }
                    }

                }



                //se la lista inizialmente è vuota allora inserisco i valori scaricati così come sono
                if(readingForecastCitySelected.length() == 0 && isCitySelectdInArray){
                    Log.i("LISTA READINGFORECAST", "VUOTA");
                    for(int i=0; i<forecastCitySelected.length(); i++){
                        readingForecastCitySelected.put(forecastCitySelected.get(i));
                    }

                } else {
                    //se la lista non è vuota allora controllo i valori da inserire con i dati che ho scaricato
                    //DOPO AVER INSERITO I VALORI IN READINGFORECAST... CONTROLLO EVENTUALI VALORI "COMUNI" TRA LE DUE LISTE
                    if (isCitySelectdInArray) {
                        Boolean isDataAlreadyIn = false;
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");

                        //prova stampa data e ora
                        String[] lastValueReadingForecastCitySelected = readingForecastCitySelected.get(readingForecastCitySelected.length()-1).toString().split("\\+");
                        String lastValue = lastValueReadingForecastCitySelected[0] + "/" + lastValueReadingForecastCitySelected[1];
                        Log.i("LASTVALUE", lastValue);
                        String[] singleDateValue = lastValueReadingForecastCitySelected[0].split("-");
                        String[] singleTimeValue = lastValueReadingForecastCitySelected[1].split(":");
                        String year = singleDateValue[0];
                        String month = singleDateValue[1];
                        String day = singleDateValue[2];
                        String hours = singleTimeValue[0];
                        String minutes = singleTimeValue[1];
                        String seconds = singleTimeValue[2];
                        String data = year+"-"+month+"-"+day +" "+hours+"-"+minutes+"-"+seconds;
                        Date dataReadingForecast = sdf.parse(data);
                        //Log.i("DATE", year +"   " + month + "   " + day);
                        Log.i("DATE", sdf.format(dataReadingForecast));

                        for (int i = 0; i < readingForecastCitySelected.length(); i++) {
                            String[] valueReadingForecastCitySelected = readingForecastCitySelected.get(i).toString().split("\\+");
                            String value = valueReadingForecastCitySelected[0] + "/" + valueReadingForecastCitySelected[1];
                            Log.i("VALUECITY", value +" AT INDEX " + i);

                            for (int j = 0; j < forecastCitySelected.length(); j++) {
                                String[] valueForecastCitySelected = forecastCitySelected.get(j).toString().split("\\+");
                                String valueSecond = valueForecastCitySelected[0] + "/" + valueForecastCitySelected[1];

                                //prova Data e ora
                                String[] singleDateValueForecast = valueForecastCitySelected[0].split("-");
                                String[] singleTimeValueForecast = valueForecastCitySelected[1].split(":");
                                String yearForecast = singleDateValueForecast[0];
                                String monthForecast = singleDateValueForecast[1];
                                String dayForecast = singleDateValueForecast[2];
                                String hoursForecast = singleTimeValueForecast[0];
                                String minutesForecast = singleTimeValueForecast[1];
                                String secondsForecast = singleTimeValueForecast[2];
                                String dataForecast = yearForecast+"-"+monthForecast+"-"+dayForecast +" "+hoursForecast+"-"+minutesForecast+"-"+secondsForecast;
                                Date dataForecastCity = sdf.parse(dataForecast);
                                if (value.equals(valueSecond)) {
                                    Log.i("VALUEUGUALI", value + "      " + valueSecond);
                                    //rimpiazzo il valore aggiornato con la stessa data
                                    //Log.i("VALUERIMOSSO ", readingForecastCitySelected.get(i).toString() + "    AT INDEX    " + String.valueOf(i));
                                    //readingForecastCitySelected.remove(i);
                                    Log.i("VALUEINSERITO ", forecastCitySelected.get(j).toString() + "  AT INDEX    " + String.valueOf(i));
                                    readingForecastCitySelected.put(i,forecastCitySelected.get(j).toString() );
                                } else if(dataForecastCity.compareTo(dataReadingForecast) > 0 && !isDataAlreadyIn){
                                    //se la data della previsione scaricata è maggiore della data nella lista
                                    Log.i("DATA SCARICATA", sdf.format(dataForecastCity));
                                    Log.i("DATA SCARICATA MAGGIORE",sdf.format(dataForecastCity) +"    " + sdf.format(dataReadingForecast) );
                                    Log.i("VALUE DA INSERIRE", forecastCitySelected.get(j).toString());
                                    readingForecastCitySelected.put(forecastCitySelected.get(j).toString());
                                }

                            }
                            isDataAlreadyIn = true;

                        }

                    }
                }




                //SCRITTURA FILE
                if(isCitySelectdInArray) {
                    fileWriter = new FileWriter(fileHistory.getAbsoluteFile());
                    BufferedWriter bw = new BufferedWriter(fileWriter);
                    bw.write(jsonObject1.toString());
                    Log.i("SCRITTO", jsonObject1.toString());
                    bw.close();
                }






                /**CODICE UTILIZZANDO IL JSON DI resultCurrentWeather PER MOSTRARE LE PREVISIONI ATTUALI
                 */
                JSONObject jsonCurrentWeather = new JSONObject(resultJSON[1]);
                JSONArray jsonCurrentWeatherArray = new JSONArray(jsonCurrentWeather.getString("weather"));
                displayWeatherInfo(jsonCurrentWeather, jsonCurrentWeatherArray);



            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }


    /** METODO PER MOSTRARE LE PREVISIONI METEO ATTUALI DELLA
     * CITTA' SELEZIONATA
     */
    public void displayWeatherInfo(JSONObject jsonObj, JSONArray jsonArray){

        String weatherIcon= "null";
        String description = null;
        String temperature = null;

        try{

            //stampo i valori dell'array
            for(int i=0; i < jsonArray.length(); i++){
                JSONObject jsonPart = jsonArray.getJSONObject(i);
                description = jsonPart.getString("description");
                weatherIcon = "icon" +jsonPart.getString("icon");
            }

            //memorizzo le info del campo "main"
            JSONObject mainCurrentWeatherJSON = new JSONObject(jsonObj.getString("main"));
            //aggiungo la temperatura alla stringa weather da mostrare
            temperature = mainCurrentWeatherJSON.getString("temp") + " ° ";
            currentWeatherTextView.setText(description + " " + temperature);

            //imposto l'immagine currentWeather in base al codice restituito dal json
            String uri = "@drawable/" + weatherIcon;
            int imageResource = getResources().getIdentifier(uri, null, getPackageName()); //get image  resource
            Drawable res = getResources().getDrawable(imageResource); //convert into drawble
            currentWeatherImageView.setImageDrawable(res); //set as image


        } catch (Exception e){
            e.printStackTrace();

        }

    }




    /** METODO PER VERIFICARE SE LA CITTA' SELEZIONATA E' PRESENTE
     * NEL FILE JSON AGGIORNANDO L'ICONA DEL CUORE
     */
    public void checkCity(){
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
                Log.i("ARRAY", "NON PRESENTE");
            }
            else{
                JSONArray citySaved = (JSONArray) jsonObject.get("citiesStarred");
                for(int i=0; i<citySaved.length(); i++){
                    Log.i("CITYSAVED", citySaved.get(i).toString());
                }
                if(citySaved.toString().contains(citySelected)){
                    Log.i("CITTA SALVATA", "PRESENTE");
                    for(int i=0; i < citySaved.length(); i++){
                        Log.i("CITTA1-CITTA2", citySaved.get(i).toString() + "  " + citySelected);
                        if(citySaved.get(i).toString().equals(citySelected)){
                            Log.i("CITTAUGUALI", "SI");
                        } else{
                            Log.i("CITTADIVERSE", "SI");
                        }

                    }
                    cityAddedToFavourite = true;
                    isCitySelectedAddedToFavourites = true;
                    favoriteImageButton.setImageResource(R.drawable.baseline_favorite_black_24dp);

                }
                else{
                    Log.i("CITTA NON SALVATA", "NO");
                    isCitySelectedAddedToFavourites = false;
                    favoriteImageButton.setImageResource(R.drawable.baseline_favorite_border_black_24dp);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**Metodo per l'inizializzazione del menù */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    /**Metodo per la gestione degli item del menù */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()){

            //export CSV
            case R.id.exportCSV:
                Log.i("Item selected", "EXPORT");

                //VERIFICO SE IL FILE ESISTE GIA'
                if(!file.exists()){
                    Log.i("FILE ESISTENTE", "NO");
                } else{

                    try{
                        //LETTURA DA FILE
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
                        JSONArray citySaved = (JSONArray) jsonObject.get("citiesStarred");

                        //verifico se il file CSV esiste già senno lo creo
                        if(!fileCSV.exists()){
                            try{
                                fileCSV.createNewFile();
                                Log.i("FILE CSV CREATO", "YES");


                            } catch (Exception e){
                                e.printStackTrace();
                            }


                        }
                        fileWriter = new FileWriter(fileCSV.getAbsoluteFile());
                        BufferedWriter bw = new BufferedWriter(fileWriter);

                        CSVPrinter csvPrinter = new CSVPrinter(bw, CSVFormat.DEFAULT.withHeader("City"));
                        for(int i=0; i<citySaved.length(); i++){
                            Log.i("MENU CITY", citySaved.get(i).toString());

                            csvPrinter.printRecord(citySaved.get(i));
                        }
                        csvPrinter.flush();
                        Toast.makeText(getApplicationContext(), "File saved in: " + this.getFilesDir(), Toast.LENGTH_SHORT).show();

                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }


                return true;

            case R.id.importCSV:
                Log.i("Item selected", "IMPORT");
                //VERIFICO SE IL FILE ESISTE GIA'
                if(!file.exists()){
                    Log.i("FILE ESISTENTE", "NO");
                } else{
                    try{

                        //Lettura file city_starred
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
                        JSONArray citySaved = (JSONArray) jsonObject.get("citiesStarred");

                        //verifico se il file CSV esiste già senno lo creo
                        if(!fileCSV.exists()){
                            try{
                                //fileCSV.createNewFile();
                                Log.i("FILE CSV", "Non esistes");


                            } catch (Exception e){
                                e.printStackTrace();
                            }
                        } else {

                            fileReaderCSV = new FileReader(fileCSV.getAbsolutePath());
                            bufferedReaderCSV = new BufferedReader(fileReaderCSV);

                            CSVParser csvParser = new CSVParser(bufferedReaderCSV, CSVFormat.DEFAULT.withHeader("City").withIgnoreHeaderCase().withTrim());

                            for (CSVRecord csvRecord : csvParser) {

                                // Accessing Values by header 'City'
                                String name = csvRecord.get("City");
                                Log.i("CSV NAME", name);

                                //controllo se il file è aggiornato
                                if (name.equals("City")) {
                                    Log.i("VALUE CSV: City", "SKIP");
                                } else {
                                    if (citySaved.toString().contains(name)) {
                                        Log.i("CITTA", "PRESENTE");
                                    } else {
                                        Log.i("CITTA AGGIUNTA", name);
                                        citySaved.put(name);
                                    }
                                }


                            }

                            //salvo modifiche su file
                            fileWriter = new FileWriter(file.getAbsoluteFile());
                            BufferedWriter bw = new BufferedWriter(fileWriter);
                            bw.write(jsonObject.toString());
                            Log.i("SCRITTO", jsonObject.toString());
                            bw.close();
                        }


                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }






                return true;
            default:
                return false;
        }


    }
}
