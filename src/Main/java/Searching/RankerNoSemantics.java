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

        HashMap<String, List<PostingTermData>> termsInQuery = this.extractTermsData(query);
        System.out.println("Got all terms");

        /*
        TODO: add documents with city at their text
         */

        HashMap<String, List<TermInDoc>> docsWithQueryTerms = this.getDocumentsTerms(termsInQuery, 1);
        System.out.println("Sorted terms per document");
/*
        TreeSet<Integer> orderedDocNum = new TreeSet<>();
        for (String strNum :
                docsWithQueryTerms.keySet()){
            orderedDocNum.add(Integer.valueOf(strNum));
        }
*/
        Set<RetrievedDocument> docsMatchingCity = this.docsMatchingCity(docsWithQueryTerms);
        System.out.println("Matched by city");

        this.rankByBM25(docsWithQueryTerms, docsMatchingCity, query, 0.79);
        System.out.println("Ranked by BM25");

        this.rankByPosition(docsWithQueryTerms, docsMatchingCity, query, 0.01);
        System.out.println("Ranked by positioning");

        this.cosSim(docsWithQueryTerms, docsMatchingCity, query, 0.2);
        System.out.println("Ranked by Cosine Similarity");

        PriorityQueue<RetrievedDocument> toReturn = new PriorityQueue<>(docsMatchingCity);
        System.out.println("Finished query: " + query.keySet() +" --- Returned " + toReturn.size() + " docs\n");
        if (toReturn.size() > 50){
            PriorityQueue<RetrievedDocument> tmp = toReturn;
            toReturn = new PriorityQueue<>();
            while ( toReturn.size() < 50){
                toReturn.add(tmp.poll());
            }
        }

        return toReturn;
    }
}
