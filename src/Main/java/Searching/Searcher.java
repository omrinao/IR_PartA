package Searching;

import FileReading.ReadFile2;
import Indexing.TermData;
import Parse.IntWrapper;
import Parse.Parser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

public class Searcher {

    private IRanker _ranker;            // strategy design pattern used to rank documents
    private boolean _stemming;          // weather stemming is required
    private HashSet<String> _stopWords; // stop word of the given corpus

    public Searcher(IRanker ranker, HashSet<String> stopWords, boolean stemming){
        _ranker = ranker;
        _stopWords = stopWords;
        _stemming = stemming;
    }

    public PriorityQueue<RetrievedDocument> getRelevantDocuments(String query, List<String> cities) {
        HashMap<String, IntWrapper> parsedQuery = Parser.parseQuery(query, _stemming, _stopWords);
        System.out.println("Finished query processing");
        return _ranker.rank(parsedQuery, cities);
    }
}
