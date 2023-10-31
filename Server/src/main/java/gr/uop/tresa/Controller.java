package gr.uop.tresa;

import com.fasterxml.jackson.core.JsonProcessingException;
import gr.uop.tresa.common.Publication;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.pattern.PatternReplaceCharFilterFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import static gr.uop.tresa.constants.Constants.*;

@RestController
public class Controller {
    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    Directory luceneDir;

    @Value("${reuters-folder}")
    String reutersFolder;

    @Value("${fsdirectory}")
    String fsDirectory;

    @GetMapping("/debug")
    public void debug(){
        for (String x: CharFilterFactory.availableCharFilters()
             ) {
            logger.info(x);
        }
    }

    @GetMapping("/load-articles")
    public String loadArticles() throws IOException {
        try(IndexWriter writer = new IndexWriter(luceneDir, newConfig())){
            File dir = new File(reutersFolder);
            for (File f : dir.listFiles()) {
                Publication publication = new Publication(new String(Files.readAllBytes(f.toPath())));
                Document doc = toDoc(publication);
                writer.addDocument(doc);
            }
        }
        int count = 0;
        try(DirectoryReader reader = DirectoryReader.open(luceneDir)){
            IndexSearcher searcher = new IndexSearcher(reader);
            count = searcher.count(new MatchAllDocsQuery());
            logger.info("There are {} documents in directory", count);
        }
        return String.valueOf(count);
    }

    private IndexWriterConfig newConfig() throws IOException {
        Map<String, Analyzer> analyzerMap = new HashMap<>();
        analyzerMap.put(TITLE, new EnglishAnalyzer());
        analyzerMap.put(PEOPLE, new KeywordAnalyzer());
        analyzerMap.put(PLACES, new KeywordAnalyzer());
        analyzerMap.put(BODY, newCustomAnalyzer());
        PerFieldAnalyzerWrapper aw = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), analyzerMap);

        return new IndexWriterConfig(aw);
    }

    private static Analyzer newCustomAnalyzer() throws IOException {
        Map<String, String> map = new HashMap<>();
        map.put("pattern", "\\.|,");
        map.put("replacement", "");

        return CustomAnalyzer.builder()
                .addCharFilter(PatternReplaceCharFilterFactory.class, map)
                .withTokenizer("standard")
                .addTokenFilter("englishPossessive")
                .addTokenFilter("lowercase")
                .addTokenFilter("stop")
                .addTokenFilter("porterStem")
                .build();

    }

    private static Document toDoc(Publication publ){
        Document doc = new Document();
        doc.add(new StringField(ID, publ.getId(), Field.Store.YES));
        doc.add(new TextField(TITLE, publ.getTitle(), Field.Store.YES));
        doc.add(new TextField(PEOPLE, publ.getPeople(), Field.Store.YES));
        doc.add(new TextField(PLACES, publ.getPlaces(), Field.Store.YES));
        doc.add(new TextField(BODY, publ.getBody(), Field.Store.YES));
        return doc;
    }

    @PostMapping("/create-article")
    public void createArticle(@RequestBody String data) throws JsonProcessingException, IOException {
        JSONObject jo = new JSONObject(data);

        Publication publication = new Publication(jo.getString(TITLE), jo.getString(PEOPLE), jo.getString(PLACES), jo.getString(BODY));
        Document doc = toDoc(publication);
        try(IndexWriter writer = new IndexWriter(luceneDir, newConfig())){
            writer.addDocument(doc);
            System.out.println(publication);
         }
        try(DirectoryReader reader = DirectoryReader.open(luceneDir)){
            IndexSearcher searcher = new IndexSearcher(reader);
            logger.info("There are {} documents in directory", searcher.count(new MatchAllDocsQuery()));
        }
    }

    @GetMapping("/test")
    public void test(){
        Set<String> set = new HashSet<>();
        try(DirectoryReader reader = DirectoryReader.open(luceneDir)){
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs docs = searcher.search(new MatchAllDocsQuery(), 20000);
            for(ScoreDoc hit: docs.scoreDocs){
                set.add(searcher.doc(hit.doc).get(PEOPLE));
            }
            } catch (IOException ex) {
            ex.printStackTrace();
        }
        for (String x: set) {
            System.out.println(x);
        }
    }

    @GetMapping("/count")
    public void count(){
        try(DirectoryReader reader = DirectoryReader.open(luceneDir)){
            IndexSearcher searcher = new IndexSearcher(reader);
            logger.info("There are {} documents in directory", searcher.count(new MatchAllDocsQuery()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/clear-folder")
    public void clear(){
        File dir = new File(fsDirectory);
        for (File f : dir.listFiles()) {
            f.delete();
        }
    }

    @PostMapping("/delete")
    public void delete(@RequestBody String ids){
        JSONArray result = new JSONArray(ids);
        for(int i=0; i<result.length(); i++){
            System.out.println(result.getString(i));
            try(IndexWriter writer = new IndexWriter(luceneDir, newConfig())){
                writer.deleteDocuments(new Term(ID, result.getString(i)));
                System.out.println("Article deleted successfully!");
            }catch (IOException e){
                System.err.println(e.getMessage());
            }
        }
    }

    @PostMapping("/search")
    public String search(@RequestBody String q) throws QueryNodeException, IOException {
        JSONArray result = new JSONArray();
        StandardQueryParser queryParser = new StandardQueryParser();
        queryParser.setAllowLeadingWildcard(true);
        queryParser.setAnalyzer(newCustomAnalyzer());
        Query query = queryParser.parse(q, BODY);
        try(DirectoryReader reader = DirectoryReader.open(luceneDir)){
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs docs = searcher.search(query , 10);
            for(ScoreDoc hit: docs.scoreDocs){
                JSONObject jo = new JSONObject();
                jo.put(ID, searcher.doc(hit.doc).get(ID));
                jo.put(TITLE, searcher.doc(hit.doc).get(TITLE));
                jo.put(PEOPLE, searcher.doc(hit.doc).get(PEOPLE));
                jo.put(PLACES, searcher.doc(hit.doc).get(PLACES));
                jo.put(BODY, searcher.doc(hit.doc).get(BODY));
                jo.put("score",hit.score);
                result.put(jo);
                logger.info(String.valueOf(hit.score));
            }
        }catch (IOException e){
            System.err.println(e.getMessage());
        }
        logger.info(q);
        return result.toString();
    }


    @PostMapping("/edit")
    public void edit(@RequestBody String data) throws IOException {
        JSONObject jo = new JSONObject(data);

        Publication publication = new Publication(jo.getString(ID), jo.getString(TITLE), jo.getString(PEOPLE), jo.getString(PLACES), jo.getString(BODY));
        Document doc = toDoc(publication);
        try(IndexWriter writer = new IndexWriter(luceneDir, newConfig())){
            writer.updateDocument(new Term(ID, doc.get(ID)), doc);
            System.out.println("Article updated successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try(DirectoryReader reader = DirectoryReader.open(luceneDir)){
            IndexSearcher searcher = new IndexSearcher(reader);
            logger.info("There are {} documents in directory", searcher.count(new MatchAllDocsQuery()));
        }
    }

    @PostMapping("/similar")
    public String similar(@RequestBody String data){
        JSONArray result = new JSONArray();
        try(DirectoryReader reader = DirectoryReader.open(luceneDir)){
            IndexSearcher searcher = new IndexSearcher(reader);
            StandardQueryParser queryParser = new StandardQueryParser();
            TopDocs docs = searcher.search(queryParser.parse(data, ID), 1);

            int id = docs.scoreDocs[0].doc;
            MoreLikeThis mrl = new MoreLikeThis(reader);
            mrl.setFieldNames(new String[]{TITLE, BODY});
            mrl.setAnalyzer(new StandardAnalyzer());
            mrl.setMinTermFreq(1);

            Query query = mrl.like(id);
            logger.info(String.valueOf(query));
            TopDocs hits = searcher.search(query, 10);

            for(ScoreDoc hit: hits.scoreDocs){
                JSONObject jo = new JSONObject();
                jo.put(ID, searcher.doc(hit.doc).get(ID));
                jo.put(TITLE, searcher.doc(hit.doc).get(TITLE));
                jo.put(PEOPLE, searcher.doc(hit.doc).get(PEOPLE));
                jo.put(PLACES, searcher.doc(hit.doc).get(PLACES));
                jo.put(BODY, searcher.doc(hit.doc).get(BODY));
                jo.put("score",hit.score);
                result.put(jo);
                logger.info(String.valueOf(hit.score));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (QueryNodeException e) {
            e.printStackTrace();
        }
        return result.toString();
    }











}
