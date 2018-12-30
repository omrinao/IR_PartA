package Searching;

import Indexing.DocumentDictionary;
import Indexing.PostingTermData;
import Indexing.TermData;
import Parse.IntWrapper;
import Parse.Parser;

import java.io.IOException;
import java.util.*;

public class RankerWithSemantics extends ARanker {

    private Semantics _semantics;       // semantic operator
    private HashSet<String> _stopWords; // stop words for synonyms analysis

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
        System.out.println("Got all semantic terms");

        HashMap<String, IntWrapper> queryOfSemantics = Parser.parseQuery(semanticsQuery.toString(), _stemming, _stopWords);
        HashMap<String, IntWrapper> mergedQuery = new HashMap<>(queryOfSemantics);
        mergedQuery.putAll(query);
        HashMap<String, List<PostingTermData>> termsInSemantics = this.extractTermsData(queryOfSemantics);

        HashMap<String, List<TermInDoc>> docsWithQueryTerms = this.getDocumentsTerms(termsInQuery, 0.8f);
        HashMap<String, List<TermInDoc>> semanticsDocs = this.getDocumentsTerms(termsInSemantics, 0.2f);

        mergeDocuments(docsWithQueryTerms, semanticsDocs);
        System.out.println("Sorted terms per document");

        Set<RetrievedDocument> docsMatchingCity = this.docsMatchingCity(docsWithQueryTerms);
        System.out.println("Matched by city");

        this.rankByBM25(docsWithQueryTerms, docsMatchingCity, mergedQuery, 0.89);
        System.out.println("Ranked by BM25");

        this.rankByPosition(docsWithQueryTerms, docsMatchingCity, query, 0.01);
        System.out.println("Ranked by positioning");

        this.cosSim(docsWithQueryTerms, docsMatchingCity, mergedQuery, 0.1);
        System.out.println("Ranked with Cosine Similarity");

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
     * @param docsWithSemanticTerms - the documents of the semantics terms
     */
    private void mergeDocuments(HashMap<String, List<TermInDoc>> docsWithQueryTerms, HashMap<String, List<TermInDoc>> docsWithSemanticTerms) {
        for (String docNum :
                docsWithQueryTerms.keySet()){
            if (docsWithSemanticTerms.containsKey(docNum)){
                docsWithQueryTerms.get(docNum).addAll(docsWithSemanticTerms.get(docNum));
            }
        }
    }
}
