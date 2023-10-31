package gr.uop.tresa;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

import static gr.uop.tresa.constants.Constants.*;


public class Main extends Application {

    private HBox searchbarPane;
    private ListView searchResults;
    private ObservableList<String> options;
    private ComboBox cb1;
    private ComboBox cb2;
    private ComboBox cb3;
    private TextField advancedSearch;
    private CheckBox exactCB;
    static Label loadLabel;


    @Override
    public void start(Stage stage)  {

        //--------------Scene1----------------------
        //Create searchbar
        searchbarPane = new HBox();

        VBox searchPane = new VBox();
        Label mainLabel = new Label("TReSA");
        Button settingsBtn = new Button("Settings");
        options = FXCollections.observableArrayList(
                "AND",
                "OR"
        );

        advancedSearch = new TextField();
        advancedSearch.setPromptText("Advanced Search");
        Button advancedBtn = new Button("Advanced Search");

        exactCB = new CheckBox("Exact Match");

        cb1 = new ComboBox(options);
        cb1.getSelectionModel().selectFirst();
        cb2 = new ComboBox(options);
        cb2.getSelectionModel().selectFirst();
        cb3 = new ComboBox(options);
        cb3.getSelectionModel().selectFirst();

        searchbarPane.getChildren().addAll(
                newSearchField("Title"),
                cb1,
                newSearchField("People"),
                cb2,
                newSearchField("Places"),
                cb3,
                newSearchField("Body"),
                exactCB,
                settingsBtn,
                advancedSearch,
                advancedBtn
        );
        searchPane.getChildren().addAll(mainLabel, searchbarPane);

        //Create buttons
        VBox mainWindow = new VBox();
        HBox buttonPane= new HBox();
        Button deleteBtn = new Button("Delete");
        Button countBtn = new Button("Count");
        Button editBtn = new Button("Edit");
        Button tutorialBtn = new Button("Tutorial");
        Button findSimilarBtn = new Button("Find Similar article");

        //Create Listview
        searchResults = new ListView();
        searchResults.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        searchResults.setCellFactory(new Callback<ListView<JSONObject>, ListCell<JSONObject>>() {
            @Override
            public ListCell<JSONObject> call(ListView<JSONObject> listView) {
                return new ListCell<JSONObject>(){
                    @Override
                    protected void updateItem(JSONObject jo, boolean b) {
                        super.updateItem(jo, b);
                        if(jo!=null){
                            setText("TITLE:        " + jo.getString(TITLE) +
                                    "                                                                             SCORE:   " + jo.getDouble("score"));
                        }else{
                            setText("");
                        }
                    }
                };
            }
        });

        //finish the scene1
        buttonPane.getChildren().addAll(deleteBtn, countBtn, editBtn, tutorialBtn, findSimilarBtn);
        mainWindow.getChildren().addAll(searchPane, searchResults, buttonPane);

        Scene scene = new Scene(mainWindow, 1920, 1080);
        stage.setTitle("TReSA");
        stage.setScene(scene);
        stage.show();

        deleteBtn.setOnAction(e->{

            HttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost("http://localhost:8080/delete");

            List<JSONObject> selectedItems = searchResults.getSelectionModel().getSelectedItems();
            JSONArray ja = new JSONArray();
            for (JSONObject jo: selectedItems) {
                ja.put(jo.getString(ID));
            }
            httpPost.setEntity(new StringEntity(ja.toString(), ContentType.APPLICATION_JSON));
            HttpResponse response = null;
            try {
                response = httpClient.execute(httpPost);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            HttpEntity entity = response.getEntity();
            searchResults.getItems().removeAll(selectedItems);
        });

        countBtn.setOnAction(e->{
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet("http://localhost:8080/count");
            HttpResponse response = null;
            try {
                response = httpClient.execute(httpGet);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            HttpEntity entity = response.getEntity();

        });

        findSimilarBtn.setOnAction(e-> {
            JSONObject selectedItem = (JSONObject) searchResults.getSelectionModel().getSelectedItem();
            HttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost("http://localhost:8080/similar");

            httpPost.setEntity(new StringEntity(selectedItem.getString(ID), ContentType.DEFAULT_TEXT));
            HttpResponse response = null;
            try {
                response = httpClient.execute(httpPost);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            HttpEntity entity = response.getEntity();

            String json = null;
            try {
                json = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            JSONArray ja = new JSONArray(json);

            searchResults.getItems().clear();
            for (int i = 0; i < ja.length(); i++) {
                searchResults.getItems().add(ja.get(i));
            }


        });

        editBtn.setOnAction(e-> {
            JSONObject selectedItem = (JSONObject) searchResults.getSelectionModel().getSelectedItem();
            if(!searchResults.getSelectionModel().isEmpty()){
                displayEditWindow(stage, selectedItem);
            }
        });

        advancedBtn.setOnAction(e -> {
            searchResults.getItems().clear();
            HttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost("http://localhost:8080/search");
            httpPost.setEntity(new StringEntity(advancedSearch.getText(), ContentType.DEFAULT_TEXT));
            HttpResponse response = null;
            try {
                response = httpClient.execute(httpPost);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            HttpEntity entity = response.getEntity();
            String json = null;
            try {
                json = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            JSONArray ja = new JSONArray(json);

            for (int i = 0; i < ja.length(); i++) {
                searchResults.getItems().add(ja.get(i));
            }
        });

        tutorialBtn.setOnAction(e -> displayTutorialWindow(stage));

        settingsBtn.setOnAction(e-> displaySettingsWindow(stage));
   }

   //-----------------------------Methods-------------------------------------


    //----------------------Makes a Not checkbox and a textfield-------------------
   private HBox newSearchTerm(String name){
        HBox hbox = new HBox();
        TextField textField = new TextField();
        textField.setPromptText(name);
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
           if(!newValue.isEmpty()){

               searchResults.getItems().clear();
               HttpClient httpClient = HttpClients.createDefault();
               HttpPost httpPost = new HttpPost("http://localhost:8080/search");
               httpPost.setEntity(new StringEntity(buildQuery(), ContentType.DEFAULT_TEXT));
               HttpResponse response = null;
               try {
                   response = httpClient.execute(httpPost);
               } catch (IOException ex) {
                   ex.printStackTrace();
               }
               HttpEntity entity = response.getEntity();
               String json = null;
               try {
                   json = EntityUtils.toString(entity, StandardCharsets.UTF_8);
               } catch (IOException ex) {
                   ex.printStackTrace();
               }
               JSONArray ja = new JSONArray(json);

               for (int i = 0; i < ja.length(); i++) {
                   searchResults.getItems().add(ja.get(i));
               }
           }else{
               searchResults.getItems().clear();
           }
       });

        CheckBox chBox = new CheckBox("Not");
        hbox.getChildren().addAll(chBox, textField);

        return hbox;
   }

   //-----------------------puts 2 textfields and a combobox together-----------------
   private VBox newSearchField(String name){
        VBox vbox = new VBox();
        ObservableList<String> options = FXCollections.observableArrayList(
               "AND",
               "OR"
        );
        ComboBox cb = new ComboBox(options);
        vbox.getChildren().addAll(
                newSearchTerm(name),
                cb,
                newSearchTerm(name)
        );
        return vbox;
   }

   //------------------Builds the query that we send to the server ------------------------
   private String buildQuery(){

        VBox titleVbox = (VBox)(searchbarPane.getChildren().get(0));
        VBox peopleVbox = (VBox)(searchbarPane.getChildren().get(2));
        VBox placesVbox = (VBox)(searchbarPane.getChildren().get(4));
        VBox bodyVbox = (VBox)(searchbarPane.getChildren().get(6));

        HBox titleHBox1 = (HBox) titleVbox.getChildren().get(0);
        HBox titleHBox2 = (HBox) titleVbox.getChildren().get(2);
        HBox peopleHBox1 = (HBox) peopleVbox.getChildren().get(0);
        HBox peopleHBox2 = (HBox) peopleVbox.getChildren().get(2);
        HBox placesHBox1 = (HBox) placesVbox.getChildren().get(0);
        HBox placesHBox2 = (HBox) placesVbox.getChildren().get(2);
        HBox bodyHBox1 = (HBox) bodyVbox.getChildren().get(0);
        HBox bodyHBox2 = (HBox) bodyVbox.getChildren().get(2);

        StringBuilder titleQuery = new StringBuilder();
        StringBuilder peopleQuery = new StringBuilder();
        StringBuilder placesQuery = new StringBuilder();
        StringBuilder bodyQuery = new StringBuilder();
        StringBuilder query = new StringBuilder();

//--------------------------------making titleQuery---------------------------------------------------------
       String exactMatch = " ";
       if(exactCB.isSelected()) exactMatch = " \"";
       if(!(((TextField)(titleHBox1.getChildren().get(1))).getText().isEmpty())){
           titleQuery.append(" " + TITLE + ": (");
           if(((CheckBox)(titleHBox1.getChildren().get(0))).isSelected()){
               titleQuery.append(" NOT");
           }

           titleQuery.append(exactMatch + ((TextField)(titleHBox1.getChildren().get(1))).getText() + exactMatch);

           if(((ComboBox)(titleVbox.getChildren().get(1))).getValue() != null){
               titleQuery.append(" " + ((ComboBox)(titleVbox.getChildren().get(1))).getValue().toString());

               if(((CheckBox)(titleHBox2.getChildren().get(0))).isSelected()){
                   titleQuery.append(" NOT");
               }
               titleQuery.append(exactMatch + ((TextField)(titleHBox2.getChildren().get(1))).getText() + exactMatch);
           }
           titleQuery.append(")");
           query.append(titleQuery);
       }
//--------------------------------making peopleQuery------------------------------------------------------------
       if(!(((TextField)(peopleHBox1.getChildren().get(1))).getText().isEmpty())){
           if(!(((TextField)(titleHBox1.getChildren().get(1))).getText().isEmpty())){
               peopleQuery.append(" " + cb1.getValue().toString());
           }
           peopleQuery.append(" " + PEOPLE + ": (");
           if(((CheckBox)(peopleHBox1.getChildren().get(0))).isSelected()){
               peopleQuery.append(" NOT");
           }

           peopleQuery.append(exactMatch + ((TextField)(peopleHBox1.getChildren().get(1))).getText() + exactMatch);

           if(((ComboBox)(peopleVbox.getChildren().get(1))).getValue() != null){
               peopleQuery.append(" " + ((ComboBox)(peopleVbox.getChildren().get(1))).getValue().toString());

               if(((CheckBox)(peopleHBox2.getChildren().get(0))).isSelected()){
                   peopleQuery.append(" NOT");
               }
               peopleQuery.append(exactMatch + ((TextField)(peopleHBox2.getChildren().get(1))).getText() + exactMatch);
           }
           peopleQuery.append(")");
           query.append(peopleQuery);
       }
//--------------------------------making placesQuery--------------------------------------------------------------
       if(!(((TextField)(placesHBox1.getChildren().get(1))).getText().isEmpty())){
           if(!(((TextField)(titleHBox1.getChildren().get(1))).getText().isEmpty()) || !(((TextField)(peopleHBox1.getChildren().get(1))).getText().isEmpty())){
               placesQuery.append(" " + cb2.getValue().toString());
           }
           placesQuery.append(" " + PLACES + ": (");
           if(((CheckBox)(placesHBox1.getChildren().get(0))).isSelected()){
               placesQuery.append(" NOT");
           }

           placesQuery.append(exactMatch + ((TextField)(placesHBox1.getChildren().get(1))).getText() + exactMatch);

           if(((ComboBox)(placesVbox.getChildren().get(1))).getValue() != null){
               placesQuery.append(" " + ((ComboBox)(placesVbox.getChildren().get(1))).getValue().toString());

               if(((CheckBox)(placesHBox2.getChildren().get(0))).isSelected()){
                   placesQuery.append(" NOT");
               }
               placesQuery.append(exactMatch + ((TextField)(placesHBox2.getChildren().get(1))).getText() + exactMatch);
           }
           placesQuery.append(")");
           query.append(placesQuery);
       }

//--------------------------------making bodyQuery--------------------------------------------------------------
       if(!(((TextField)(bodyHBox1.getChildren().get(1))).getText().isEmpty())){
           if(!(((TextField)(titleHBox1.getChildren().get(1))).getText().isEmpty()) || !(((TextField)(peopleHBox1.getChildren().get(1))).getText().isEmpty()) || !(((TextField)(placesHBox1.getChildren().get(1))).getText().isEmpty())){
               bodyQuery.append(" " + cb3.getValue().toString());
           }
           bodyQuery.append(" " + BODY + ": (");
           if(((CheckBox)(bodyHBox1.getChildren().get(0))).isSelected()){
               bodyQuery.append(" NOT");
           }

           bodyQuery.append(exactMatch + ((TextField)(bodyHBox1.getChildren().get(1))).getText() + exactMatch);

           if(((ComboBox)(bodyVbox.getChildren().get(1))).getValue() != null){
               bodyQuery.append(" " + ((ComboBox)(bodyVbox.getChildren().get(1))).getValue().toString());

               if(((CheckBox)(bodyHBox2.getChildren().get(0))).isSelected()){
                   bodyQuery.append(" NOT");
               }
               bodyQuery.append(exactMatch + ((TextField)(bodyHBox2.getChildren().get(1))).getText() + exactMatch);
           }
           bodyQuery.append(")");
           query.append(bodyQuery);
       }

       return query.toString();
   }

    //-----------------------Creates the Tutorial Window---------------------------------
    private static void displayTutorialWindow(Stage primaryStage){
        //--------------Tutorial Stage--------------------
        Stage tutorialStage = new Stage();
        tutorialStage.initModality(Modality.APPLICATION_MODAL);
        tutorialStage.initOwner(primaryStage);
        tutorialStage.setResizable(true);
        tutorialStage.setTitle("Tutorial");

        VBox mainPane = new VBox();
        Label title = new Label("How to use TReSA");
        Label info = new Label("Each TextField tells you, which field is referred to.\n" +
                "There are 2 TextFields for every field in case you want to search for 2 different words or 2 different phrases\n" +
                "and you have the option to use \"AND\" and \"OR\". You have this options between fields too, for example you can have \"Phrase1 in title\" OR \"Phrase2 in body\".\n" +
                "Before each TextField you have the \"NOT\" option in case you don't want your search to have this phrase or word.\n" +
                "If want a exact match with the phrases you provided you can check the CheckBox labeled as \"Exact match\"\n" +
                "In case you want to do a more complex search you can use the advanced search where you can write the query by yourself.\n");
        Button closeBtn = new Button("Close");

        closeBtn.setOnAction(e -> tutorialStage.close());
        mainPane.getChildren().addAll(title, info, closeBtn);
        Scene tutorialScene= new Scene(mainPane, 700, 400);
        tutorialStage.setScene(tutorialScene);
        tutorialStage.show();
    }

   //-----------------------Creates the Settings Window---------------------------------
   private static void displaySettingsWindow(Stage primaryStage){

       //--------------Settings Stage--------------------
       Stage settingsStage = new Stage();
       settingsStage.initModality(Modality.APPLICATION_MODAL);
       settingsStage.initOwner(primaryStage);
       settingsStage.setResizable(true);
       settingsStage.setTitle("Settings");
       Button closeBtn= new Button("Close");
       Button loadBtn = new Button("Load");
       Button saveBtn = new Button("Save");
       Button clearfldBtn = new Button("Clear Folder");
       loadLabel = new Label();

       Label settingsLabel = new Label("Settings");

       //Create TextFields
       TextField tfTitle = new TextField();
       TextField tfPeople = new TextField();
       TextField tfPlaces = new TextField();
       TextArea taBody = new TextArea();
       tfTitle.setPromptText("Title");
       tfPeople.setPromptText("People");
       tfPlaces.setPromptText("Places");
       taBody.setPromptText("Body");

       closeBtn.setOnAction(e -> settingsStage.close());

       HBox mainSettingsPane = new HBox();
       VBox settingsBtnPane = new VBox();
       VBox contextPane = new VBox();
       contextPane.getChildren().addAll(settingsLabel, tfTitle, tfPeople, tfPlaces, taBody);
       settingsBtnPane.getChildren().addAll(loadBtn, saveBtn, clearfldBtn, closeBtn, loadLabel);
       settingsBtnPane.setAlignment(Pos.CENTER);
       mainSettingsPane.getChildren().addAll(settingsBtnPane, contextPane);
       Scene settingsScene= new Scene(mainSettingsPane, 700, 400);
       settingsStage.setScene(settingsScene);
       settingsStage.show();

       //-------------------Buttons Functionality---------------

       clearfldBtn.setOnAction(e->{
           HttpClient httpClient = HttpClients.createDefault();
           HttpGet httpGet = new HttpGet("http://localhost:8080/clear-folder");
           HttpResponse response = null;
           try {
               response = httpClient.execute(httpGet);
           } catch (IOException ex) {
               ex.printStackTrace();
           }
           HttpEntity entity = response.getEntity();
       });

       loadBtn.setOnAction(e->{
           new Thread(new Loader()).start();
           loadLabel.setText("Loading Articles...");
       });

       saveBtn.setOnAction(e->{
           HttpClient httpClient = HttpClients.createDefault();
           HttpPost httpPost = new HttpPost("http://localhost:8080/create-article");

           JSONObject jo = new JSONObject();
           jo.put(TITLE, tfTitle.getText());
           jo.put(PEOPLE, tfPeople.getText());
           jo.put(PLACES, tfPlaces.getText());
           jo.put(BODY, taBody.getText());

           httpPost.setEntity(new StringEntity(jo.toString(), ContentType.APPLICATION_JSON));
           HttpResponse response = null;
           try {
               response = httpClient.execute(httpPost);
           } catch (IOException ex) {
               ex.printStackTrace();
           }
           HttpEntity entity = response.getEntity();
       });
   }

    //-----------------------Creates the Edit Window---------------------------------
   private static void displayEditWindow(Stage primaryStage, JSONObject json){
       //------------Edit Stage----------------------
       Stage editStage = new Stage();
       editStage.initOwner(primaryStage);
       editStage.initModality(Modality.APPLICATION_MODAL);
       editStage.setResizable(true);
       editStage.setTitle("Edit");
       Button doneBtn= new Button("Done");
       Button closeeBtn= new Button("Close");
       TextField tfeTitle = new TextField();
       TextField tfePeople = new TextField();
       TextField tfePlaces = new TextField();
       TextArea taeBody = new TextArea();
       tfeTitle.setPromptText("Title");
       tfePeople.setPromptText("People");
       tfePlaces.setPromptText("Places");
       taeBody.setPromptText("Body");
       taeBody.setPrefRowCount(10);
       taeBody.setPrefColumnCount(100);
       taeBody.setWrapText(true);
       taeBody.appendText("/ndata");

       tfeTitle.setText(json.getString(TITLE));
       tfePeople.setText(json.getString(PEOPLE));
       tfePlaces.setText(json.getString(PLACES));
       taeBody.setText(json.getString(BODY));

       VBox editPane = new VBox();
       HBox editBtnPane = new HBox();
       editBtnPane.getChildren().addAll(closeeBtn, doneBtn);
       editPane.getChildren().addAll(tfeTitle, tfePeople, tfePlaces, taeBody, editBtnPane);

       closeeBtn.setOnAction(e -> editStage.close());
       editPane.setAlignment(Pos.CENTER);
       Scene editScene= new Scene(editPane, 700, 400);
       editStage.setScene(editScene);
       editStage.show();


       doneBtn.setOnAction(e->{
           HttpClient httpClient = HttpClients.createDefault();
           HttpPost httpPost = new HttpPost("http://localhost:8080/edit");

           json.put(TITLE, tfeTitle.getText());
           json.put(PEOPLE, tfePeople.getText());
           json.put(PLACES, tfePlaces.getText());
           json.put(BODY, taeBody.getText());

           httpPost.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));
           HttpResponse response = null;
           try {
               response = httpClient.execute(httpPost);
           } catch (IOException ex) {
               ex.printStackTrace();
           }
           HttpEntity entity = response.getEntity();
       });

   }

    public static void main(String[] args) throws IOException {
        launch();
    }
}