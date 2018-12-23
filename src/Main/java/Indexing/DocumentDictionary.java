package Indexing;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

public class DocumentDictionary implements Serializable {

    HashMap<Integer, DocumentData> _dictionary;
    long _nextPointer;
    double _avgDocLength;


    public DocumentDictionary() {
        _dictionary = new HashMap<>();
        _nextPointer = 0;
    }

    public void insertDoc(Integer id, DocumentData data){
        _dictionary.put(id, data);
    }

    public void insertDoc(Integer id, long pointer, String docName){
        DocumentData d = new DocumentData(pointer, docName);
        insertDoc(id, d);
    }

    public DocumentData getDocData(Integer id){
        return _dictionary.get(id);
    }

    public long getPointer(Integer id){
        return getDocData(id)._pointer;
    }

    public String getName(Integer id){
        return getDocData(id)._docName;
    }

    public boolean containsDoc(Integer id){
        return _dictionary.containsKey(id);
    }

    public long get_nextPointer() {
        return _nextPointer;
    }

    public void set_nextPointer(long _nextPointer) {
        this._nextPointer = _nextPointer;
    }

    public Set<Integer> getKeysSet(){
        return _dictionary.keySet();
    }

    public double get_avgDocLength() {
        return _avgDocLength;
    }

    public void set_avgDocLength(double __avgDocLength) {
        this._avgDocLength = __avgDocLength;
    }

    public int get_totalDocCount() {
        return _dictionary.size();
    }

}
