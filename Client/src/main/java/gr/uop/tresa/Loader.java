package gr.uop.tresa;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Loader implements Runnable{

    @Override
    public void run() {
        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("http://localhost:8080/load-articles");

        HttpResponse response = null;
        try {
            response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
//            Main.loadLabel.setText("Loaded: " + EntityUtils.toString(entity, StandardCharsets.UTF_8) + "documents successfully!");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
