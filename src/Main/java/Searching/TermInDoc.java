package Searching;

import Indexing.PostingTermData;

public class TermInDoc {

    private String _termName;
    private PostingTermData _data;
    private float _multiplier;

    public TermInDoc(String _termName, PostingTermData _data, float _originalQuery) {
        this._termName = _termName;
        this._data = _data;
        this._multiplier = _originalQuery;
    }

    public String get_termName() {
        return _termName;
    }

    public void set_termName(String _termName) {
        this._termName = _termName;
    }

    public PostingTermData get_data() {
        return _data;
    }

    public void set_data(PostingTermData _data) {
        this._data = _data;
    }

    public float get_multiplier() {
        return _multiplier;
    }

    public void set_multiplier(float _originalQuery) {
        this._multiplier = _originalQuery;
    }
}
