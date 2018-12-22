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
    int _docAmount;
    HashMap<String, TermData> _dictionary;

    public Searcher(IRanker ranker, HashSet<String> stopWords, boolean stemming){
        _ranker = ranker;
        _stopWords = stopWords;
        _stemming = stemming;
    }

    public PriorityQueue<RetrievedDocument> getRelevantDocuments(String query, List<String> cities) {
        HashMap<String, IntWrapper> parsedQuery = Parser.parseQuery(query, _stemming, _stopWords);
        return _ranker.rank(parsedQuery, cities);
    }

/*
    public static void main(String[] args){
        String query = "polytechnic China trailblazer";
        HashSet<String> stopWords = ReadFile2.getStopWords("C:\\Users\\חגי קלינהוף\\Desktop\\שנה ג'\\סמסטר ה'\\אחזור מידע\\פרויקט מנוע\\Part 1 tests\\corpus\\");

        IRanker r =new RankerNoSemantics()
        Searcher s = new Searcher()
    }
*/
}
