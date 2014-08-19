package com.example.ariary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

/** MainActivity or the application.
 *
 * @author Marius Rabenarivo <marius@espa-tana.mg>
 *
 */
public class MainActivity extends Activity {
    /** JSONArray containing the currencies. */
    private JSONArray currencies;
    /** JSONArray containing the date. */
    private JSONObject date;
    /** Layout of the MainActivity. */
    private LinearLayout layout;
    /** TextView for the date. */
    private TextView dateTextView;
    /** ListView for the currencies. */
    private ListView currenciesListView;
    /** Message TextView. */
    private TextView msgTextView;
    /** URL of the back-end. */
    private static final String URL = "http://backend-mspmada.rhcloud.com";
    /** id of the currencies in the JSON response. */
    private static final String CURRENCIES = "currencies";
    /** id of the date in the JSON response. */
    private static final String DATE = "date";
    /** Status code of the HTTP response. */
    private int responseStatus;
    /** ProgressDialog for displaying progression of synchronization. */
    private ProgressDialog progressDialog;
    /** Retry Button on error. */
    private Button retryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layout = (LinearLayout) findViewById(R.id.layout_id);

        currenciesListView = new ListView(this);
        currenciesListView.setLayoutParams(new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT)
        );

        if (isConnected()) {
            new HttpAsyncTask(this).execute(URL);

        } else {
            msgTextView = new TextView(this);
            msgTextView.setLayoutParams(
                    new LinearLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT)
                    );
            msgTextView.setText(
                    getResources().getString(R.string.not_connected_text));

            layout.addView(msgTextView);
        }
    }
    /** Async Task for sending Http Request.
    *
    * @author Marius
    *
    */
   private class HttpAsyncTask extends AsyncTask<String, Void, String> {
       /** Variable for conserving the provided context. */
       private Activity context;
       /** Constructor of the HttpAsyncTask.
        *
        * @param pContext provided context
        */
       public HttpAsyncTask(final Activity pContext) {
           this.context = pContext;
       }
       @Override
       protected void onPreExecute() {
           progressDialog = new ProgressDialog(context);
           context.showDialog(0);
       }
       @Override
       protected String doInBackground(final String... urls) {
           return POST(urls[0]);

       }
       // onPostExecute displays the results of the AsyncTask.
       @Override
       protected void onPostExecute(final String result) {
           progressDialog.dismiss();
           JSONObject response = null;
           if (responseStatus == HttpStatus.SC_OK) {
               try {
                   response = new JSONObject(result);
               } catch (JSONException e) {
                   e.printStackTrace();
               }

               try {
                   date = response.getJSONObject(DATE);
                   currencies = response.getJSONArray(CURRENCIES);

                   dateTextView = new TextView(context);
                   dateTextView.setLayoutParams(new LinearLayout.LayoutParams(
                           LayoutParams.WRAP_CONTENT,
                           LayoutParams.WRAP_CONTENT));
                   dateTextView.setText(date.getString("day") + "/"
                                       + date.getString("month") + "/"
                                       + "20" + date.getString("year"));

                   layout.removeAllViews();

                   layout.addView(dateTextView);

                   List<HashMap<String, String>> currenciesList =
                           new ArrayList<HashMap<String, String>>();

                   HashMap<String, String> element;
                   JSONObject currency;

                   for (int i = 0; i < currencies.length(); i++) {
                       element = new HashMap<String, String>();
                       try {
                           currency = currencies.getJSONObject(i);
                           element.put("text1", currency.getString("Symbole"));
                           element.put("text2", currency.getString("Value"));
                       } catch (JSONException e) {
                           e.printStackTrace();
                       }

                       currenciesList.add(element);
                   }

                   ListAdapter currenciesAdapter = new SimpleAdapter(
                                           context,
                                           currenciesList,
                                           android.R.layout.simple_list_item_2,
                                           new String[]{
                                               "text1",
                                               "text2"
                                           },
                                           new int[] {
                                               android.R.id.text1,
                                               android.R.id.text2
                                           });
                   currenciesListView.setAdapter(currenciesAdapter);
                   currenciesListView.setBackgroundColor(
                           getResources().getColor(
                                   android.R.color.darker_gray));

                   layout.addView(currenciesListView);
               } catch (JSONException e) {
                   e.printStackTrace();
               }

           } else {
               msgTextView = new TextView(context);
               msgTextView.setLayoutParams(
                       new LinearLayout.LayoutParams(
                           LayoutParams.WRAP_CONTENT,
                           LayoutParams.WRAP_CONTENT)
                       );
               msgTextView.setText(
                       getResources().getString(R.string.error_text));

               retryButton = new Button(context);
               retryButton.setLayoutParams(
                       new LinearLayout.LayoutParams(
                               LayoutParams.WRAP_CONTENT,
                               LayoutParams.WRAP_CONTENT)
                           );
               retryButton.setText(
                       getResources().getString(R.string.retry_button_text));
               retryButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(final View arg0) {
                    HttpAsyncTask.this.execute(URL);

                }
            });
               layout.addView(retryButton);
               layout.addView(msgTextView);
           }

      }
   }
    /**
    *
    * @param url URL of the resource
    * @return result
    */
   private String POST(final String url) {
       InputStream inputStream = null;
       String result = "";
       try {
           HttpClient httpclient = new DefaultHttpClient();
           HttpGet httpGet = new HttpGet(url);
           httpGet.setHeader("Accept", "application/json");
           httpGet.setHeader("Content-type", "application/json");
           HttpResponse httpResponse = httpclient.execute(httpGet);
           responseStatus = httpResponse.getStatusLine().getStatusCode();
           inputStream = httpResponse.getEntity().getContent();
           if (inputStream != null) {
               result = convertInputStreamToString(inputStream);
           } else {
               result = "Did not work!";
           }
       } catch (Exception e) {
           e.printStackTrace();
       }

       return result;
   }
   /** Convert InputStream to String.
    *
    * @param inputStream Input Stream to convert
    * @return String from the Input Stream
    * @throws IOException
    */
   private static String convertInputStreamToString(
                         final InputStream inputStream)
                         throws IOException {
       BufferedReader bufferedReader = new BufferedReader(
               new InputStreamReader(inputStream));
       String line = "";
       String result = "";
       while ((line = bufferedReader.readLine()) != null) {
           result += line;
       }
       inputStream.close();
       return result;
   }
   /** Test the connectivity.
    *
    * @return State of the connectivity
    */
   private boolean isConnected() {
       ConnectivityManager connectivityManager = (ConnectivityManager)
                                   getSystemService(CONNECTIVITY_SERVICE);
       NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
       return networkInfo != null
                             && networkInfo.isAvailable()
                             && networkInfo.isConnected();
   }

   @Override
   public Dialog onCreateDialog(final int id) {
       if (progressDialog == null) {
           progressDialog = new ProgressDialog(this);
           progressDialog.setCancelable(false);
           progressDialog.setTitle(R.string.progress_bar_dialog_title);
           progressDialog.setMessage(
                   getResources().getString(
                           R.string.progress_bar_dialog_message));
           progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
       }
       return progressDialog;
   }
}
