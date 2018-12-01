package Terms;

public class IntWrapper {

    private int _value;

    public IntWrapper(int _value) {
        this._value = _value;
    }

    public int get_value() {
        return _value;
    }

    public void set_value(int _value) {
        this._value = _value;
    }

    public void increase(int increse){
        _value = _value + increse;
    }

    public void increase(){
        _value = _value+1;
    }
}
