import com.oracle.tools.packager.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Created by Ilia Sheiko on 16/05/2018.
 */
public class DocumentIdLoader {
    private Scanner scanner;
    private static final String sequencesRoot = "resources/sequences/";


    public void run(String rawUrl, String filePath){
        String inline = loadInlineJson(rawUrl);
        List<String> ids = parseInline(inline);
        writeToFile(filePath+".txt", ids);
        writeToFile(filePath+"First.txt", ids.subList(0,1));
        Log.info("Document ids' downloaded");
    }

    public void run(String rawProjectCollectionsUrl, String filePath, List<String> collectionCodes){
        LinkedList<String> inlines = collectionCodes.stream()
                .map(code -> loadInlineJson(rawProjectCollectionsUrl+code+"/documents"))
                .collect(Collectors.toCollection(LinkedList::new));
        LinkedList<String> ids = inlines.stream()
                .map(this::parseInline)
                .collect(LinkedList::new, List::addAll, List::addAll);
        writeToFile(filePath, ids);
    }


    private String loadInlineJson(String rawUrl){
        String inline = "";
        try {
            URL url = new URL(rawUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            int responseСode = connection.getResponseCode();
            if (responseСode != 200)
                throw new RuntimeException("HttpResponseCode" + responseСode);
            scanner = new Scanner(url.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }


        while (scanner.hasNext())
            inline += scanner.nextLine() + "\n";
        scanner.close();
        return inline;
    }

    private List<String> parseInline(String inline){
        LinkedList<String> ids = new LinkedList<>();
        String[] firstLvl = inline.split("\"_id\":\""); //each document id follows after that
        for (int i = 1; i < firstLvl.length; i++) {
            String[] secondLvl = firstLvl[i].split("\"", 2); //cut remaining
            ids.add(secondLvl[0]);
        }
        return ids;
    }

    private void writeToFile(String filePath, List<String> list){
        Path file = Paths.get(filePath);
        try {
            Files.write(file, list, Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public static void main(String[] args){
//        new DocumentIdLoader().run("http://localhost:8080/lumeer-engine/rest/organizations/DefCode/projects/DefProjectCode/collections/DefColCode/documents", sequencesRoot+"singleCollectionIds.txt");
//    }
}
