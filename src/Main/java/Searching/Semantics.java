package Searching;

import Parse.IntWrapper;
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
import java.util.*;

public class Semantics implements Serializable {

    public JsonElement m_JsonE;

    public PriorityQueue<RetrievedDocument> rank(HashMap<String, IntWrapper> query, List<String> cities) {


        return null;
    }

    public ArrayList<String> meanLikeTerms(String term){
        ArrayList<String> toReturn = new ArrayList<>();
        try {

            OkHttpClient okClient = new OkHttpClient();
            HttpUrl.Builder httpURL = Objects.requireNonNull(HttpUrl.parse("https://api.datamuse.com/words?ml=" + term)).newBuilder();
            String url = httpURL.build().toString();
            Request request = new Request.Builder().url(url).build();
            Response response = okClient.newCall(request).execute();
            JsonParser parser = new JsonParser();
            if (response.body() == null)
                return toReturn;
            m_JsonE = parser.parse(response.body().string());



            JsonArray meanLike = m_JsonE.getAsJsonArray();

            for (int i = 0; i < meanLike.size() && i < 5; i++) {
                JsonObject terms = (JsonObject) (meanLike.get(i));
                toReturn.add(terms.get("word").getAsString());
            }
        }
        catch (Exception e){
            System.out.println("Term " + term + " did not yield any synonyms");
        }
        return toReturn;
    }

    public static void main(String[] args) {
         Semantics test = new Semantics();
        //test.rank(null, null);
        ArrayList<String> print = test.meanLikeTerms("encryption");
        for (String s: print) {
            System.out.println(s);
        }


    }
}
