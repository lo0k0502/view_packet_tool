package com.example.quardimu.ui;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.quardimu.R;
import com.example.quardimu.VerticalElement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static com.example.quardimu.MainActivity.mOutputFileDir;


public class RecordsFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {


    private TextView textView;

    private String mUrl = "https://140.123.124.219/~villager/quadIMU/";
    private String mUserName = "student";
    private String mPassword = "";
    private static int bufferLines = 150;


    public static RecordsFragment newInstance() {
        return new RecordsFragment();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_records, container, false);
        textView = root.findViewById(R.id.textView);
        textView.setMovementMethod(new ScrollingMovementMethod());
        File dir = new File(mOutputFileDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        Button lsButton = root.findViewById(R.id.ls_button);
        Button lsServer = root.findViewById(R.id.ls_server_button);
        Button rmButton = root.findViewById(R.id.rm_button);
        Button pushButton = root.findViewById(R.id.push_button);
        Button clearButton = root.findViewById(R.id.clear_button);
        Button pullButton = root.findViewById(R.id.pull_button);
        VerticalElement.VerticalButton actionButton = root.findViewById(R.id.action_tool);
        final LinearLayout buttonLayout = root.findViewById(R.id.buttonsLayout);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.requireActivity());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        terInit();

        //Buttons behavior
        lsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ls();
            }
        });
        lsServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ls_srv();
            }
        });
        rmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rm();
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                terInit();
            }
        });

        pushButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File dir = new File(mOutputFileDir);
                if (dir.exists()) {
                    File[] files = dir.listFiles();
                    if (files != null && files.length > 0) {
                        terCommand("push");
                        terUpdateInit("pushing... ");
                        for (File file : files) {
                            terUpdate("pushing " + file.getName() + "...");
                            requestUpload(file);
                        }
                    } else {
                      terCommand("push");
                      terOut("");
                    }
                }
            }
        });

        pullButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestDownload();
            }
        });

        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (buttonLayout.getVisibility() == View.GONE) {
                    buttonLayout.setVisibility(View.VISIBLE);
                } else {
                    buttonLayout.setVisibility(View.GONE);
                }

            }
        });

        return root;
    }


    // Terminal actions
    private void terInit() {
        textView.setText("$> ");
    }

    private void terCommand(String cmd) {
        textView.append(cmd + "\n");
    }

    private void terOut(String str) {
        textView.append(str + "\n" + "$> ");
    }
    private void terUpdateInit(String str) {
        textView.append(str);
    }

    private void terUpdate(String str) {
        if (textView.getText() != null) {
            String currentValue = textView.getText().toString();
            String newValue = currentValue.substring(0, currentValue.lastIndexOf("\n")) + "\n";
            textView.setText(newValue);
        }
        textView.append(str);

    }


    //Button actions
    private void requestDownload(){
        terCommand("pull");
        NukeSSLCerts.nuke();
        RequestQueue queue = Volley.newRequestQueue(requireActivity());
        StringRequest stringRequest = new StringRequest(Request.Method.POST, mUrl + "count.php",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(!response.isEmpty()) {
                            int num = Integer.parseInt(response);
                            terUpdateInit("pulling...");
                            for (int i=0;i<num;i++) {
                                downloadFilesInDir(i);
                            }
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> params = new HashMap<>();
                String creds = String.format("%s:%s", mUserName, mPassword);
                String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
                params.put("Authorization", auth);
                return params;
            }
        };
        queue.add(stringRequest);
    }
    private void downloadFilesInDir(final int num){
        NukeSSLCerts.nuke();
        RequestQueue queue = Volley.newRequestQueue(requireActivity());
        StringRequest stringRequest = new StringRequest(Request.Method.POST, mUrl + "filename.php",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        terUpdate("pulling " + response + "...\n$>" );
                        requestFile(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("getNum", String.valueOf(num));
                return params;
            }
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> params = new HashMap<>();
                String creds = String.format("%s:%s", mUserName, mPassword);
                String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
                params.put("Authorization", auth);
                return params;
            }
        };
        queue.add(stringRequest);
    }
    private void requestFile(final String fileName) {
        NukeSSLCerts.nuke();
        RequestQueue requestQueue;

        Cache cache = new DiskBasedCache(requireContext().getCacheDir(), 1024 * 1024 * 10);
        Network network = new BasicNetwork(new HurlStack());

        requestQueue = new RequestQueue(cache, network);
        requestQueue.start();
        String url = mUrl + "records/" + fileName;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    FileOutputStream outputStream = new FileOutputStream(new File(mOutputFileDir, fileName));
                    outputStream.write(response.getBytes());
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        terUpdate("pulling " + fileName + "..." + error.toString() + "\n$>");
                    }
                }) {

            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> params = new HashMap<>();
                String creds = String.format("%s:%s", mUserName, mPassword);
                String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
                params.put("Authorization", auth);
                return params;
            }
        };
        requestQueue.add(stringRequest);
    }


    private void requestUpload(final File file) {

        NukeSSLCerts.nuke();
        RequestQueue queue = Volley.newRequestQueue(requireActivity());
        StringRequest stringRequest = new StringRequest(Request.Method.POST, mUrl + "check.php",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (response.equals("true")) {
                            terUpdate("pushing " + file.getName() + "...exists\n$> ");
                        } else {
                            String line;
                            StringBuilder buffer = new StringBuilder();
                            BufferedReader br;
                            int num =0;
                            try {
                                br = new BufferedReader(new FileReader(file));
                                while ((line = br.readLine()) != null) {
                                    if (num > bufferLines) {
                                        buffer.append("\n").append(line);
                                        uploadData(file.getName(), buffer.toString(), file.length());
                                        buffer = new StringBuilder();
                                        num = 0;
                                    } else {
                                        buffer.append("\n").append(line);
                                        num++;
                                    }
                                }
                                uploadData(file.getName(), buffer.toString(), file.length());
                            } catch (IOException e) {
                                e.printStackTrace();
                                textView.append(e.toString() + "\n$> ");
                            }
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                terUpdate("pushing " + file.getName() + "..." + error.toString() + "\n$> ");
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("getFileName", file.getName());
                return params;
            }

            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> params = new HashMap<>();
                String creds = String.format("%s:%s", mUserName, mPassword);
                String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
                params.put("Authorization", auth);
                return params;
            }
        };
        queue.add(stringRequest);
    }

    private void uploadData(final String fileName, final String data, final long length) {

        NukeSSLCerts.nuke();
        RequestQueue queue = Volley.newRequestQueue(requireActivity());

        StringRequest stringRequest = new StringRequest(Request.Method.POST, mUrl + "upload.php",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (Long.parseLong(response) / length >= 1) {
                            terUpdate("pushing " + fileName + "...100%\n$>");
                        } else {
                            terUpdate("pushing " + fileName + "..." + 100 * Long.parseLong(response) / length + "%");
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                terUpdate("pushing " + fileName + "..." + error.toString() + "\n$>");
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();

                params.put("getFileName", fileName);
                params.put("getData", data);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> params = new HashMap<>();
                String creds = String.format("%s:%s", mUserName, mPassword);
                String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
                params.put("Authorization", auth);
                return params;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy( 50000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(stringRequest);
    }

    private void ls() {
        File dir = new File(mOutputFileDir);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                StringBuilder tmp = new StringBuilder();
                for (File file : files) {
                    tmp.append(file.getName()).append(" #").append(file.length()).append("\n");
                }
                terCommand("ls");
                terOut(tmp.toString());
            }
        }
    }
    private void ls_srv(){
        terCommand("ls_srv");
        NukeSSLCerts.nuke();
        RequestQueue queue = Volley.newRequestQueue(requireActivity());
        StringRequest stringRequest = new StringRequest(Request.Method.POST, mUrl + "list.php",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        terOut(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> params = new HashMap<>();
                String creds = String.format("%s:%s", mUserName, mPassword);
                String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
                params.put("Authorization", auth);
                return params;
            }
        };
        queue.add(stringRequest);
    }


    private void rm() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setMessage(R.string.rm_alert_message);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            public void onClick(DialogInterface dialog, int id) {
                File dir = new File(mOutputFileDir);
                if (dir.exists()) {
                    String[] files = dir.list();
                    if (files != null) {
                        boolean result = false;
                        for (String file : files) {
                            result = new File(dir, file).delete();
                        }
                        if (result) {
                            terCommand("rm");
                        } else {
                            terCommand("rm unsuccessful");
                        }
                        terOut("");
                    }
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        mUrl = sharedPreferences.getString("url", "");
        mUserName = sharedPreferences.getString("usr", "");
        mPassword = sharedPreferences.getString("passwd", "");
    }


    public static class NukeSSLCerts {

        public static void nuke() {
            try {
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }

                            @Override
                            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            }
                        }
                };

                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String arg0, SSLSession arg1) {
                        return true;
                    }
                });
            } catch (Exception ignored) {
            }
        }
    }

}
