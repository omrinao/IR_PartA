package Indexing;

import FileReading.Document;

import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;

public class Indexer implements Runnable {

    private int m_docsPerPartialPosting;
    private int _partialPostingCount;
    private int m_docsIndexed;
    private volatile boolean m_parsingDone;
    private boolean _stemmer;

    private final String PARTIAL_POSTING = "PartialPosting";
    private final String TXT = ".txt";
    private final String DOC_POSTING = "DocumentPosting";
    private final String WORKING_DIRECTORY;
    private final String STEMMER = "STEM";

    private BlockingQueue<Document> m_docsQueue;            // queue of document given from the parser
    public HashMap<String, TermData> _corpusDictionary;         // the total corpus dictionary
    private HashMap<String, TreeSet<PostingTermData>> _partialPosting;  // the partial posting data for the current documents

    private CityIndex _cityIndex;                            // instance of the city API, contains all API relevant details
    private HashMap <String, CityDetails> _cityDictionary;

    private HashMap<Integer, PostingDocData> _docData;       // structure for docs posting data




    public Indexer(int docsPerPartialPosting, String writingLocation) {
        this.WORKING_DIRECTORY = writingLocation + "\\";
        this.m_docsPerPartialPosting = docsPerPartialPosting;
        this.m_docsIndexed = 0;
        this.m_parsingDone = false;
        this._partialPostingCount = 0;

        this._corpusDictionary = new HashMap<>();
        //this._upperCaseDictionary = new HashMap<>();

        this._partialPosting = new HashMap<>();
        //this._upperPartialPosting = new TreeMap<>();
        this._cityIndex = new CityIndex();
        this._cityDictionary = new HashMap<>();

        _docData = new HashMap<>();
    }


    /**
     * method to get documents and index them properly in the dictionary
     * and in partial posting structure for later disk write
     */
    public void indexDocuments(){
        int partialIndexed = 0;
        while (true){
            Document d = null;
            try {

                d = m_docsQueue.take();

                if (d.getFinal()){
                    break;
                }

                d.setLength(d.getTermsMap().size()); // setting length of the document for later use

                LinkedHashMap<String, ArrayList<Integer>> docTerms = d.getTermsMap();
                for (String term: docTerms.keySet()) {//inserting to dictionary + partial posting

                    if (term.equals(term.toLowerCase())){ // it's a lower case term or non alphabetic term
                        if (_corpusDictionary.containsKey(term)){
                            TermData t = _corpusDictionary.get(term);
                            t.m_df = t.m_df+1;                        // adding to document frequency
                            t.m_totalTF += docTerms.get(term).size();   // adding to total tf count
                        }
                        else if (_corpusDictionary.containsKey(term.toUpperCase())) { // upper case term now observed with lower case!
                            TermData toLowerTerm = _corpusDictionary.remove(term.toUpperCase());
                            toLowerTerm.m_df += 1;
                            toLowerTerm.m_totalTF += docTerms.get(term).size();

                            _corpusDictionary.put(term.toUpperCase(), toLowerTerm);
                        }
                        else {  // adding a new term data to the dictionary
                            TermData newTerm = new TermData(1, docTerms.get(term).size());
                            _corpusDictionary.put(term, newTerm);
                        }


                        if (_partialPosting.containsKey(term)){ // lower case exist this partial posting
                            TreeSet<PostingTermData> postingTermData = _partialPosting.get(term);  // adding to partial posting
                            postingTermData.add(new PostingTermData(d.getDocNum(), docTerms.get(term), d.getLength()));
                        }
                        else if (_partialPosting.containsKey(term.toUpperCase())){ // upper case exist this partial posting
                            TreeSet<PostingTermData> toLowerPosting = _partialPosting.remove(term.toUpperCase());
                            toLowerPosting.add(new PostingTermData(d.getDocNum(), docTerms.get(term), d.getLength()));
                            _partialPosting.put(term, toLowerPosting);
                        }
                        else { // upper nor lower exist this partial posting
                            PostingTermData newPosting = new PostingTermData(d.getDocNum(), docTerms.get(term), d.getLength());
                            TreeSet<PostingTermData> treeSet = new TreeSet<>();
                            treeSet.add(newPosting);
                            _partialPosting.put(term, treeSet);
                        }
                    }

                    // it's an upper case term
                    else{
                        if (_corpusDictionary.containsKey(term.toLowerCase())){ // it's an upper case but have seen a lower case before
                            TermData t = _corpusDictionary.get(term.toLowerCase());
                            t.m_df += 1;
                            t.m_totalTF += docTerms.get(term).size();
                        }
                        else if (_corpusDictionary.containsKey(term)){ // have seen only upper case
                            TermData t = _corpusDictionary.get(term);
                            t.m_df += 1;
                            t.m_totalTF += docTerms.get(term).size();
                        }
                        else { // never seen before
                            TermData newTerm = new TermData(1, docTerms.get(term).size());
                            _corpusDictionary.put(term, newTerm);
                        }

                        if (_partialPosting.containsKey(term.toLowerCase())){ // it's an upper, but have seen a lower case this partial posting
                            TreeSet<PostingTermData> postingTreeSet = _partialPosting.get(term.toLowerCase());
                            postingTreeSet.add(new PostingTermData(d.getDocNum(), docTerms.get(term), d.getLength()));
                        }
                        else if (_partialPosting.containsKey(term)){ // have seen upper this partial posting
                            TreeSet<PostingTermData> postingTree = _partialPosting.get(term);
                            postingTree.add(new PostingTermData(d.getDocNum(), docTerms.get(term), d.getLength()));
                        }
                        else { // upper case and does'nt exist
                            PostingTermData termData = new PostingTermData(d.getDocNum(), docTerms.get(term), d.getLength());
                            TreeSet<PostingTermData> treeSet = new TreeSet<>();
                            treeSet.add(termData);
                            _partialPosting.put(term, treeSet);
                        }
                    }


                } // for end
                // inserting to city dictionary
                if (!d.getCity().isEmpty()) {//checking if <F P=104> label is exist
                    HashMap<String, ArrayList<Integer>> docsLocations = new HashMap<>();
                    if (_corpusDictionary.containsKey(d.getCity().toUpperCase())){//check if the corpus contains the city
                        docsLocations.put(d.getDocNum(), d.getTermsMap().get(d.getCity().toUpperCase()));//get the docnum + locations
                    }
                    else{
                        docsLocations.put(d.getDocNum(), null);
                    }
                    if (_cityDictionary.get(d.getCity()) == null) {//city dictionary does not contains the city
                        if (_cityIndex.getDetails(d.getCity())[0] == null) {//its not a capital city
                            _cityDictionary.put(d.getCity(), new CityDetails(null, null, null, docsLocations));//inserting just the city name and the document number
                        } else {// its a capital city
                            String[] details = _cityIndex.getDetails(d.getCity());
                            _cityDictionary.put(d.getCity(), new CityDetails(details[0], details[2], details[1], docsLocations));
                        }

                    } else {//city dictionary contains the city, updating docnum + locations
                        _cityDictionary.get(d.getCity()).getM_docs().put(d.getDocNum(), docsLocations.get(d.getDocNum()));//adding another document
                    }
                }

                _docData.put(Integer.valueOf(d.getDocNum()),
                        new PostingDocData(d.getMaxTF(), docTerms.size(), d.get_startLine(), d.get_endLine(), d.get_path()));

                m_docsIndexed++; // needed for work report
                partialIndexed++;

                if (partialIndexed == m_docsPerPartialPosting){
                    writePartialPostings();
                    partialIndexed = 0;
                }

                //System.out.println("Indexed doc: " + d.getDocNum());
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        writePartialPostings(); // after finished all documents, empty the final partial posting

        System.out.println("FINISHED INDEXING at: " + java.time.LocalTime.now());
        IndexMerger merger = new IndexMerger(WORKING_DIRECTORY, _corpusDictionary);
        try {
            merger.mergePostings();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //TreeMap<String, TermData> sorted = new TreeMap<>(_corpusDictionary);

        writeDictionary();
        writeCityDictionary();
        System.out.println("Exiting Indexer");
    }

    /**
     * method to write the dictionary to disk
     */
    private void writeDictionary() {
        String path;
        if (_stemmer){
            path = WORKING_DIRECTORY + STEMMER + "TermsDictionary";
        }
        else{
            path = WORKING_DIRECTORY + "TermsDictionary";
        }

        try(ObjectOutputStream write = new ObjectOutputStream(new FileOutputStream(path));) {
            write.writeObject(_corpusDictionary);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeCityDictionary(){
        String path = WORKING_DIRECTORY + "CityDicionary";

        try(ObjectOutputStream write = new ObjectOutputStream(new FileOutputStream(path));) {
            write.writeObject(_cityDictionary);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * a method to write partial posting once needed
     * the method also truncate the existing partial posting objects
     */
    private void writePartialPostings() { /* this method get throws. need to handle */
        String partialName;

        if (_stemmer)
            partialName = WORKING_DIRECTORY + PARTIAL_POSTING  + _partialPostingCount + STEMMER + TXT;
        else
            partialName = WORKING_DIRECTORY + PARTIAL_POSTING  + _partialPostingCount + TXT;

        try {
            if (!_partialPosting.isEmpty())
                writePartialPosting(partialName);
            if (!_docData.isEmpty()){
                writeToDocPosting();
            }
        }
        catch (IOException e){
            /* do something here */
        }

        _partialPostingCount++;
        _partialPosting.clear(); // maybe clear
    }

    /**
     * method to write to doc posting
     * @throws IOException - if error occured duric writing
     */
    private void writeToDocPosting()  throws IOException{
        try(
                BufferedWriter bw = new BufferedWriter(new FileWriter(WORKING_DIRECTORY + DOC_POSTING + TXT))
                ){
            TreeMap<Integer, PostingDocData> sorted = new TreeMap<>(_docData);

            for (Integer docNum :
                    sorted.keySet()) {
                PostingDocData cur = sorted.get(docNum);
                bw.append(String.format("%s\n", cur.toString()));
            }
        }
    }


    /**
     * a method to write the lower case partial posting
     * @param partialName - the name of the partial posting
     * @throws IOException - if an error occurred during file execution
     */
    private void writePartialPosting(String partialName) throws IOException {
        try(
                BufferedWriter lowerWriter = new BufferedWriter(new PrintWriter(partialName))
        ){
            // init an ordered map with comperator for strings ignoring upper/lower cases
            TreeMap<String, TreeSet<PostingTermData>> orderedPartialPosting = new TreeMap<>(new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    return o1.compareToIgnoreCase(o2);
                }
            });
            orderedPartialPosting.putAll(_partialPosting);

            for (String termEntry : orderedPartialPosting.keySet()){
                String toWrite = createPostingTerm(termEntry, _partialPosting.get(termEntry));

                lowerWriter.append(toWrite);
            }

            lowerWriter.flush();
        }
        catch (IOException e){
            throw new IOException("lower partial posting error.", e);
        }
    }

    /**
     * a method to create the string of a given term based on his posting data set
     * @param termEntry - the term to be written
     * @param treeSet - the posting data of the set
     * @return string representing the posting of the given term
     */
    private String createPostingTerm(String termEntry, TreeSet<PostingTermData> treeSet) {
        StringBuilder toReturn = new StringBuilder(termEntry + ":");

        for (PostingTermData termData : treeSet){
            toReturn.append(termData._doc).append(" "); // writing doc num
            toReturn.append(termData._termOccurrences).append(" "); // writing doc TF
            toReturn.append(termData._locations[0]).append(" "); // if occurred in first 20%
            toReturn.append(termData._locations[1]); // if occurred in last 20%
            toReturn.append(",");
        }
        toReturn.append("\n");
        return toReturn.toString();
    }


    public void setDocsQueue(BlockingQueue<Document> docsQueue) {
        m_docsQueue = docsQueue;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        indexDocuments();
    }

    public void setParserDone(boolean done){
        System.out.println("Indexer: received - Parsing is DONE");
        this.m_parsingDone = done;
    }

    public boolean getParserDone(){
        return m_parsingDone;
    }

    public void setStemmer(boolean stem){
        this._stemmer = stem;
    }

    public int getNumOfIndexed(){return m_docsIndexed;}

    public int getNumOfTerms(){return _corpusDictionary.size();}
}