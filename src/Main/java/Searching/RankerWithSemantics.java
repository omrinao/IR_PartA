package Searching;

import Indexing.DocumentDictionary;
import Indexing.PostingTermData;
import Indexing.TermData;
import Parse.IntWrapper;
import Parse.Parser;

import java.io.IOException;
import java.util.*;

public class RankerWithSemantics extends ARanker {

    //private SemanticAid _semantics;
    private Semantics _semantics;
    private HashSet<String> _stopWords;

    public RankerWithSemantics(HashMap<String, TermData> _corpusDictionary, List<String> _cities, DocumentDictionary docDict, boolean _stemming, String _outputPath, HashSet<String> stopWords) throws IOException {
        super(_corpusDictionary, _cities, docDict, _stemming, _outputPath);

        _stopWords = stopWords;
        _semantics = new Semantics(); //new SemanticAid("C:\\Users\\חגי קלינהוף\\Desktop\\Engine Output", 5);
    }

    /**
     * method to rank and retrieve top 50 results based on parameters
     * @param query  - the query
     * @param cities - list of cities constrains, null if no constrains
     * @return - top 50 results
     */
    @Override
    public PriorityQueue<RetrievedDocument> rank(HashMap<String, IntWrapper> query, List<String> cities) {


        HashMap<String, List<PostingTermData>> termsInQuery = this.extractTermsData(query);
        System.out.println("Got all terms");

        // getting all semantics into a 'query' of it's own
        /**/
        StringBuilder semanticsQuery = new StringBuilder();
        for (String word :
                query.keySet()){
            try{
                List<String> lsi = _semantics.meanLikeTerms(word); // getSynonyms(word);
                for (String synonyms :
                        lsi){
                    semanticsQuery.append(synonyms).append(" ");
                }
                if (false)
                    throw new IOException();
            }
            catch (IOException e){
                System.out.println("whoops");
            }
        }

        HashMap<String, IntWrapper> termOfSemantics = Parser.parseQuery(semanticsQuery.toString(), _stemming, _stopWords);
        HashMap<String, List<PostingTermData>> termsInSemantics = this.extractTermsData(termOfSemantics);

        HashMap<String, List<TermInDoc>> docsWithQueryTerms = this.getDocumentsTerms(termsInQuery, 0.8f);
        HashMap<String, List<TermInDoc>> semanticsDocs = this.getDocumentsTerms(termsInSemantics, 0.2f);

        mergeQueries(docsWithQueryTerms, semanticsDocs);
        System.out.println("Sorted terms per document");

        Set<RetrievedDocument> docsMatchingCity = this.docsMatchingCity(docsWithQueryTerms);
        System.out.println("Matched by city");

        this.rankByBM25(docsWithQueryTerms, docsMatchingCity, query, 0.99);
        System.out.println("Ranked by BM25");

        this.rankByPosition(docsWithQueryTerms, docsMatchingCity, query, 0.01);
        System.out.println("Ranked by positioning");

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

    /**
     * method to merge terms of wuery with ones of semantics
     * @param docsWithQueryTerms - the documents of the actual terms
     * @param semanticsQuery - the documents of the semantics terms
     */
    private void mergeQueries(HashMap<String, List<TermInDoc>> docsWithQueryTerms, HashMap<String, List<TermInDoc>> semanticsQuery) {
        for (String docNum :
                docsWithQueryTerms.keySet()){
            if (semanticsQuery.containsKey(docNum)){
                docsWithQueryTerms.get(docNum).addAll(semanticsQuery.get(docNum));
            }
        }
    }
}
