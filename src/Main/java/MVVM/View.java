package MVVM;

import Indexing.TermData;
import javafx.application.Application;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.HashMap;
import java.util.Observer;

public class View implements Observer {

    private ViewModel vm;

    @FXML
    public TextField corpus;
    public TextField dictpost;
    public CheckBox stemming;


    public void setVm(ViewModel vm) {
        this.vm = vm;
    }

    /**
     * this method will allow the user to select a directory from the computer
     * @param actionEvent
     */
    public void directoryBrowseEvent(ActionEvent actionEvent) {

        try {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory =
                    directoryChooser.showDialog(Main.pStage);

            if(selectedDirectory == null){
                corpus.setText("No Directory selected");
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
    public void dictionaryBrowseEvent(ActionEvent actionEvent) {

        try {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory =
                    directoryChooser.showDialog(Main.pStage);

            if(selectedDirectory == null){
                dictpost.setText("No Directory selected");
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
            vm.execute(details);
        } catch (Exception e) {

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
            corpus.setText("");
            dictpost.setText("");
        } catch (Exception e) {

        }
    }

    /**
     * this method will send to the view model a request to load the dictionary
     * @param actionEvent
     */
    public void loadDictEvent(ActionEvent actionEvent) {

        try {
            vm.loadDict(String.valueOf(stemming.isSelected()));
        } catch (Exception e) {

        }
    }

    /**
     * this method will send to the view model a request to view the dictionary
     * @param actionEvent
     */
    public void showDictEvent(ActionEvent actionEvent) {

        try {
            Stage stage = new Stage();
            stage.setTitle("Dictionary");
            FXMLLoader fxmlLoader = new FXMLLoader();
            Parent root = fxmlLoader.load(getClass().getResource("../showDict.fxml").openStream());
            Scene scene = new Scene(root, 800, 550);
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL); //Lock the window until it closes
            stage.setResizable(false);
            vm.showDict(String.valueOf(stemming.isSelected()));
            stage.show();
        } catch (Exception e) {

        }
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
            if (arg instanceof HashMap){
                HashMap<String, TermData> dictionary = (HashMap<String, TermData>) arg;

                displayDictionary(dictionary);
            }
            else if (arg instanceof String){
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

    private void popInfo(String info) {
        Alert prob = new Alert(Alert.AlertType.INFORMATION);
        DialogPane dialogPane = prob.getDialogPane();
        //dialogPane.getStylesheets().add(getClass().getResource("ViewStyle.css").toExternalForm());
        //dialogPane.getStyleClass().add("myDialog");

        prob.setContentText(info);
        prob.showAndWait();
    }

    private void displayDictionary(HashMap<String, TermData> dictionary) {
        // TODO
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
}

