package Indexing;

public class PostingDocData {

    int _maxTF;
    int _uniqueTerms;
    short _startLine;
    short _endLine;
    String _relativePath;
    int _length;
    String _city;

    public PostingDocData(int _maxTF, int _uniqueTerms, short _startLine, short _endLine, String _relativePath, int length, String city) {
        this._maxTF = _maxTF;
        this._uniqueTerms = _uniqueTerms;

        this._startLine = _startLine;
        this._endLine = _endLine;
        this._relativePath = _relativePath;
        this._length = length;
        if (city.isEmpty())
            this._city = "!";
        else
            this._city = city;
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
        return String.format("%s %s %s %s %s %s %s", _startLine, _endLine, _relativePath, _uniqueTerms, _maxTF, _length, _city);
    }
}
