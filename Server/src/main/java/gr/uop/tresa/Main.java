package gr.uop.tresa;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.nio.file.Paths;

@SpringBootApplication
public class Main {

    @Value("${fsdirectory}")
    String fsdirectory;

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean
    public Directory getDirectory() throws IOException {
        return FSDirectory.open(Paths.get(fsdirectory));
    }

}