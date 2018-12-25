package Indexing;

import java.util.*;

public class PostingDocData {

    int _maxTF;
    int _uniqueTerms;
    short _startLine;
    short _endLine;
    String _relativePath;
    int _length;
    String _city;
    String[] _entities;
    float[] _ranks;

    public PostingDocData(int _maxTF, int _uniqueTerms, short _startLine, short _endLine,
                          String _relativePath, int length, String city, PriorityQueue<StrongEntity> strongEntities) {
        this._maxTF = _maxTF;
        this._uniqueTerms = _uniqueTerms;
        _entities = new String[5];
        _ranks = new float[5];
        setStrongEntities(strongEntities);

        this._startLine = _startLine;
        this._endLine = _endLine;
        this._relativePath = _relativePath;
        this._length = length;
        if (city.isEmpty())
            this._city = "!";
        else
            this._city = city;
    }

    /**
     * method to set strong entities
     * @param strongEntities - the mapping of the entities
     */
    private void setStrongEntities(PriorityQueue<StrongEntity> strongEntities) {
        for (int i =0; i<_entities.length; i++){
            if (!strongEntities.isEmpty()){
                StrongEntity top = strongEntities.remove();
                _entities[i] = top.get_term();
                _ranks[i] = top.get_rank();
            }
            else{
                _entities[i] = "?";
                _ranks[i] = 0;
            }
        }
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
        return String.format("%s %s %s %s %s %s %s %s", _startLine, _endLine, _relativePath, _uniqueTerms, _maxTF, _length, _city, stringEntities());
    }

    private String stringEntities() {
        StringBuilder toReturn = new StringBuilder();
        for (int i=0; i<_entities.length; i++){
            toReturn.append(_entities[i]).append("-").append(String.format("%.1f",_ranks[i])).append(" ");
        }
        String s = toReturn.toString();
        return s.trim();
    }
}
