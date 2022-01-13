package me.lean.tech.dev;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Properties;

public class Mtls {

    public static void main(String[] args) throws IOException {
        Properties props = new Properties();
        props.load(Mtls.class.getClassLoader().getResourceAsStream("application.properties"));
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String baseUrl = props.getProperty("lean.base.url");
            String uri = baseUrl + "/banks/v1/";
            HttpGet request = new HttpGet(uri);
            // add request headers
            request.addHeader("lean-app-token", props.getProperty("lean.app.token"));
            request.addHeader(HttpHeaders.ACCEPT, "application/json");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                // Get HttpResponse Status
                System.out.println(response.getStatusLine().toString());        // HTTP/1.1 200 OK
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    // return it as a String
                    String result = EntityUtils.toString(entity);
                    System.out.println(result);
                } else {
                    System.out.println("Unexpected error: unable to retrieve response");
                }

            }
        }

    }

}
