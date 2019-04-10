package com.tec.pierre.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

  private final String LINK = "https://www.gamedesigning.org/gaming/characters/";

  private Thread thread;

  private ImageView imageCharacter;

  private Button option1;
  private Button option2;
  private Button option3;

  private int correctOption = -1;
  private int actualIndex = -1;

  private int correctAnswers = 0;

  private ArrayList<String> characters = new ArrayList<>();
  private ArrayList<String> images = new ArrayList<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    option1 = findViewById(R.id.option1);
    option2 = findViewById(R.id.option2);
    option3 = findViewById(R.id.option3);

    imageCharacter = findViewById(R.id.imageCharacter);

    thread = new Thread(LINK);
    thread.execute();

  }

  public void generateQuiz() {
    int option = newOption();
    Download download = new Download(images.get(option), imageCharacter);
    download.execute();
    int[] options = {-1, -1, -1};
    newCorrectOption();
    options[correctOption] = option;
    for(int i = 0; i < options.length; i++) {
      if(options[i] == -1) {
        options[i] = newOption(options);
      }
    }
    option1.setText(characters.get(options[0]));
    option2.setText(characters.get(options[1]));
    option3.setText(characters.get(options[2]));
  }

  public void selectOption(View view) {
    Button button = (Button)view;
    int option = Integer.valueOf(button.getTag().toString());
    if(option == correctOption) {
      correctAnswers++;
    }
    setTitle("Correct Answers: " + correctAnswers);
    generateQuiz();
  }

  private void newCorrectOption() {
    Random random = new Random();
    correctOption = random.nextInt(3);
  }

  private int newOption(int[] values) {
    Random random = new Random();
    int option = -1;
    do {
      option = random.nextInt(characters.size());
    }
    while(inArray(values, option));
    return option;
  }

  private boolean inArray(int[] array, int value) {
    for(int i = 0; i < array.length; i++) {
      if(array[i] == value) {
        return true;
      }
    }
    return false;
  }

  private int newOption(int actualIndex) {
    Random random = new Random();
    int option = -1;
    do {
      option = random.nextInt(characters.size());
    }
    while(option == actualIndex);
    return option;
  }

  private int newOption() {
    Random random = new Random();
    int option = -1;
    do {
      option = random.nextInt(characters.size() -1);
    }
    while(option == actualIndex);
    return option;
  }

  private class Download extends AsyncTask<Void, Void, Void> {

    private String link;
    private Bitmap bitmap;
    private URL url;
    private ImageView image;

    public Download(String link, ImageView image) {
      this.link = link;
      this.image = image;
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      boolean flagContinue = false;
      if(isNetworkAvailable()) {
        try {
          url = new URL(link);
          flagContinue = true;
        }
        catch (MalformedURLException e) {

        }
      }
      if(!flagContinue) {
        this.cancel(true);
      }
    }

    @Override
    protected Void doInBackground(Void... voids) {
      try {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();
        InputStream inputStream = connection.getInputStream();
        bitmap = BitmapFactory.decodeStream(inputStream);
      }
      catch (MalformedURLException e) {
        e.printStackTrace();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
      return null;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
      super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      super.onPostExecute(aVoid);
      image = findViewById(R.id.imageCharacter);
      image.setImageBitmap(bitmap);
    }

    private boolean isNetworkAvailable() {
      ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
      return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

  }

  private class Thread extends AsyncTask<Void, Void, Void> {

    private String link;
    private URL url;

    public Thread(String link) {
      this.link = link;
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      boolean flagContinue = false;
      if(isNetworkAvailable()) {
        try {
          url = new URL(link);
          flagContinue = true;
        }
        catch (MalformedURLException e) {

        }
      }
      if(!flagContinue) {
        this.cancel(true);
      }
    }

    @Override
    protected Void doInBackground(Void... voids) {
      String result = getHTML();
      if(!result.equals("")) {
        Document doc = Jsoup.parse(result);
        Elements elementsCharacters = doc.select("div .entry-content h2");
        Elements elementsImages = doc.select("div .entry-content hr + p img");
        for(int i = 0; i < elementsCharacters.size(); i++) {
           String character = elementsCharacters.get(i).ownText();
           characters.add(character);
        }
        for(int i = 0; i < elementsImages.size(); i++) {
          String image = elementsImages.get(i).absUrl("src");
          if(!image.equals("")) {
            images.add(image);
          }
        }
      }
      return null;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
      super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      super.onPostExecute(aVoid);
      generateQuiz();
    }

    private String getHTML() {
      String result = "";
      try {
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String str;
        while((str = in.readLine()) != null) {
          result += str;
        }
        in.close();
      }
      catch (IOException e) {
        Log.i("Error", "Fallo al leer pagina");
      }
      return result;
    }

    private boolean isNetworkAvailable() {
      ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
      return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

  }

}
