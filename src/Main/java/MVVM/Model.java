package MVVM;

import FileReading.Document;
import FileReading.ReadFile2;
import Indexing.CityDetails;
import Indexing.DocumentDictionary;
import Indexing.Indexer;
import Indexing.TermData;
import Parse.Parser;
import Searching.IRanker;
import Searching.RankerNoSemantics;
import Searching.RetrievedDocument;
import Searching.Searcher;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Model extends Observable {

    private String _corpusPath;
    private String _writeTo;
    private HashMap<String, TermData> _loadedDict;
    private DocumentDictionary _loadedDocDict;
    private HashSet<String> _languagesFound;

    private int _totalDocCount;
    private double _avgDocLength;


    public Model(){

    }


    /**
     * a method that starts the entire process of indexing
     * @param details - details needed for execution
     */
    public void execute(String[] details) {
        _corpusPath = details[1] + "\\";
        _writeTo = details[2] + "\\";
        startPartA(details[0]);

    }


    /**
     * method to start the entire process of part A
     * @param stemming - if stemming is needed
     */
    private void startPartA(String stemming){
        boolean stem = Boolean.valueOf(stemming);

        // --------- initing blocking queues ----------
        BlockingQueue<Document> beforeParse = new ArrayBlockingQueue<>(1000);
        BlockingQueue<Document> afterParse = new ArrayBlockingQueue<>(1000);

        // --------- initing working classes ----------
        ReadFile2 reader = new ReadFile2(_corpusPath);
        Parser parser = new Parser();
        Indexer indexer = new Indexer(4000, _writeTo);

        // --------- setting Read File ----------
        reader.setQueue(beforeParse);

        // --------- setting Parser ------------
        parser.setStemmer(stem);
        parser.setStopWords(reader.getStopWords());
        parser.setBeforeParse(beforeParse);
        parser.setAfterParse(afterParse);

        // --------- setting Indexer -----------
        indexer.setDocsQueue(afterParse);
        indexer.setStemmer(stem);

        // ----------- initing threads ----------
        long start = System.nanoTime();

        Thread tReader = new Thread(reader);
        tReader.start();
        Thread tParser = new Thread(parser);
        tParser.start();
        Thread tIndexer = new Thread(indexer);
        tIndexer.start();

        HashMap<Path, Exception> m = null;
        try {
            tIndexer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        _languagesFound = indexer.get_docLanguages();
        _loadedDict = indexer._corpusDictionary;
        _loadedDocDict = indexer.get_docDictionary();
        _totalDocCount = indexer.getNumOfIndexed();
        _avgDocLength = indexer.getAvgDocLength();

        long end = System.nanoTime();
        long total = end-start;
        long milis = total/1000000;

        String time = "Done! \nTotal Time : " + milis/1000.00 + " Seconds";
        String results = String.format("%s \nNumber of indexed docs: %s\nNumber of different terms in the corpus: %s",
                time, indexer.getNumOfIndexed(), indexer.getNumOfTerms());

        setChanged();
        notifyObservers(results);
    }


    /**
     * method to reset the program.
     * -> deleting all posting and dictionary files
     * @param details - the place of which the file were written
     */
    public void reset(String[] details) {
        String removeFrom = null;
        if (_writeTo != null || !_writeTo.isEmpty()){
            removeFrom = _writeTo;
        }else if (!details[1].isEmpty()) {
            removeFrom = details[1] + "\\";
        }else {
            setChanged();
            notifyObservers("Please specify an exact folder to reset files.");
            return;
        }

        try (Stream<Path> paths = Files.walk(Paths.get(removeFrom), 1)){
            paths.filter(Files::isRegularFile).forEach(new Consumer<Path>() {
                @Override
                public void accept(Path path) {

                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });

            setChanged();
            notifyObservers("Reseting folder success!");
        }catch (IOException e){
            e.printStackTrace();
            setChanged();
            notifyObservers("Error while reseting the folder: " + e.getMessage());
        }

        finally {
            System.gc();
        }
    }


    /**
     * method to load the dictionary from the path
     * @param details - load args
     */
    public void loadDict(String[] details) {
        String stemming = details[0];
        String loadFrom = null;

        if (_writeTo != null && !_writeTo.isEmpty()){
            loadFrom = _writeTo;
        }
        else if (details[1] != null && !details[1].isEmpty()){
            loadFrom = details[1] + "\\";
        }
        else {
            setChanged();
            notifyObservers("Error!\n" + "Please specify a directory of a proper dictionary" );
            return;
        }

        if (Boolean.valueOf(stemming)){
            try {
                ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(loadFrom + "STEMTermsDictionary"));
                _loadedDict = (HashMap<String, TermData>) inputStream.readObject();
                inputStream.close();

                inputStream = new ObjectInputStream(new FileInputStream(loadFrom + "STEMDocDictionary"));
                _loadedDocDict = (DocumentDictionary) inputStream.readObject();
                inputStream.close();

                setChanged();
                notifyObservers("Dictionary with stemming loaded!");

            }catch (IOException e ){
                setChanged();
                notifyObservers("Error at openening file: " + e.getMessage());

            }catch (ClassNotFoundException f){
                setChanged();
                notifyObservers("Error at loading dicitionary: " + f.getMessage());
            }
        }

        else {
            try {
                ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(loadFrom + "TermsDictionary"));
                _loadedDict = (HashMap<String, TermData>) inputStream.readObject();
                inputStream.close();

                inputStream = new ObjectInputStream(new FileInputStream(loadFrom + "DocDictionary"));
                _loadedDocDict = (DocumentDictionary) inputStream.readObject();
                inputStream.close();

                setChanged();
                notifyObservers("Dictionary without stemming loaded!");

            } catch (IOException e) {
                setChanged();
                notifyObservers("Error at opening file: " + e.getMessage());

            } catch (ClassNotFoundException f) {
                setChanged();
                notifyObservers("Error at loading dictionary: " + f.getMessage());
            }

        }

    }

    /**
     * method to show the content of the dictionary
     * @param stemming - weather dictionary with stemming is requested or not
     */
    public void showDict(String stemming) {
        //displaying dictionary
        System.out.println("check showdict");
    }


    /**
     * @return - languages found
     */
    public HashSet<String> getLanguages() {
        return _languagesFound;
    }

    /**
     * method to retrieve terms dictionary
     * @param stem - weather stemming is asked
     * @param path - the path of the dic
     * @return dictionary
     */
    public HashMap<String, TermData> getTermDict(String stem, String path) {
        if (_loadedDict == null){
            String stemming = Boolean.valueOf(stem) ? "STEM" : "";
            try {
                ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(path + "\\" + stemming + "TermsDictionary"));
                _loadedDict = (HashMap<String, TermData>) inputStream.readObject();
                inputStream.close();
            }catch (IOException | ClassNotFoundException e){

            }
        }

        return _loadedDict;
    }

    /**
     * method to process a single given query
     * @param query - the query
     * @param cities - list of cities restrictions for the query
     * @param stemming - weather stemming is requested or not
     * @return - priority queue of relevant documents
     */
    public PriorityQueue<RetrievedDocument> proccessQuery(String query, List<String> cities, boolean stemming){

        IRanker r = new RankerNoSemantics(_loadedDict, cities, _loadedDocDict, stemming, _writeTo);
        Searcher s = new Searcher(r, ReadFile2.getStopWords(_corpusPath), stemming);

        PriorityQueue<RetrievedDocument> top50 = s.getRelevantDocuments(query, cities);

        return top50;
    }

    /**
     * method to retrieve the cities
     * @param outputDirectory - output directory given
     * @param stemming - weather stemming is required
     * @return ordered set of cities if success, null otherwise
     */
    public TreeSet<String> getCities(String outputDirectory, boolean stemming){
        if (_writeTo != null && !_writeTo.isEmpty()){
            outputDirectory = _writeTo;
        }
        else if (outputDirectory != null && !outputDirectory.isEmpty()){
            _writeTo = outputDirectory + "\\";
        }
        else {
            setChanged();
            notifyObservers("Error!\n" + "Please specify a directory of a proper dictionary" );
        }

        String stem = "";
        if (stemming){
            stem = "STEM";
        }

        try(
                ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(stem + "CityDictionary"))
                ){
            HashMap <String, CityDetails> cities = (HashMap <String, CityDetails>) inputStream.readObject();
            return new TreeSet<String>(cities.keySet());
        }
        catch (IOException e ){
            setChanged();
            notifyObservers("Error at openening file: " + e.getMessage());

        }catch (ClassNotFoundException f){
            setChanged();
            notifyObservers("Error at loading dicitionary: " + f.getMessage());
        }

        return null;
    }


    public static void main(String[] args){
        Model m = new Model();

        m._writeTo = "C:\\Users\\חגי קלינהוף\\Desktop\\Engine Output\\Doc Test";
        m._corpusPath = "C:\\Users\\חגי קלינהוף\\Desktop\\שנה ג'\\סמסטר ה'\\אחזור מידע\\פרויקט מנוע\\Part 1 tests\\corpus";
        String[] details = {"false", m._corpusPath, m._writeTo};
        m.loadDict(details);


        String query = "polytechnic Churchill trailblazer";
        PriorityQueue<RetrievedDocument> retrievedDocuments = m.proccessQuery(query, null, false);
        while (!retrievedDocuments.isEmpty()){
            System.out.println(retrievedDocuments.poll());
        }
/*
        try (
                BufferedWriter bw = new BufferedWriter(new PrintWriter("C:\\Users\\חגי קלינהוף\\Desktop\\Engine Output\\Doc Test\\doctest.txt"));
                RandomAccessFile ra = new RandomAccessFile("C:\\Users\\חגי קלינהוף\\Desktop\\Engine Output\\Doc Test\\DocumentPosting.txt", "r");
                ){

            for (Integer id :
                    m._loadedDocDict.getKeysSet()){
                long pointer = m._loadedDocDict.getPointer(id);
                ra.seek(pointer);

                String capturedLine = ra.readLine();
                bw.write(capturedLine+"\n");
            }
        }
        catch (IOException e){
            e.printStackTrace();
            System.out.println(e.getMessage());
        }*/
    }
}
