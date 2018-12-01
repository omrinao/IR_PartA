package MVVM;

public class Model {

    public void execute(String[] details) {
        for (String s: details
             ) {
            System.out.println(s);
        }
    }

    public void reset(String[] details) {

        //delete dictionary, posting file, reset main memory
        System.out.println("check reset");

    }

    public void loadDict(String stemming) {

        //load dictionary from disk to memory, the parameter is with or without stemming
        System.out.println("check loaddict");

    }

    public void showDict(String stemming) {
        //displaying dictionary
        System.out.println("check showdict");
    }
}
