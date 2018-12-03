package Indexing;

public class PostingDocData {

    int _maxTF;
    int _uniqueTerms;
    //String _city;
    short _startLine;
    short _endLine;
    String _relativePath;

    public PostingDocData(int _maxTF, int _uniqueTerms, short _startLine, short _endLine, String _relativePath) {
        this._maxTF = _maxTF;
        this._uniqueTerms = _uniqueTerms;
       // this._city = _city;
        this._startLine = _startLine;
        this._endLine = _endLine;
        this._relativePath = _relativePath;
    }

    public int get_maxTF() {
        return _maxTF;
    }

    public int get_uniqueTerms() {
        return _uniqueTerms;
    }

   /* public String get_city() {
        return _city;
    }
*/
    public short get_startLine() {
        return _startLine;
    }

    public short get_endLine() {
        return _endLine;
    }

    public String get_relativePath() {
        return _relativePath;
    }

    @Override
    public String toString() {
        return String.format("%s %s %s %s %s", _startLine, _endLine, _relativePath, _uniqueTerms, _maxTF);
    }
}
