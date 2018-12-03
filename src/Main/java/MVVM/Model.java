package MVVM;

import FileReading.Document;
import FileReading.ReadFile;
import FileReading.ReadFile2;
import Indexing.Indexer;
import Indexing.TermData;
import Parse.Parser;
import sun.nio.ch.ThreadPool;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Observable;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Model extends Observable {

    private ThreadPoolExecutor _threads;
    private String _corpusPath;
    private String _writeTo;
    private HashMap<String, TermData> _loadedDict;
    private HashSet<String> _languagesFound;


    public Model(){
        this._threads = (ThreadPoolExecutor)Executors.newCachedThreadPool();
    }


    /**
     * a method that starts the entire process of indexing
     * @param details - details needed for execution
     */
    public void execute(String[] details) {
        _corpusPath = details[1] + "\\";
        _writeTo = details[2] + "\\";
        startPartA(details[0], details[1], details[2]);

    }


    /**
     * method to start the entire process of part A
     * @param stemming - if stemming is needed
     * @param corpusPath - path of the corpus
     * @param writingPath - path of where to write
     */
    private void startPartA(String stemming, String corpusPath, String writingPath){
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

        Future<HashMap<Path, Exception>> problems = _threads.submit(reader);
        Thread tParser = new Thread(parser);
        tParser.start();
        Thread tIndexer = new Thread(indexer);
        tIndexer.start();

        HashMap<Path, Exception> m = null;
        try {
            m = problems.get();
            tIndexer.join();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        _languagesFound = indexer.get_docLanguages();

        long end = System.nanoTime();
        long total = end-start;
        long milis = total/1000000;

        String time = "Done! \nTotal Time : " + milis/1000.00 + " Seconds" +
                "" +
                "";
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
        if (_writeTo == null || _writeTo.isEmpty()){
            removeFrom = details[1];
        }else if (!details[1].isEmpty()) {
            removeFrom = _writeTo;
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
     * @param stemming - load args
     */
    public void loadDict(String stemming) {

        if (Boolean.valueOf(stemming)){
            try {
                ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(_writeTo + "STEMTermsDictionary"));
                HashMap<String, TermData> dict = (HashMap<String, TermData>) inputStream.readObject();
                _loadedDict = dict;
                inputStream.close();

                setChanged();
                notifyObservers("Dictionary without stemming loaded!");

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
                ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(_writeTo + "TermsDictionary"));
                HashMap<String, TermData> dict = (HashMap<String, TermData>) inputStream.readObject();
                inputStream.close();

                setChanged();
                notifyObservers("Dictionary with stemming loaded!");

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

    public HashSet<String> getLanguages() {
        return _languagesFound;
    }
}
