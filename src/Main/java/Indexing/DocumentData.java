package Indexing;

import java.io.Serializable;

public class DocumentData implements Serializable {

    long _pointer;
    String _docName;
    PostingDocData _data;

    public DocumentData(long _pointer, String _docName, PostingDocData data) {
        this._pointer = _pointer;
        this._docName = _docName;
        this._data = data;
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

    public PostingDocData get_data() {
        return _data;
    }

    public void set_data(PostingDocData _data) {
        this._data = _data;
    }
}
