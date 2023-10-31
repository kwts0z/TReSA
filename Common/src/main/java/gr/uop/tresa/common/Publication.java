package gr.uop.tresa.common;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import java.util.UUID;


public class Publication {

    private String id;
    private String title;
    private String people;
    private String places;
    private String body;

    public Publication(String document){
        id = UUID.randomUUID().toString();
        title = parseElement("TITLE", document);
        people = parseElement("PEOPLE", document);
        places = parseElement("PLACES", document);
        body = parseElement("BODY", document);
    }

    public Publication(String title, String people, String places, String body){
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.people = people;
        this.places = places;
        this.body = body;
    }

    public Publication(String id, String title, String people, String places, String body){
        this.id = id;
        this.title = title;
        this.people = people;
        this.places = places;
        this.body = body;
    }

    private static String parseElement(String tag, String document){
        Document doc = Jsoup.parse(document, Parser.xmlParser());
        return doc
                .getElementsByTag(tag)
                .get(0)
                .text();
    }
    public String getId() { return id; }

    public String getTitle() {
        return title;
    }

    public String getPeople() {
        return people;
    }

    public String getPlaces() {
        return places;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString(){
        int end = 15;
        return "ID: " + id +  "\nTITLE: " + title +"\n"+"PEOPLE: " + people + "\n" + "PLACES: " + places + "\n" + "BODY: " + body.substring(0, body.length()<end? body.length() : end) + "...\n";
    }
}
