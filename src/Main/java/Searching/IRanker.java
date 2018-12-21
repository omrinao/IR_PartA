package Searching;

import Parse.IntWrapper;

import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

public interface IRanker {

    /**
     * mathod to rank and retrieve top 50 results based on parameters
     * @param query - the query
     * @param cities - list of cities constrains, null if no constrains
     * @return - top 50 results
     */
    public PriorityQueue<RetrievedDocument> rank(HashMap<String, IntWrapper> query, List<String> cities);
}
