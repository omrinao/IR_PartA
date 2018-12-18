package MVVM;

import Indexing.TermData;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;

public class View implements Observer {

    private ViewModel vm;

    @FXML
    public TextField corpus;
    public TextField dictpost;
    public TextField tf_loadQueryFile;
    public CheckBox stemming;

    public javafx.scene.control.ChoiceBox _languageChoice;
    public ObservableList<String> _languagesList= FXCollections.observableArrayList();
    public ListView listView;


    public void setVm(ViewModel vm) {
        this.vm = vm;
    }

    /**
     * this method will allow the user to select a directory from the computer
     * @param actionEvent
     */
    public void corpusChoose(ActionEvent actionEvent) {

        try {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory =
                    directoryChooser.showDialog(Main.pStage);

            if(selectedDirectory == null){
                corpus.setText("");
            }else{
                corpus.setText(selectedDirectory.getAbsolutePath());
            }
        } catch (Exception e) {

        }
    }

    /**
     * this method will allow the user to select a directory from the computer
     * @param actionEvent
     */
    public void outputChoose(ActionEvent actionEvent) {

        try {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory =
                    directoryChooser.showDialog(Main.pStage);

            if(selectedDirectory == null){
                dictpost.setText("");
            }else{
                dictpost.setText(selectedDirectory.getAbsolutePath());
            }
        } catch (Exception e) {

        }
    }

    /**
     * this method will execute the user input
     * @param actionEvent
     */
    public void executeEvent(ActionEvent actionEvent) {

        try {
            String [] details = new String[3];
            details[0] = String.valueOf(stemming.isSelected());
            details[1] = corpus.getText();
            details[2] = dictpost.getText();
            if ( details[1] == null||details[1].isEmpty()
                     ||
                    details[0]==null || details[0].isEmpty()){
                popProblem("Please specify both:\nCorpus directory\nWriting path");
                return;
            }

            vm.execute(details);
            HashSet<String> languagesFound = vm.getLanguages();
            if (languagesFound!=null){
                TreeSet<String> sorted = new TreeSet<>(languagesFound);
                for (String s:
                        sorted){
                    if (!s.matches(".*\\d+.*")){
                        _languagesList.add(s);
                    }
                }
                _languageChoice.setItems(_languagesList);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * this method will send to the view model a request for resetting from the user
     * @param actionEvent
     */
    public void resetEvent(ActionEvent actionEvent) {

        try {
            String [] details = new String[2];
            details[0] = corpus.getText();
            details[1] = dictpost.getText();

            vm.reset(details);
            stemming.setSelected(false);

        } catch (Exception e) {

        }
    }

    /**
     * this method will send to the view model a request to load the dictionary
     * @param actionEvent
     */
    public void loadDictEvent(ActionEvent actionEvent) {

        try {
            String[] args = {String.valueOf(stemming.isSelected()), dictpost.getText()};
            vm.loadDict(args);
        } catch (Exception e) {

        }
    }

    /**
     * this method will send to the view model a request to view the dictionary
     * @param actionEvent
     */
    public void showDictEvent(ActionEvent actionEvent) {

        Stage stage=new Stage();
        HashMap<String, TermData> unsortedDict = vm.getTermDict(String.valueOf(stemming.isSelected()), dictpost.getText());
        if (unsortedDict == null){
            popProblem("Dictionary not found\nExecute the program\nor\nLoad existing dictionary");
            actionEvent.consume();
            return;
        }

        listView = new ListView();

        TreeMap<String, TermData> sorted = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });
        sorted.putAll(unsortedDict);
        for (String s : sorted.keySet()) {
            listView.getItems().add("Term:  " + s + "     Total TF:  "+ unsortedDict.get(s).getM_totalTF());
        }

        Scene scene=new Scene(new Group());
        stage.initModality(Modality.APPLICATION_MODAL); //Lock the window until it closes
        final VBox vBox=new VBox();
        vBox.setSpacing(5);
        vBox.setPadding(new Insets(10,0,0,10));
        vBox.setPrefWidth(350);
        vBox.getChildren().addAll(listView);
        vBox.setAlignment(Pos.CENTER);


        Group group=((Group) scene.getRoot());
        group.getChildren().addAll(vBox);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * This method is called whenever the observed object is changed. An
     * application calls an <tt>Observable</tt> object's
     * <code>notifyObservers</code> method to have all the object's
     * observers notified of the change.
     *
     * @param o   the observable object.
     * @param arg an argument passed to the <code>notifyObservers</code>
     */
    @Override
    public void update(java.util.Observable o, Object arg) {
        if (o == vm){
            if (arg instanceof String){
                String info = (String) arg;
                if (info.contains("Error")){
                    popProblem(info);
                }
                else{
                    popInfo(info);
                }
            }
        }
    }


    /**
     * generic method to pop problems
     * @param info - info that will be displayed
     */
    private void popInfo(String info) {
        Alert prob = new Alert(Alert.AlertType.INFORMATION);
        DialogPane dialogPane = prob.getDialogPane();
        //dialogPane.getStylesheets().add(getClass().getResource("ViewStyle.css").toExternalForm());
        //dialogPane.getStyleClass().add("myDialog");

        prob.setContentText(info);
        prob.showAndWait();
    }

    /**
     * a method to pop errors with a description
     *
     * @param description - of the error occured
     */
    private void popProblem(String description) {
        Alert prob = new Alert(Alert.AlertType.ERROR);
        DialogPane dialogPane = prob.getDialogPane();
        //dialogPane.getStylesheets().add(getClass().getResource("ViewStyle.css").toExternalForm());
        //dialogPane.getStyleClass().add("myDialog");

        prob.setContentText(description);
        prob.showAndWait();
    }

    /**
     * this method will allow the user to select a directory from the computer
     * @param actionEvent
     */
    public void queryChoose(ActionEvent actionEvent) {

        try {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory =
                    directoryChooser.showDialog(Main.pStage);

            if(selectedDirectory == null){
                tf_loadQueryFile.setText("");
            }else{
                tf_loadQueryFile.setText(selectedDirectory.getAbsolutePath());
            }
        } catch (Exception e) {

        }
    }


    public void cityChoose(ActionEvent actionEvent){

    }
}

