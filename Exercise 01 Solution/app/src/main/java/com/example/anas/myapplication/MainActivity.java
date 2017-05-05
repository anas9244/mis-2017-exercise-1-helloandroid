package com.example.anas.myapplication;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    EditText editText;
    Button button;
    TextView textView;
    String urlString="";
    ImageView imageview;

    // Regular Expressions to verify the url.
    // Source: http://stackoverflow.com/questions/34134704/how-to-check-if-a-string-contains-url-in-java
    public static final String URL_REGEX = "((http:\\/\\/|https:\\/\\/)?(www.)?(([a-zA-Z0-9-]){2,}\\.){1,4}([a-zA-Z]){2,6}(\\/([a-zA-Z-_\\/\\.0-9#:?=&;,]*)?)?)";


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //to hide the keyboard on app start.
        // Source: http://stackoverflow.com/questions/5056734/android-force-edittext-to-remove-focus
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        editText= (EditText) findViewById(R.id.editText);


        //to call the onclick method of the "connect" button  when clicking "Send" on keyboard.
        // Source: http://stackoverflow.com/questions/4451374/use-enter-key-on-softkeyboard-instead-of-clicking-button
        editText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
           @Override
           public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
               if (actionId == EditorInfo.IME_ACTION_SEND) {
                   button.callOnClick();
                   return true;
               }
               return false;
           }
       });

        imageview=(ImageView)findViewById(R.id.imageView);

        button = (Button)findViewById(R.id.button);
        textView = (TextView) findViewById(R.id.textview1);
        textView.setMovementMethod(new ScrollingMovementMethod());

        button.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {

               // Hide keyboard to show the result better.
               //http://stackoverflow.com/questions/13593069/androidhide-keyboard-after-button-click
               LinearLayout mainLayout;
               mainLayout = (LinearLayout)findViewById(R.id.linearLayout);
               InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
               imm.hideSoftInputFromWindow(mainLayout.getWindowToken(), 0);


               //to show "loading" before showing the response in any way, whether the url is image or not.
               textView.setVisibility(View.VISIBLE);
               imageview.setVisibility(View.GONE);

               editText= (EditText) findViewById(R.id.editText);
               urlString= editText.getText().toString();

               // to verify that the url given is valid and not missing the protocol before sending the url for the connection according to the protocol provided.
               Pattern p = Pattern.compile(URL_REGEX);
               Matcher m = p.matcher(urlString);

                 if (m.find())
                 {
                      if (urlString.startsWith("http://")) {
                          textView.setText("Loading...");
                          new ConnectTaskHttp().execute();}
                      else if (urlString.startsWith("https://")){
                          textView.setText("Loading...");
                          new ConnectTaskHttps().execute();}
                      else
                          Toast.makeText(getApplicationContext(), "Missing Protocol (http or https)", Toast.LENGTH_LONG).show();

                 }
                 else
                     Toast.makeText(getApplicationContext(), "URL is not valid", Toast.LENGTH_LONG).show();
           }
       });
    }


    boolean checkTimeOut;
    boolean checkURLFound;
    boolean checkConRefused;
    Bitmap bmp;
    boolean image;
    String response="";


    //perform http connection in background and show the response when finished(on post execute method).
    // Source: http://www.codexpedia.com/android/asynctask-and-httpurlconnection-sample-in-android/
    private class ConnectTaskHttp extends AsyncTask<Void, Void, String>{

        @Override
        protected String doInBackground(Void... params) {

            URL url;
            HttpURLConnection con = null;
            response="";
            try{
                url = new URL(urlString);
                con = (HttpURLConnection) url.openConnection();

                //set the connection timeout to 10 seconds
                //https://eventuallyconsistent.net/2011/08/02/working-with-urlconnection-and-timeouts/
                con.setConnectTimeout(10 * 1000);

                // Handle url not found
                if(con.getResponseCode() == HttpsURLConnection.HTTP_OK){
                    checkURLFound=true;
                }else {checkURLFound=false;}

                //Detect if url is for an image or not.
                //Source: http://stackoverflow.com/questions/3453641/detect-if-specified-url-is-an-image-in-android
                String contentType = con.getHeaderField("Content-Type");
                image = contentType.startsWith("image/");
                if (image)
                { InputStream in = new URL(urlString).openStream();
                    bmp = BitmapFactory.decodeStream(in);
                    in.close();
                }
                else {
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String inputBuffer;

                    while ((inputBuffer = in.readLine()) != null)
                    response+=inputBuffer;
                    in.close();
                }

            }
            catch (Exception e) {
                e.printStackTrace();

                //Handle timeout and connection refused
                //Sources: http://stackoverflow.com/questions/6888139/how-do-i-catch-connection-refused-exception-in-java
                // , http://stackoverflow.com/questions/2799938/httpurlconnection-timeout-question
                if (e instanceof SocketTimeoutException)
                    checkTimeOut=true;
                if (e instanceof ConnectException)
                    checkConRefused=true;
            }

            finally {
                if (con != null)
                {con.disconnect();}

            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            if(!image){
            textView.setText(result);
            textView.setVisibility(View.VISIBLE);
            imageview.setVisibility(View.GONE);
            }
            else{ imageview.setImageBitmap(bmp);
                textView.setVisibility(View.GONE);
                imageview.setVisibility(View.VISIBLE);
            }

            if (checkTimeOut)
                Toast.makeText(getApplicationContext(), "Connection Timeout", Toast.LENGTH_LONG).show();
            if (!checkURLFound)
                Toast.makeText(getApplicationContext(), "URL Not Found", Toast.LENGTH_LONG).show();
            if (checkConRefused)
                Toast.makeText(getApplicationContext(), "Connection Refused", Toast.LENGTH_LONG).show();
        }
    }



    //perform https connection in background and show the response when finished(on post execute method).
    // Source: http://www.codexpedia.com/android/asynctask-and-httpurlconnection-sample-in-android/
    private class ConnectTaskHttps extends AsyncTask<Void, Void, String>{
        @Override
        protected String doInBackground(Void... params) {

            URL url;
            HttpsURLConnection con = null;
            response="";
            try{
                url = new URL(urlString);
                con = (HttpsURLConnection) url.openConnection();
                con.setConnectTimeout(10 * 1000);
                if(con.getResponseCode() == HttpsURLConnection.HTTP_OK){
                        checkURLFound=true;
                }else {checkURLFound=false;}

                String contentType = con.getHeaderField("Content-Type");
                image = contentType.startsWith("image/");
                if (image)
                { InputStream in = new URL(urlString).openStream();
                    bmp = BitmapFactory.decodeStream(in);
                    in.close();
                }

                else {
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String inputBuffer;

                    while ((inputBuffer = in.readLine()) != null)
                        response+=inputBuffer;
                    in.close();
                }
            }

            catch (Exception e) {
                e.printStackTrace();
                if (e instanceof SocketTimeoutException)
                    checkTimeOut=true;
                if (e instanceof ConnectException)
                    checkConRefused=true;
            }


            finally {
                    if (con != null)
                {con.disconnect();}
            }

            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            if(!image){

                textView.setText(result);
                imageview.setVisibility(View.GONE);
                textView.setVisibility(View.VISIBLE);
            }
            else{ imageview.setImageBitmap(bmp);
                textView.setVisibility(View.GONE);
                imageview.setVisibility(View.VISIBLE);
            }

            if (checkTimeOut)
                Toast.makeText(getApplicationContext(), "Connection Timeout", Toast.LENGTH_LONG).show();
            if (!checkURLFound)
                Toast.makeText(getApplicationContext(), "URL Not Found", Toast.LENGTH_LONG).show();
            if (checkConRefused)
                Toast.makeText(getApplicationContext(), "Connection Refused", Toast.LENGTH_LONG).show();

        }
    }

    private boolean doubleBackToExitPressedOnce = false;

    @Override
    protected void onResume() {
        super.onResume();
        this.doubleBackToExitPressedOnce = false;
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press twice to exit", Toast.LENGTH_SHORT).show();

        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        doubleBackToExitPressedOnce = false;
                    }
                }, 2000);

    }





}

