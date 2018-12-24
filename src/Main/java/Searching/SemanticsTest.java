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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

public class SemanticsTest implements Serializable {

    public JsonElement m_JsonE;

    public PriorityQueue<RetrievedDocument> rank(HashMap<String, IntWrapper> query, List<String> cities) {
        try {

            OkHttpClient okClient = new OkHttpClient();
            HttpUrl.Builder httpURL = HttpUrl.parse("https://api.datamuse.com/words?ml=football").newBuilder();
            String url = httpURL.build().toString();
            Request request = new Request.Builder().url(url).build();
            Response response = okClient.newCall(request).execute();
            JsonParser parser = new JsonParser();
            m_JsonE = parser.parse(response.body().string());
        }

        catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    public ArrayList<String> meanLikeTerms(String term){
        JsonArray meanLike = m_JsonE.getAsJsonArray();
        ArrayList<String> toReturn = new ArrayList();
        for (int i = 0; i < meanLike.size() && i < 5; i++) {
            JsonObject terms = (JsonObject) (meanLike.get(i));
            toReturn.add(terms.get("word").getAsString());
        }
        return toReturn;
    }

    public static void main(String[] args) {
        SemanticsTest test = new SemanticsTest();
        test.rank(null, null);
        ArrayList<String> print = test.meanLikeTerms("Hello");
        for (String s: print) {
            System.out.println(s);
        }
    }
}
