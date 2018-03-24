package com.james.caywidge;

/**
 * Created by James on 2018-03-03.
 */

import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import cz.msebera.android.httpclient.Header;


/**
 * Created by James on 2018-03-02.
 */

public class Cayenne {
    String Email = "";
    String Password = "";

    TextView ErrorBox = null;

    String CayAuthToken = "";
    String BigString = "";

    int zone = 0;

    final HashMap<String, String> hashMap = new HashMap<String, String>();
    final ArrayList <String> SensorArrayList = new ArrayList<String>();

    interface CayRecdAuth{
        void CayRecdAuthHandler_Callback();
    }
    interface CayRecdThing{
        void CayRecdThingHandler_Callback();
    }

    public CayAuthResponder myCayAuthResponder = new CayAuthResponder();
    public CayThingResponder myCayThingResponder = new CayThingResponder();

    private List<CayRecdAuth> CayRecdAuth_Callback = new ArrayList<CayRecdAuth>();
    private List<CayRecdThing> CayRecdThing_Callback = new ArrayList<CayRecdThing>();

    public class CayAuthResponder {
        public void addListener(CayRecdAuth who) {
            CayRecdAuth_Callback.add(who);
        }

        void AuthSomething() {
            for (CayRecdAuth listener : CayRecdAuth_Callback) {
                listener.CayRecdAuthHandler_Callback();
            }
        }
    }
    public class CayThingResponder{
            public void addListener(CayRecdThing who) {
                CayRecdThing_Callback.add(who);
            }

        void ThingSomething(){
            for (CayRecdThing listener : CayRecdThing_Callback) {
                listener.CayRecdThingHandler_Callback();
            }
            CayRecdThing_Callback.clear();  // only callback once ???
        }
    }

    public Cayenne(String CayEmail, String CayPassword, int Zone, TextView CayErrorBox) {
        Email = CayEmail;
        Password = CayPassword;
        ErrorBox = CayErrorBox;
        zone = Zone;
    }

    public void UpdateStatus(Integer statusCode, String str) {
        AddtoBigBox("(" + String.valueOf(statusCode) + ") " + str);
    }

    public void AddtoBigBox(String what) {
        BigString = "=> " + what + "\r\n" + BigString;
        if (ErrorBox != null){
            ErrorBox.setText(BigString);
        }
    }

    interface CayDataPointRecd{
        void CayDataPointRecdHandler_Callback();
    }

    public final class CayDataPoint  {
        double v = 0;
        String ts = "";
        String unit = "";
        String device_type = "";
        Date Date_ts = null;
        String sensorID = "";
        String deviceID = "";
        TextView OutputBox;
        Date reqSend = null;
        Date reqRecv = null;
        int Pending = 0;
        String sensorname = "";
        public CayDataPointResponder myResponse;

        private List<CayDataPointRecd> CayDataPoint_Callbacks   = new ArrayList<CayDataPointRecd>();

        public CayDataPoint(String devID, String senID, TextView CayPointOutputBox) {
            deviceID = devID;
            sensorID = senID;
            OutputBox = CayPointOutputBox;
            myResponse = new CayDataPointResponder();
        }

        public class CayDataPointResponder{

            public void addListener(CayDataPointRecd who) {
                CayDataPoint_Callbacks.add(who);
            }

            void SomethingHappened(){
                for (CayDataPointRecd listener : CayDataPoint_Callbacks){
                    listener.CayDataPointRecdHandler_Callback();
                }
            }
        }

        public void update() {

            sensorname = "";
            String key = deviceID+sensorID;
            if (hashMap.containsKey(key)){
                sensorname = hashMap.get(key);
            }

            String urlString = "https://platform.mydevices.com/v1.1/telemetry/" + deviceID +
                    "/sensors/" + sensorID + "/summaries?type=" + "latest";

            AsyncHttpClient getCayenne = new AsyncHttpClient();
            getCayenne.addHeader("Authorization", "Bearer " + CayAuthToken);
            reqSend = new Date();
            Pending = 1;
            getCayenne.get(urlString, new AsyncHttpResponseHandler() {

                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                    // called when response HTTP status is "200 OK"
                    try {
                        reqRecv = new Date();
                        Pending = 0;
                        JSONArray ja = new JSONArray(new String(response));
                        if (ja.length() > 0 ){
                            JSONObject jo = ja.getJSONObject(0);

                            v = jo.getDouble("v");
                            ts = jo.getString("ts");
                            unit = jo.getString("unit");
                            device_type = jo.getString("device_type");
                            Date_ts = getjavaDateTime(ts);

                            write_it();
                        }

                    } catch (Exception e) {
                        UpdateStatus(statusCode, "CayDataPoint " + new String(response) + " " + e.getMessage());
                    }

                    myResponse.SomethingHappened();
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                    reqRecv = new Date();
                    Pending = 0;
                    UpdateStatus(statusCode, "CayDataPoint " + e.getMessage() + ", t=" + String.valueOf(reqRecv.getTime() - reqSend.getTime()) + "ms");
                    myResponse.SomethingHappened();
                }
            });
        }

        public String write_it_format(){

            String box = "";

            if (Date_ts != null){
                long old = howold();

                if (old < 100){
                    box = String.valueOf(this.v) + this.unit  + " -" + String.valueOf(old) + "s";
                } else if (old < 6000){
                    box = String.valueOf(this.v)  + this.unit  + " -" + String.valueOf(old/60) + "m";
                } else {
                    box = String.valueOf(this.v) + this.unit  +  " -" + String.valueOf(old/3600) + "h";
                }
                //String details =  String.valueOf(this.v) + " " + this.unit + "  " + hmmssatime(this.Date_ts) + ", -" + String.valueOf(old) + "s";
                //String details =  String.valueOf(this.v) + " " + this.unit +  ", -" + box;
            }

            return box;
        }

        public void write_it() {

            String x = write_it_format();

            long old = howold();
            String details =  String.valueOf(this.v) + " " + this.unit + "  " + hmmssatime(this.Date_ts) + ", -" + String.valueOf(old) + "s";
            String box = "";
            if (old < 100){
                box = String.valueOf(this.v) + " -" + String.valueOf(old) + "s";
            } else if (old < 6000){
                box = String.valueOf(this.v) + " -" + String.valueOf(old/60) + "m";
            } else {
                box = String.valueOf(this.v) + " -" + String.valueOf(old/3600) + "h";
            }

            if (OutputBox != null) {
                OutputBox.setText(box);
            }
            AddtoBigBox(this.sensorname + " " + details + ", t=" + String.valueOf(reqRecv.getTime() - reqSend.getTime()) + "ms");
        }

        public SimpleDateFormat longDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        public SimpleDateFormat hmmssaDateFormat = new SimpleDateFormat("h:mm:ss a");
        public SimpleDateFormat MMMddhhmmDateFormat = new SimpleDateFormat("MMMdd hh:mm aa");

        public Date getjavaDateTime(String ts) {
            Date parsedate = new Date();
            try {
                parsedate = longDateFormat.parse(ts);
            } catch (ParseException ex) {
            }
            return new Date(parsedate.getTime() + zone * 3600000);
        }

        public long getjavaDateTimeaslong(String ts) {
            return getjavaDateTime(ts).getTime();
        }

        public String MMMddhhmmtime(Date Date_ts) {
            return MMMddhhmmDateFormat.format(Date_ts.getTime() );
        }

        public String hmmssatime(Date Date_ts) {
            return hmmssaDateFormat.format(new Date(Date_ts.getTime()));
        }

        public long howold() {
            Date now = new Date();
            return (now.getTime() -  this.Date_ts.getTime()) / 1000;
        }

        public String sync_update(){

            String urlConstructor = "https://platform.mydevices.com/v1.1/telemetry/" + deviceID +
                    "/sensors/" + sensorID + "/summaries?type=" + "latest";

            String rawValue = "";

            sensorname = "";
            String key = deviceID+sensorID;
            if (hashMap.containsKey(key)){
                sensorname = hashMap.get(key);
            }

            try {
                reqSend = new Date();
                Pending = 1;
                URL url2 = new URL(urlConstructor);
                URLConnection con2 = url2.openConnection();
                HttpURLConnection http2 = (HttpURLConnection)con2;
                http2.setRequestMethod("GET");
                http2.setRequestProperty("Authorization", "Bearer " + CayAuthToken);
                http2.connect();
                reqRecv = new Date();
                Pending = 0;

                try {

                    rawValue = parseResponse(http2.getInputStream(), "UTF-8");
                    JSONArray ja = new JSONArray(new String(rawValue));
                    if (ja.length() > 0 ) {
                        JSONObject jo = ja.getJSONObject(0);

                        v = jo.getDouble("v");
                        rawValue = jo.getString("v");
                        ts = jo.getString("ts");
                        unit = jo.getString("unit");
                        device_type = jo.getString("device_type");
                        Date_ts = getjavaDateTime(ts);
                    }

                    http2.disconnect();


                } catch (Exception e) {
                    UpdateStatus(0, e.getMessage());
                }
            } catch (Exception e) {
                UpdateStatus(0, e.getMessage());
            }
            return write_it_format();
        }

        public void updategraph(GraphView CayGraph, int leftorright, int linecolor,Double Lower, Double Upper) {

            Date start;
            long startdate;
            long enddate;

            final GraphView graph = CayGraph;
            final int lor = leftorright;  // 0 for primary/left scale, 1 for right/secondary scale
            final int col = linecolor;
            final Double lower = Lower;
            final Double upper = Upper;

            start = new Date();
            enddate = start.getTime();
            startdate = enddate - 24 * 3600000;   // 24 hours of data in ms

            String urlString = "https://platform.mydevices.com/v1.1/telemetry/" + deviceID +
                    "/sensors/" + sensorID + "/summaries?type=custom&startDate=" + String.valueOf(startdate) + "&endDate=" + String.valueOf(enddate);

            AsyncHttpClient getCayenne = new AsyncHttpClient();
            getCayenne.addHeader("Authorization", "Bearer " + CayAuthToken);
            reqSend = new Date();

            getCayenne.get(urlString, new AsyncHttpResponseHandler() {

                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                    // called when response HTTP status is "200 OK"
                    try {
                        reqRecv = new Date();
                        JSONArray ja = new JSONArray(new String(response));

                        List<DataPoint> dataPoints = new ArrayList<DataPoint>(ja.length() - 1);

                        JSONObject first = ja.getJSONObject(0);
                        JSONObject last = ja.getJSONObject(ja.length() - 1);

                        long firsttime = getjavaDateTimeaslong(first.getString("ts"));
                        long lasttime = getjavaDateTimeaslong(last.getString("ts"));

                        String unit = first.getString("unit");

                        Double min = ja.getJSONObject(0).getDouble("v");
                        Double max = min;

                        for (int i = 0; i < ja.length(); i++) {
                            JSONObject jo = ja.getJSONObject(ja.length() - i - 1);
                            v = jo.getDouble("v");
                            if (v < min){min = v;}
                            if (v > max) {max = v;}

                            ts = jo.getString("ts");
                            dataPoints.add(new DataPoint(getjavaDateTimeaslong(ts), v));
                        }
                        Double upperbound = 0.0;
                        Double lowerbound = 0.0;

                        if ((upper - lower) < 0.1){
                            upperbound = max;
                            lowerbound = min;
                        } else {
                            upperbound = upper;
                            lowerbound = lower;
                        }

                        LineGraphSeries<DataPoint> ser = new LineGraphSeries<DataPoint>(dataPoints.toArray(new
                                DataPoint[dataPoints.size()]));

                        String toptitle = MMMddhhmmtime(new Date(lasttime)) + "   <<<<< >>>>>   "+ MMMddhhmmtime(new Date(firsttime));

                        graph.setTitle(toptitle);
                        graph.getGridLabelRenderer().setNumHorizontalLabels(4);
                        graph.getGridLabelRenderer().setNumVerticalLabels(5);

                        graph.getGridLabelRenderer().setVerticalAxisTitle( unit);
                        //graph.getGridLabelRenderer().setHorizontalAxisTitle( MMMddhhmmtime(getjavaDateTime(last.getString("ts"))) + " + hours");

                        graph.getGridLabelRenderer().setLabelFormatter( new DateAsXAxisLabelFormatter(null, DateFormat.getTimeInstance(DateFormat.SHORT)));
                        graph.getViewport().setMinX(lasttime+(firsttime-lasttime)*.75);
                        graph.getViewport().setMaxX(firsttime);

                        //graph.getGridLabelRenderer().setHumanRounding(false);

                        graph.getViewport().setScalable(true);
                        graph.getViewport().setScrollable(true);        // scroll X axis to see 24 hours in 4 hour window
                        //graph.getViewport().setScalableY(true);
                        //graph.getViewport().setScrollableY(true);


                        //graph.getViewport().setXAxisBoundsManual(true);
                        //graph.getViewport().setMinX(hoursback - 4); // hoursback should contain highest value in series, about 24
                        //graph.getViewport().setMaxX(hoursback);     // display 4 hours originally at recent 8 hours

                        if (lor == 0) {
                            graph.addSeries(ser);
                            ser.setColor(col);
                            graph.getViewport().setYAxisBoundsManual(true);
                            //graph.getViewport().setMinY(0);
                            //graph.getViewport().setMaxY(80);
                            graph.getViewport().setMinY(lowerbound);
                            graph.getViewport().setMaxY(upperbound);

                        } else {
                            graph.getSecondScale().addSeries(ser);
                            graph.getSecondScale().setMinY(lowerbound);
                            graph.getSecondScale().setMaxY(upperbound);
                            ser.setColor(col);
                            graph.getGridLabelRenderer().setVerticalLabelsSecondScaleColor(col);
                        }

                        String xxx = String.valueOf(ja.length()) + " points, t=" + String.valueOf(reqRecv.getTime() - reqSend.getTime()) + "ms";
                        AddtoBigBox(xxx);

                    } catch (Exception e) {
                        UpdateStatus(0, e.getMessage());
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                    reqRecv = new Date();
                    Pending = 0;
                    UpdateStatus(statusCode, e.getMessage());
                }
            });
        }
    }

    public void sync_getThings() {

        InputStream in = null;
        String str = "";
        String raw = "";

        try {
            URL url = new URL("https://platform.mydevices.com/v1.1/things");

            URLConnection con = url.openConnection();
            HttpURLConnection http = (HttpURLConnection)con;
            http.setRequestMethod("GET");
            http.setRequestProperty("Authorization", "Bearer " + CayAuthToken);

            http.connect();

            raw = parseResponse(http.getInputStream(), "UTF-8");
            http.disconnect();

            try {
                JSONArray ja = new JSONArray(new String(raw));
                JSONObject first = ja.getJSONObject(0);

                for (int i = 0; i < ja.length(); i++) {
                    JSONObject jo = ja.getJSONObject(i);
                    String name = jo.getString("name");
                    String active = jo.getString("status");
                    String devID = jo.getString("id");

                    JSONArray children = jo.getJSONArray("children");

                    AddtoBigBox("Device: " + String.valueOf(i) + " " + name + " " + active);

                    if (children != null) {
                        for (int j = 0; j < children.length(); j++) {
                            JSONObject sen = children.getJSONObject(j);
                            String senName = sen.getString("name");
                            String senActive = sen.getString("status");
                            String senID = sen.getString("id");

                            String key = devID + senID;
                            String DevSen = name + " : " + senName;
                            hashMap.put(key, DevSen);
                            SensorArrayList.add(DevSen);

                            AddtoBigBox("  Sensor: " + String.valueOf(j) + " " + DevSen + " " + senActive);

                            // creating a datapoint for everything in the list
                            // no query, but could burn through some memory
                            //
                            CayDataPoint x = new CayDataPoint(devID, senID, null);
                            //
                            //  x.update();

                        }
                    }
                }
            } catch (Exception e){
                UpdateStatus(0,e.getMessage());
            }

        } catch (Exception e) {
            UpdateStatus(0, e.getMessage());
        }

        //CayAuthToken = rawToken;
        //return rawToken;
    }


    public void getThings() {

        String urlString = "https://platform.mydevices.com/v1.1/things";

        AsyncHttpClient getCayenne = new AsyncHttpClient();
        getCayenne.addHeader("Authorization", "Bearer " + CayAuthToken);

        getCayenne.get(urlString, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                // called when response HTTP status is "200 OK"
                try {
                    JSONArray ja = new JSONArray(new String(response));
                    JSONObject first = ja.getJSONObject(0);

                    for (int i = 0; i < ja.length(); i++) {
                        JSONObject jo = ja.getJSONObject(i);
                        String name = jo.getString("name");
                        String active = jo.getString("status");
                        String devID = jo.getString("id");

                        JSONArray children = jo.getJSONArray("children");

                        AddtoBigBox("Device: " + String.valueOf(i) + " " + name + " " + active);

                        if (children != null) {
                            for (int j = 0; j < children.length(); j++) {
                                JSONObject sen = children.getJSONObject(j);
                                String senName = sen.getString("name");
                                String senActive = sen.getString("status");
                                String senID = sen.getString("id");

                                String key = devID + senID;
                                String DevSen = name + " : " + senName;
                                hashMap.put(key, DevSen);
                                SensorArrayList.add(DevSen);

                                AddtoBigBox("  Sensor: " + String.valueOf(j) + " " + DevSen + " " + senActive);

                                // creating a datapoint for everything in the list
                                // no query, but could burn through some memory
                                //
                                CayDataPoint x = new CayDataPoint(devID, senID, null);
                                //
                                //  x.update();

                            }
                        }
                    }
                } catch (Exception e) {
                    UpdateStatus(0, e.getMessage());
                }

                myCayThingResponder.ThingSomething();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                UpdateStatus(statusCode, e.getMessage());
            }
        });
    }


    public void getCayenneAuth() {

        // get auth number from cayenne based on email and password

        CayAuthToken = "";

        AsyncHttpClient getCayenne = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("grant_type", "password");
        params.put("email", Email);
        params.put("password", Password);

        getCayenne.post("https://auth.mydevices.com/oauth/token", params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                // called when response HTTP status is "200 OK"
                UpdateStatus(statusCode, new String(response));

                try {
                    JSONObject jo = new JSONObject(new String(response));

                    CayAuthToken = jo.getString("access_token");

                } catch (Exception e) {
                    UpdateStatus(0, e.getMessage());
                }
                myCayAuthResponder.AuthSomething();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)

                UpdateStatus(statusCode, e.getMessage());
            }
        });
    }

    public void getTime() {

        AsyncHttpClient client = new AsyncHttpClient();

        client.get("https://postman-echo.com/time/now", new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                // called when response HTTP status is "200 OK"
                UpdateStatus(statusCode,new String(response));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                UpdateStatus(statusCode, e.getMessage());
            }
        });
    }

    public String sync_getTime() {

        InputStream in = null;
        String str = "";
        String rawToken = "";

        try {
            URL url = new URL("https://postman-echo.com/time/now");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            try {
                in = new BufferedInputStream(urlConnection.getInputStream());
                str = urlConnection.getResponseMessage();    //readStream(in);
            } finally {
                urlConnection.disconnect();
            }

            AddtoBigBox(in.toString());

        } catch (Exception e) {
            UpdateStatus(0, e.getMessage());
        }
        try {
            rawToken = parseResponse(in, "UTF-8");

        } catch (Exception e){
            UpdateStatus(0, e.getMessage());
        }
        return rawToken;
    }


    public void sync_getCayenneAuth() {

        InputStream in = null;
        String str = "";
        String rawToken = "";

        String bodyConstructor = "{\"grant_type\":\"password\",\"email\":\"" + Email +
                "\",\"password\":\""+ Password + "\"}";

        try {
            URL url = new URL("https://auth.mydevices.com/oauth/token");

            URLConnection con = url.openConnection();
            HttpURLConnection http = (HttpURLConnection)con;
            http.setRequestMethod("POST");
            http.setDoOutput(true);

            byte[] out =  bodyConstructor.getBytes(StandardCharsets.UTF_8);
            int length = out.length;
            http.setFixedLengthStreamingMode(length);
            http.setRequestProperty("content-type", "application/json; charset=UTF-8");
            http.connect();

            try {
                OutputStream os = http.getOutputStream();
                os.write(out);
            } catch (Exception e) {
                UpdateStatus(0, e.getMessage());
            }

            rawToken = parseResponse(http.getInputStream(), "UTF-8");
            http.disconnect();
            JSONObject jo = new JSONObject(rawToken);
            rawToken = jo.getString("access_token");

        } catch (Exception e) {
            UpdateStatus(0, e.getMessage());
        }

        CayAuthToken = rawToken;

    }

    public static String parseResponse(InputStream inputStream, String encoding) throws IOException {
        return readFully(inputStream).toString(encoding);
    }

    public static ByteArrayOutputStream readFully(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos;
    }
}
