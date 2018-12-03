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
import java.util.Observable;
import java.util.concurrent.*;

public class Model extends Observable {

    private ThreadPoolExecutor _threads;
    private String _corpusPath;
    private String _writeTo;


    public Model(){
        this._threads = (ThreadPoolExecutor)Executors.newCachedThreadPool();
    }


    /**
     * a method that starts the entire process of indexing
     * @param details - details needed for execution
     */
    public void execute(String[] details) {
        _corpusPath = details[1];
        _writeTo = details[2];
        Thread t = new Thread(() -> startPartA(details[0], details[1], details[2]));
        t.start();

        setChanged();
        notifyObservers("Processing has began!");
    }


    /**
     * method to start the entire process of part A
     * @param stemming - if stemming is needed
     * @param corpusPath - path of the corpus
     * @param writingPath - path of where to write
     */
    private void startPartA(String stemming, String corpusPath, String writingPath){
        // --------- initing blocking queues ----------
        BlockingQueue<Document> beforeParse = new ArrayBlockingQueue<>(1000);
        BlockingQueue<Document> afterParse = new ArrayBlockingQueue<>(1000);

        // --------- initing working classes ----------
        ReadFile2 reader = new ReadFile2(corpusPath);
        Parser parser = new Parser();
        Indexer indexer = new Indexer(600, writingPath);

        // --------- setting Read File ----------
        reader.setQueue(beforeParse);

        // --------- setting Parser ------------
        if (Boolean.valueOf(stemming)){
            parser.setStemmer(true);
        }else{
            parser.setStemmer(false);
        }
        parser.setStopWords(reader.getStopWords());
        parser.setBeforeParse(beforeParse);
        parser.setAfterParse(afterParse);

        // --------- setting Indexer -----------
        indexer.setDocsQueue(afterParse);

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


        long end = System.nanoTime();
        long total = end-start;
        long milis = total/1000000;

        String time = "Done! \nTotal Time : " + milis/60000.00;
        System.out.println(String.format("%s \nNumber of indexed docs: %s\nNumber of different terms in the corpus: %s",
                time, indexer.getNumOfIndexed(), indexer.getNumOfTerms()));

        //setChanged();
        //notifyObservers(time);
    }


    /**
     * method to reset the program.
     * -> deleting all posting and dictionary files
     * @param details - the place of which the file were written
     */
    public void reset(String[] details) {
        String removeFrom = details[1];

        try {
            Files.delete(Paths.get(removeFrom + "\\FinalPosting.txt"));
        }catch (IOException e){
            setChanged();
            notifyObservers("Error deleting file: " + e.getMessage());
        }
        try {
            Files.delete(Paths.get(removeFrom + "\\TermsPosting"));
        }catch (IOException e){
            setChanged();
            notifyObservers("Error deleting file: " + e.getMessage());
        }
        try {
            Files.delete(Paths.get(removeFrom + "\\STEMFinalPosting.txt"));
        }catch (IOException e){
            setChanged();
            notifyObservers("Error deleting files: " + e.getMessage());
        }
        try {
            Files.delete(Paths.get(removeFrom + "\\STEMTermsPosting"));
        }catch (IOException e){
            setChanged();
            notifyObservers("Error deleting files: " + e.getMessage());
        }

        setChanged();
        notifyObservers("code d0");

    }


    /**
     * method to load the dictionary from the path
     * @param stemming - load args
     */
    public void loadDict(String stemming) {

        if (Boolean.valueOf(stemming)){
            try {
                ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(_writeTo + "\\TermsDictionary"));
                HashMap<String, TermData> dict = (HashMap<String, TermData>) inputStream.readObject();
                inputStream.close();

                setChanged();
                notifyObservers(dict);

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
                ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(_writeTo + "\\STEMTermsDictionary"));
                HashMap<String, TermData> dict = (HashMap<String, TermData>) inputStream.readObject();
                inputStream.close();

                setChanged();
                notifyObservers(dict);

            } catch (IOException e) {
                setChanged();
                notifyObservers("Error at opening file: " + e.getMessage());

            } catch (ClassNotFoundException f) {
                setChanged();
                notifyObservers("Error at loading dictionary: " + f.getMessage());
            }

        }

    }


    public void showDict(String stemming) {
        //displaying dictionary
        System.out.println("check showdict");
    }
}
