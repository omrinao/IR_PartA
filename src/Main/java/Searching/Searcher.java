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

    IRanker _ranker;
    boolean _stemming;
    HashSet<String> _stopWords;

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
