package Searching;

import Indexing.PostingDocData;
import Indexing.PostingTermData;
import Indexing.TermData;
import Parse.IntWrapper;

import java.io.*;
import java.util.*;

public abstract class ARanker implements IRanker  {

    protected HashMap<String, TermData> _corpusDictionary;
    protected List<String> _cities;
    protected int _totalDocNum;
    protected double _avgDocLength;
    protected boolean _stemming;
    protected String _outputPath;

    protected final String _termsPosting = "FinalPosting";
    protected final String _docsPosting = "DocumentPosting";
    protected final String _stem = "STEM";
    protected final String _txt = ".txt";


    protected ARanker(HashMap<String, TermData> _corpusDictionary, List<String> _cities, int _totalDocNum, double _avgDocLength, boolean _stemming, String _outputPath) {
        this._corpusDictionary = _corpusDictionary;
        this._cities = _cities;
        this._totalDocNum = _totalDocNum;
        this._avgDocLength = _avgDocLength;
        this._stemming = _stemming;
        this._outputPath = _outputPath;
    }

    /**
     * method to retrieve all data of the query's terms
     * @param query - the map of the query
     * @return - map of term and its posting data
     */
    protected HashMap<String, List<PostingTermData>> extractTermsData(HashMap<String, IntWrapper> query){
        String path = _outputPath + _termsPosting;
        if (_stemming){
            path = path + _stem;
        }
        path = path + _txt;

        HashMap<String, List<PostingTermData>> toReturn = new HashMap<>();

        try (
                RandomAccessFile ra = new RandomAccessFile(path, "r");
        ) {
            for (String term :
                    query.keySet()){
                if (!_corpusDictionary.containsKey(term)){
                    continue;
                }

                long pointer = _corpusDictionary.get(term).getM_pointer();
                ra.seek(pointer);
                String allPostingData = ra.readLine();
                int start = allPostingData.indexOf("#~");
                if (start <=0 ){
                    System.out.println("bad pointer!!");
                    continue;
                }

                List<PostingTermData> totalTermOccurrences = new ArrayList<>();
                allPostingData = allPostingData.substring(start+2, allPostingData.length()-1);
                String[] splitByDoc = allPostingData.split(",");

                for (String aSplitByDoc : splitByDoc) {
                    String[] splitByAttributes = aSplitByDoc.split(" ");
                    PostingTermData termData = new PostingTermData(splitByAttributes[0], Integer.valueOf(splitByAttributes[1]),
                            Byte.valueOf(splitByAttributes[2]), Byte.valueOf(splitByAttributes[3]));

                    totalTermOccurrences.add(termData);
                }

                toReturn.put(term, totalTermOccurrences);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return toReturn;
    }

    /**
     * with a given map of terms and where they occurred, returns a map of docs and terms with their data
     * @param termsData - the mapping of terms and where they occurred
     * @return - mapping of docs and terms with their data
     */
    protected HashMap<String, HashMap<String, PostingTermData>> getDocumentsTerms(HashMap<String, List<PostingTermData>> termsData){
        HashMap<String, HashMap<String, PostingTermData>> toReturn = new HashMap<>();

        for (String term :
                termsData.keySet()){
            List<PostingTermData> termsList = termsData.get(term);

            for (PostingTermData data :
                    termsList){
                HashMap<String, PostingTermData> documentTerms = toReturn.computeIfAbsent(data.get_doc(), k -> new HashMap<>()); // getting the terms of the doc if exist
                documentTerms.put(term, data);
            }
        }
        return toReturn;
    }

    /**
     * given an ordered set of docNums will return thier posting data
     * @param docNums - ordered set of doc numbers
     * @return - set of the corresponding posting data
     */
    protected Set<RetrievedDocument> docsMatchingCity(TreeSet<Integer> docNums){
        Set<RetrievedDocument> matchingDoc = new LinkedHashSet<>();
        String path = _outputPath + _docsPosting;
        if (_stemming){
            path = path +_stem;
        }
        path = path+_txt;

        try (
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"))
                ){
            int lineCount = 0;
            for (Integer doc :
                    docNums){
                String data = null;
                while (lineCount<=doc){
                    data = br.readLine();
                    lineCount++;
                }

                if (data == null){
                    System.out.println("Error reading document posting");
                    throw new IOException();
                }

                String[] docData = data.split(" ");
                short startLine = Short.valueOf(docData[0]);
                short endLine = Short.valueOf(docData[1]);
                String relative = docData[2];
                int unique = Integer.valueOf(docData[3]);
                int maxTF = Integer.valueOf(docData[4]);
                int length = Integer.valueOf(docData[5]);
                String city = docData[6];

                if (_cities==null || _cities.contains(city)){
                    RetrievedDocument retrievedDocument = new RetrievedDocument();
                    retrievedDocument.set_city(city);
                    retrievedDocument.set_docNum(String.valueOf(doc));
                    retrievedDocument.set_length(length);

                    matchingDoc.add(retrievedDocument);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return matchingDoc;
    }

    /**
     * method that adds rank BM25 rank to set of retrieved docs
     * @param allDocs - the entire docs with matching query terms
     * @param cityDocs - docs that match
     */
    protected void rankByBM25(HashMap<String, HashMap<String, PostingTermData>> allDocs, Set<RetrievedDocument> cityDocs, HashMap<String, IntWrapper> query, double value){

        double k = 2;
        double b = 0.75;

        for (RetrievedDocument document:
                cityDocs){
            HashMap<String, PostingTermData> docMatchingTerms = allDocs.get(document.get_docNum());

            if (docMatchingTerms==null) // should never enter here
                continue;

            double rank = 0;
            for (String termInDoc:
                    docMatchingTerms.keySet()){
                int termCount = docMatchingTerms.get(termInDoc).get_termOccurrences();
                double normalizedDocLength = (document._length/_avgDocLength);

                int wordQueryCount = query.get(termInDoc).get_value();
                double numerator = (k+1)*termCount;
                double denominator = termCount + k*(1-b+b*normalizedDocLength);
                double normalizedDF = Math.log10((_totalDocNum+1)/_corpusDictionary.get(termInDoc).getM_df());

                rank += (wordQueryCount*(numerator/denominator)*normalizedDF);
            }
            document.add_rank(value*rank);
        }
    }

}
