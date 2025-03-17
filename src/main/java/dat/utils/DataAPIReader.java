package dat.utils;


import com.fasterxml.jackson.databind.ObjectMapper;
import dat.exceptions.ApiException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DataAPIReader
{
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public String getDataFromClient(String url)
    {
        try
        {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200)
            {
                return response.body();
            }
            else
            {
                throw new ApiException(response.statusCode(), "GET request failed. Status code: " + response.statusCode());
                //System.out.println("GET request failed. Status code: " + response.statusCode());
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error fetching data from API", e);
        }
    }
}

