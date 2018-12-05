package Indexing;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.io.Serializable;
import java.net.UnknownHostException;

public class CityIndex implements Serializable{

    // this object will hold only the information we need (capital, population, currency) from all the API
    public JsonElement m_JsonE;

    public CityIndex() {
        try {

            OkHttpClient okClient = new OkHttpClient();
            HttpUrl.Builder httpURL = HttpUrl.parse("https://restcountries.eu/rest/v2/all?fields=capital;name;population;currencies").newBuilder();
            String url = httpURL.build().toString();
            Request request = new Request.Builder().url(url).build();
            Response response = okClient.newCall(request).execute();
            JsonParser parser = new JsonParser();
            m_JsonE = parser.parse(response.body().string());
        }

        catch (Exception e){

        }

    }


    public String [] getDetails(String city) {
        String tolowerCity = city.toLowerCase();
        String [] citySplit = tolowerCity.split(" ");
        if (citySplit.length > 1) {
            city = "";
            for (int i = 0; i < citySplit.length; i++) {
                citySplit[i] = Character.toUpperCase(citySplit[i].charAt(0)) + citySplit[i].substring(1);
                city += citySplit[i] + " ";
            }
            city = city.substring(0, city.length() - 1);
        }
        else{
            city = Character.toUpperCase(tolowerCity.charAt(0)) + tolowerCity.substring(1);
        }
        String [] details= new String[3];

        if(m_JsonE == null)
            return details;

        JsonArray fullDetailsArray = m_JsonE.getAsJsonArray();
        for (int i = 0; i < fullDetailsArray.size(); i++) {
            JsonObject citiesDetails = (JsonObject) (fullDetailsArray.get(i));
            String capitalCity = citiesDetails.get("capital").getAsString();

            if (capitalCity.equals(city)) {
                details[0] = citiesDetails.get("name").getAsString();
                details[1] = citiesDetails.get("population").getAsString();
                JsonArray JE = citiesDetails.getAsJsonArray("currencies");
                for (Object o : JE) {
                    JsonObject jsonLineItem = (JsonObject) o;
                    JsonElement val=jsonLineItem.get("code");
                    details[2]=val.getAsString();
                }
            }
        }
        return details;
    }

    public static void main(String[] args) {
        CityIndex ci = new CityIndex();
        String[] s = ci.getDetails("moscow");
        for (String g: s
             ) {
            System.out.println(g);
        }
    }

}
