package ro.alexp.gear2steps;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpSender
{
    public static int postJson(String urlText, String json) throws Exception
    {
        URL url = new URL(urlText);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        OutputStream os = conn.getOutputStream();
        os.write(json.getBytes("UTF-8"));
        os.flush();
        os.close();

        int code = conn.getResponseCode();

        try
        {
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while (br.readLine() != null)
            {
                // Consumam raspunsul ca sa inchidem curat conexiunea.
            }
            br.close();
        }
        catch (Exception ignored)
        {
        }

        conn.disconnect();
        return code;
    }
}
