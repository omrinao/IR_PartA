package Searching;

import Indexing.DocumentDictionary;
import Indexing.PostingTermData;
import Indexing.TermData;
import Parse.IntWrapper;

import java.io.RandomAccessFile;
import java.util.*;

public class RankerNoSemantics extends ARanker {

    public RankerNoSemantics(HashMap<String, TermData> _corpusDictionary, List<String> _cities, DocumentDictionary docDict, boolean _stemming, String _outputPath) {
        super(_corpusDictionary, _cities, docDict, _stemming, _outputPath);
    }


    @Override
    public PriorityQueue<RetrievedDocument> rank(HashMap<String, IntWrapper> query, List<String> cities){
        PriorityQueue<RetrievedDocument> toReturn = new PriorityQueue<>();

        HashMap<String, List<PostingTermData>> termsInQuery = this.extractTermsData(query);
        System.out.println("Got all terms");

        HashMap<String, HashMap<String, PostingTermData>> docsWithQueryTerms = this.getDocumentsTerms(termsInQuery);
        System.out.println("Sorted terms per document");

        TreeSet<Integer> orderedDocNum = new TreeSet<>();
        for (String strNum :
                docsWithQueryTerms.keySet()){
            orderedDocNum.add(Integer.valueOf(strNum));
        }

        Set<RetrievedDocument> docsMatchingCity = this.docsMatchingCity(orderedDocNum);
        System.out.println("Matched by city");
        System.out.println("Starting to rank");
        this.rankByBM25(docsWithQueryTerms, docsMatchingCity, query, 1);

        toReturn.addAll(docsMatchingCity);
        if (toReturn.size() > 50){
            PriorityQueue<RetrievedDocument> tmp = toReturn;
            toReturn = new PriorityQueue<>();
            while ( toReturn.size() <= 50){
                toReturn.add(tmp.poll());
            }
        }

        return toReturn;
    }
}
