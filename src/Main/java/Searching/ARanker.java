package Searching;

import Indexing.*;
import Parse.IntWrapper;

import java.io.*;
import java.util.*;

public abstract class ARanker implements IRanker  {

    protected HashMap<String, TermData> _corpusDictionary; // corpus terms dictionary
    protected DocumentDictionary _documentDictionary;      // corpus docs dictionary
    protected List<String> _cities;                        // list of city constrains
    protected int _totalDocNum;                            // total doc number
    protected double _avgDocLength;                        // avg doc length
    protected boolean _stemming;                           // weather stemming required
    protected String _outputPath;                          // path of posting files

    protected final String _termsPosting = "FinalPosting";
    protected final String _docsPosting = "DocumentPosting";
    protected final String _stem = "STEM";
    protected final String _txt = ".txt";


    protected ARanker(HashMap<String, TermData> _corpusDictionary, List<String> _cities, DocumentDictionary docDict, boolean _stemming, String _outputPath) {
        this._corpusDictionary = _corpusDictionary;
        this._cities = _cities;
        this._documentDictionary = docDict;
        this._totalDocNum = docDict.get_totalDocCount();
        this._avgDocLength = docDict.get_avgDocLength();
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
                String word = term;
                if (!_corpusDictionary.containsKey(word)){
                    word = word.toLowerCase();
                    if (!_corpusDictionary.containsKey(word)){
                        word = word.toUpperCase();
                        if (!_corpusDictionary.containsKey(word)){
                            continue;
                        }
                    }

                }

                long pointer = _corpusDictionary.get(word).getM_pointer();
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

                toReturn.put(word, totalTermOccurrences);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return toReturn;
    }

    protected HashMap<String, HashMap<String, PostingTermData>> extractCitiesInText(){
        HashMap<String , HashMap<String, PostingTermData>> toReturn = new HashMap<>();



        return toReturn;
    }

    /**
     * with a given map of terms and where they occurred, returns a map of docs and terms with their data
     * @param termsData - the mapping of terms and where they occurred
     * @return - mapping of docs and terms with their data
     */
    protected HashMap<String, List<TermInDoc>> getDocumentsTerms(HashMap<String, List<PostingTermData>> termsData, float termImportance){
        HashMap<String, List<TermInDoc>> toReturn = new HashMap<>();

        for (String term :
                termsData.keySet()){
            List<PostingTermData> termsList = termsData.get(term);

            for (PostingTermData data :
                    termsList){
                List<TermInDoc> documentTerms = toReturn.computeIfAbsent(data.get_doc(), k -> new ArrayList<>()); // getting the terms of the doc if exist
                documentTerms.add(new TermInDoc(term, data, termImportance));
            }
        }
        return toReturn;
    }

    /**
     * given an ordered set of docNums will return their posting data
     * @param docNums - ordered set of doc numbers
     * @return - set of the corresponding posting data

    protected Set<RetrievedDocument> docsMatchingCity(HashMap<String, List<TermInDoc>> docNums){
        Set<RetrievedDocument> matchingDoc = new LinkedHashSet<>();
        String path = _outputPath + _docsPosting;
        if (_stemming){
            path = path +_stem;
        }
        path = path+_txt;

        try (
                RandomAccessFile ra = new RandomAccessFile(path, "r")
                ){
            long pointer = 0;
            for (String docS :
                    docNums.keySet()){
                Integer doc = Integer.valueOf(docS);
                pointer = _documentDictionary.getPointer(doc);
                String officialName = _documentDictionary.getName(doc);
                if (pointer == -1){ // should never enter here
                    System.out.println("document have not been processed properly" + doc);
                    throw new IOException("bad pointer");
                }

                ra.seek(pointer);
                String data = ra.readLine();

                String[] docData = data.split(" ");
                int startLine = Integer.valueOf(docData[0]);
                int endLine = Integer.valueOf(docData[1]);
                String relative = docData[2];
                int unique = Integer.valueOf(docData[3]);
                int maxTF = Integer.valueOf(docData[4]);
                int length = Integer.valueOf(docData[5]);
                String city = docData[6];

                if (_cities.isEmpty() || _cities.contains(city)){
                    ArrayList<String> strongEntities = getEntities(docData);
                    RetrievedDocument retrievedDocument = new RetrievedDocument();
                    retrievedDocument.set_city(city);
                    retrievedDocument.set_docNum(String.valueOf(doc));
                    retrievedDocument.set_length(length);
                    retrievedDocument.set_startLine(startLine);
                    retrievedDocument.set_endLine(endLine);
                    retrievedDocument.set_file(relative);
                    retrievedDocument.set_docName(officialName);
                    retrievedDocument.set_strongEntities(strongEntities);

                    matchingDoc.add(retrievedDocument);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }

        return matchingDoc;
    }*/

    /**
     * method to extract strong entities
     * @param docData - split array of document posting
     * @return - list of strong entities
     */
    private ArrayList<String> getEntities(String[] docData) {
        ArrayList<String> toReturn = new ArrayList<>();
        String e1 = docData[7], e2=docData[8], e3=docData[9], e4=docData[10], e5=docData[11];
        toReturn.add(e1);
        toReturn.add(e2);
        toReturn.add(e3);
        toReturn.add(e4);
        toReturn.add(e5);
        return toReturn;
    }

    /**
     * method that adds rank BM25 rank to set of retrieved docs
     * @param allDocs - the entire docs with matching query terms
     * @param cityDocs - docs that match
     */
    protected void rankByBM25(HashMap<String, List<TermInDoc>> allDocs, Set<RetrievedDocument> cityDocs, HashMap<String, IntWrapper> query, double value){

        double k = 2;
        double b = 0.75;

        for (RetrievedDocument document:
                cityDocs){
            List<TermInDoc> docMatchingTerms = allDocs.get(document.get_docNum());

            if (docMatchingTerms==null) // should never enter here
                continue;

            double rank = 0;
            for (TermInDoc termInDoc:
                    docMatchingTerms){
                int termCount = termInDoc.get_data().get_termOccurrences();
                double normalizedDocLength = (document.get_length()/_avgDocLength);

                int wordQueryCount = 1; //termCountInQuery(query, termInDoc.get_termName());
                double numerator = (k+1)*termCount;
                double denominator = termCount + k*(1-b+b*normalizedDocLength);
                double normalizedDF = Math.log10((_totalDocNum+1)/_corpusDictionary.get(termInDoc.get_termName()).getM_df());


                double wordRank = (wordQueryCount*(numerator/denominator)*normalizedDF);
                wordRank = wordRank*termInDoc.get_multiplier();
                if (document.strongEntitiesContainIgnoreCases(termInDoc.get_termName())){
                    wordRank = wordRank*1.5;
                }

                rank += wordRank;
            }
            document.add_rank(value*rank);
        }
    }

    /**
     * method to rank documents by the terms position
     * @param allDocs - all docs retrieved
     * @param cityDocs - docs retrieved by city
     * @param query - the query
     * @param value - the value of this ranking
     */
    protected void rankByPosition(HashMap<String, List<TermInDoc>> allDocs,
                                  Set<RetrievedDocument> cityDocs, HashMap<String, IntWrapper> query, double value){

        for (RetrievedDocument document:
                cityDocs){
            List<TermInDoc> docMatchingTerms = allDocs.get(document.get_docNum());

            if (docMatchingTerms==null) // should never enter here
                continue;

            double rank = 0;
            for (TermInDoc termInDoc:
                    docMatchingTerms){
                PostingTermData t = termInDoc.get_data();
                byte first20 = t.get_locations()[0];
                byte last20 = t.get_locations()[1];

                rank += (first20 + last20)*5;
            }
            document.add_rank(value*rank);
        }
    }


    protected void cosSim(HashMap<String, List<TermInDoc>> allDocs,
                          Set<RetrievedDocument> cityDocs,
                          HashMap<String, IntWrapper> query,
                          double value){
        double totalDocNum = _totalDocNum; // total doc number for normalization

        double queryWeightedLength = 0;     // weighted length of query.
        for (IntWrapper s :
                query.values()){
            queryWeightedLength += Math.pow(s.get_value(),2) ;
        }
        queryWeightedLength = Math.sqrt(queryWeightedLength);


        for (RetrievedDocument document :
                cityDocs){
            List<TermInDoc> docMatchingTerms = allDocs.get(document.get_docNum());
            if (docMatchingTerms==null) // should never enter here
                continue;

            double rank = 0;
            double numerator = 0;
            double weightedLength = _documentDictionary.getDocData(Integer.valueOf(document.get_docNum())).get_weightedLength();
            double maxTf = document.get_maxTf();

            // ---- calc of numerator: w_q*w_d
            for (TermInDoc termInDoc :
                    docMatchingTerms){
                String termName = termInDoc.get_termName();
                double tf = termInDoc.get_data().get_termOccurrences()/maxTf;
                double idf = Math.log10(totalDocNum/_corpusDictionary.get(termName).getM_df());
                numerator += tf*idf*termCountInQuery(query, termName)*termInDoc.get_multiplier();
            }

            // calc denominator
            double denominator = Math.sqrt(weightedLength*queryWeightedLength);
            rank = numerator/denominator;

            document.add_rank(rank*value);
        }

    }


    /**
     * given an ordered set of docNums will return their posting data
     * @param docNums - ordered set of doc numbers
     * @return - set of the corresponding posting data
    */
    protected Set<RetrievedDocument> docsMatchingCity(HashMap<String, List<TermInDoc>> docNums){
        Set<RetrievedDocument> matchingDoc = new HashSet<>();

        for (String docS :
                docNums.keySet()){
            try {
                Integer doc = Integer.valueOf(docS);
                DocumentData dData = _documentDictionary.getDocData(doc);
                String officialName = dData.get_docName();
                PostingDocData dPos = dData.get_data();

                if (_cities.isEmpty() || _cities.contains(dPos.get_city())){
                    ArrayList<String> strongEntities = getEntities(dPos);
                    RetrievedDocument retrievedDocument = new RetrievedDocument();
                    retrievedDocument.set_city(dPos.get_city());
                    retrievedDocument.set_docNum(String.valueOf(doc));
                    retrievedDocument.set_length(dPos.get_length());
                    retrievedDocument.set_startLine(dPos.get_startLine());
                    retrievedDocument.set_endLine(dPos.get_endLine());
                    retrievedDocument.set_file(dPos.get_relativePath());
                    retrievedDocument.set_docName(officialName);
                    retrievedDocument.set_strongEntities(strongEntities);
                    retrievedDocument.set_maxTf(dPos.get_maxTF());

                    matchingDoc.add(retrievedDocument);
                }
            }
            catch (Exception e){
                e.printStackTrace();
                System.out.println(e.getMessage());
            }
        }

        return matchingDoc;
    }

    /**
     * method to get the strong entities of a document
     * @param postingDocData - the posting data
     * @return - list of string representing the strong entities of this document
     */
    private ArrayList<String> getEntities(PostingDocData postingDocData){
        String[] entities = postingDocData.get_entities();
        float[] ranks = postingDocData.get_ranks();

        ArrayList<String> toReturn = new ArrayList<>();
        for (int i=0; i<entities.length; i++){
            toReturn.add(entities[i] + "-" + ranks[i]);
        }

        return toReturn;
    }

    /**
     * method to get the count of a term in a given query
     * @param query - the given query
     * @param term - the observed term
     * @return - the count of term in a query, 0 if does not exist
     */
    private int termCountInQuery(HashMap<String, IntWrapper> query, String term) {
        IntWrapper result = null;
        if (query.containsKey(term)){
            result = query.get(term);
        }
        else {
            term = term.toLowerCase();
            if (query.containsKey(term)){
                result = query.get(term);
            }
            else {
                term = term.toUpperCase();
                if (query.containsKey(term)){
                    result = query.get(term);
                }
            }
        }

        return (result==null ? 0 : result.get_value());
    }

}
