package Indexing;

import java.util.ArrayList;

public class PostingTermData implements Comparable {

    String _doc;
    int _termOccurrences;
    byte[] _locations;

    public PostingTermData(String _doc, ArrayList<Integer> locations, int size) {
        this._doc = _doc;
        _termOccurrences = locations.size();
        _locations = new byte[2];

        if (locations.get(0) <= size*0.2){
            _locations[0] = 1;
        }else {
            _locations[0] = 0;
        }

        if (locations.get(locations.size()-1) >= size*0.8){
            _locations[1] = 1;
        }else {
            _locations[1] = 0;
        }

    }

    public PostingTermData(String docNum, int occurrences, byte first20, byte last20){
        _doc = docNum;
        _termOccurrences = occurrences;
        _locations = new byte[2];
        _locations[0] = first20;
        _locations[1] = last20;
    }



    /**
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException   if the specified object's type prevents it
     *                              from being compared to this object.
     */
    @Override
    public int compareTo(Object o) {
        if (!(o instanceof PostingTermData))
            return -1; //?
        PostingTermData other = (PostingTermData) o;

        return _doc.compareTo(other._doc);
    }


    public String get_doc(){return _doc;}


    public void set_doc(String _doc) {
        this._doc = _doc;
    }

    public int get_termOccurrences() {
        return _termOccurrences;
    }

    public void set_termOccurrences(int _termOccurrences) {
        this._termOccurrences = _termOccurrences;
    }

    public byte[] get_locations() {
        return _locations;
    }

    public void set_locations(byte[] _locations) {
        this._locations = _locations;
    }
}