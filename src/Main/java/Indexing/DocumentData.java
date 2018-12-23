package Indexing;

import java.io.Serializable;

public class DocumentData implements Serializable {

    long _pointer;
    String _docName;

    public DocumentData(long _pointer, String _docName) {
        this._pointer = _pointer;
        this._docName = _docName;
    }

    public long get_pointer() {
        return _pointer;
    }

    public void set_pointer(long _pointer) {
        this._pointer = _pointer;
    }

    public String get_docName() {
        return _docName;
    }

    public void set_docName(String _docName) {
        this._docName = _docName;
    }
}
