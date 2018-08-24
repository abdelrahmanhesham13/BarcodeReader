package net.thewassa.barcodereader;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button scanBtn, search, result, back;
    private EditText editText;
    private ProgressBar progressBar;
    private TextView internet;
    private static final String ticketUrl = "http://thewassa.com/resAndroid.php";
    private static final String updateUrl = "http://thewassa.com/updateUsed.php";
    String scanContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        scanBtn = (Button) findViewById(R.id.scan_button);
        editText = (EditText) findViewById(R.id.edit_text);
        result = (Button) findViewById(R.id.result);
        search = (Button) findViewById(R.id.search);
        back = (Button) findViewById(R.id.back);
        internet = (TextView) findViewById(R.id.internet);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        search.setOnClickListener(this);
        scanBtn.setOnClickListener(this);
        back.setOnClickListener(this);
        result.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.scan_button) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                IntentIntegrator scanIntegrator = new IntentIntegrator(this);
                scanIntegrator.initiateScan();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 3);
            }
        } else if (view.getId() == R.id.search) {
            scanContent = editText.getText().toString();
            if (!TextUtils.isEmpty(scanContent) && scanContent != null) {
                try {
                    Uri builder = Uri.parse(ticketUrl)
                            .buildUpon().appendQueryParameter("ticket", scanContent).build();
                    URL url = new URL(builder.toString());
                    ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                    if (networkInfo != null && networkInfo.isConnected()) {
                        new TicketAsyncTask().execute(url);
                    } else {
                        internet.setText(getString(R.string.no_internet));
                        internet.setVisibility(View.VISIBLE);
                        search.setVisibility(View.INVISIBLE);
                        scanBtn.setVisibility(View.INVISIBLE);
                        editText.setVisibility(View.INVISIBLE);
                        back.setVisibility(View.VISIBLE);
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            } else {
                showSnackBarMessage(getString(R.string.enter_ticket));
            }
        } else if (view.getId() == R.id.back) {
            editText.setText("");
            editText.setVisibility(View.VISIBLE);
            search.setVisibility(View.VISIBLE);
            scanBtn.setVisibility(View.VISIBLE);
            back.setVisibility(View.INVISIBLE);
            internet.setVisibility(View.INVISIBLE);
            result.setVisibility(View.INVISIBLE);
        } else if (view.getId() == R.id.result) {
            if (result.getText().equals("التذكرة صالحة للاستخدام")) {
                back.setVisibility(View.INVISIBLE);
                result.setVisibility(View.INVISIBLE);
                try {
                    Uri builder = Uri.parse(updateUrl)
                            .buildUpon().appendQueryParameter("ticket", scanContent).build();
                    URL url = new URL(builder.toString());
                    ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                    if (networkInfo != null && networkInfo.isConnected()) {
                        new ValidAsyncTask().execute(url);
                    } else {
                        internet.setText(getString(R.string.no_internet));
                        internet.setVisibility(View.VISIBLE);
                        search.setVisibility(View.INVISIBLE);
                        scanBtn.setVisibility(View.INVISIBLE);
                        editText.setVisibility(View.INVISIBLE);
                        back.setVisibility(View.VISIBLE);
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanningResult != null) {
            scanContent = scanningResult.getContents();
            String scanFormat = scanningResult.getFormatName();
            try {
                Uri builder = Uri.parse(ticketUrl)
                        .buildUpon().appendQueryParameter("ticket", scanContent).build();
                URL url = new URL(builder.toString());
                ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    new TicketAsyncTask().execute(url);
                } else {
                    internet.setText(getString(R.string.no_internet));
                    internet.setVisibility(View.VISIBLE);
                    search.setVisibility(View.INVISIBLE);
                    scanBtn.setVisibility(View.INVISIBLE);
                    editText.setVisibility(View.INVISIBLE);
                    back.setVisibility(View.VISIBLE);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        } else {
            Toast toast = Toast.makeText(getApplicationContext(),
                    getString(R.string.nothing), Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 3: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    IntentIntegrator scanIntegrator = new IntentIntegrator(this);
                    scanIntegrator.initiateScan();

                } else {

                    Toast.makeText(this, getString(R.string.denied), Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    private class TicketAsyncTask extends AsyncTask<URL, Void, String> {

        HttpURLConnection conn = null;
        InputStream in = null;

        @Override
        protected void onPreExecute() {
            editText.setVisibility(View.INVISIBLE);
            search.setVisibility(View.INVISIBLE);
            scanBtn.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(URL... urls) {
            URL url = urls[0];
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("GET");

                conn.connect();
                in = conn.getInputStream();

                StringBuilder output = new StringBuilder();
                if (in != null) {
                    InputStreamReader inputStreamReader = new InputStreamReader(in, Charset.forName("UTF-8"));
                    BufferedReader reader = new BufferedReader(inputStreamReader);
                    String line = reader.readLine();
                    while (line != null) {
                        output.append(line);
                        line = reader.readLine();
                    }
                }
                return output.toString();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            progressBar.setVisibility(View.INVISIBLE);
            if (s.contains("التذكرة صالخة للاستخدام")) {
                editText.setVisibility(View.INVISIBLE);
                search.setVisibility(View.INVISIBLE);
                scanBtn.setVisibility(View.INVISIBLE);
                result.setText(getString(R.string.valid));
                result.setBackgroundColor(Color.GREEN);
                result.setVisibility(View.VISIBLE);
                back.setVisibility(View.VISIBLE);
            } else if (s.contains("التذكرة غير صالحة للأستخدام")) {
                editText.setVisibility(View.INVISIBLE);
                search.setVisibility(View.INVISIBLE);
                scanBtn.setVisibility(View.INVISIBLE);
                result.setText(getString(R.string.invalid));
                result.setBackgroundColor(Color.RED);
                result.setVisibility(View.VISIBLE);
                back.setVisibility(View.VISIBLE);
            } else {
                editText.setVisibility(View.INVISIBLE);
                search.setVisibility(View.INVISIBLE);
                scanBtn.setVisibility(View.INVISIBLE);
                internet.setVisibility(View.VISIBLE);
                internet.setText(getString(R.string.wrong));
                back.setVisibility(View.VISIBLE);
            }
        }
    }

    private class ValidAsyncTask extends AsyncTask<URL, Void, String> {

        HttpURLConnection conn = null;
        InputStream in = null;

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(URL... urls) {
            URL url = urls[0];
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("GET");

                conn.connect();
                in = conn.getInputStream();

                Scanner scanner = new Scanner(in);
                scanner.useDelimiter("\\A");

                boolean hasInput = scanner.hasNext();
                if (hasInput) {
                    return scanner.next();
                } else {
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            Toast.makeText(MainActivity.this, getString(R.string.done), Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.INVISIBLE);
            editText.setText("");
            editText.setVisibility(View.VISIBLE);
            search.setVisibility(View.VISIBLE);
            scanBtn.setVisibility(View.VISIBLE);
            back.setVisibility(View.INVISIBLE);
            internet.setVisibility(View.INVISIBLE);
            result.setVisibility(View.INVISIBLE);
        }
    }

    private void showSnackBarMessage(String message) {

        if (findViewById(android.R.id.content) != null) {

            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
        }
    }


}
