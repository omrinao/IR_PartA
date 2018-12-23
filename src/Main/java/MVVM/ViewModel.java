package MVVM;

import Indexing.TermData;
import Searching.RetrievedDocument;

import java.util.*;

public class ViewModel extends Observable implements Observer {

    private Model _model;

    public void setM(Model m) {
        this._model = m;
    }

    public void execute(String [] details) {
        _model.execute(details);
    }

    public void reset(String[] details) {
        _model.reset(details);
    }

    public void loadDict(String[] args) {
        _model.loadDict(args);
    }

    public void showDict(String stemming) {
        _model.showDict(stemming);
    }

    /**
     * This method is called whenever the observed object is changed. An
     * application calls an <tt>Observable</tt> object's
     * <code>notifyObservers</code> method to have all the object's
     * observers notified of the change.
     *
     * @param o   the observable object.
     * @param arg an argument passed to the <code>notifyObservers</code>
     */
    @Override
    public void update(Observable o, Object arg) {
        if (o == _model){
            setChanged();
            notifyObservers(arg);
        }
    }

    public HashSet<String> getLanguages() {
        return _model.getLanguages();
    }

    public HashMap<String, TermData> getTermDict(String stem, String path) {
        return _model.getTermDict(stem, path);
    }

    public TreeSet<String> getCities(String outputDirectory, boolean stemming){
        return _model.getCities(outputDirectory, stemming);
    }

    public PriorityQueue<RetrievedDocument> processQuery(String query, List<String> cities, boolean stemming, String corpus){
        return _model.processQuery(query, cities, stemming, corpus);
    }
}
