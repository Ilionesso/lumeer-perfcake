/**
 * Created by Ilia Sheiko on 24/04/2018.
 */


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.perfcake.PerfCakeException;
import org.perfcake.ScenarioExecution;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class LumeerPerfRunner {

    private String configPath;
    private JSONObject config;
    private JSONObject common;

    private String chartsDir;
    private String serverHost;
    private String threadsCount;
    private String scenariosRootDir;

    private String organizationsDir;
    private String projectsDir;
    private String collectionsDir;
    private String documentsDir;


    private boolean doOrganizationTests;
    private boolean doProjectsTests;
    private boolean doCollectionsTests;
    private boolean doDocumentsTests;

    private boolean doProjectsOrganizationsTests;
    private boolean doCollectionsProjectsTests;
    private boolean doDocumentsCollectionsTests;

    public static void main(String[] args){
        if (args.length == 1)
            new LumeerPerfRunner(args[0]).run();
        else
            new LumeerPerfRunner("config.json").run();
    }

    private LumeerPerfRunner(String configPath) {
        this.configPath = configPath;
    }

    private void run(){
        initConfig();
        runTests();
    }

    private void initConfig(){
        try {
            JSONParser parser = new JSONParser();
            config = (JSONObject) parser.parse(new FileReader(configPath));
        } catch (ParseException | IOException e) {
            e.printStackTrace();
            return;
        }
        common = (JSONObject) config.get("common");
        chartsDir = (String) common.get("chartsDir");
        serverHost = (String) common.get("server.host");
        threadsCount = (String) common.get("threads.count");
        scenariosRootDir = (String) common.get("scenarios.rootDir");

        doOrganizationTests = (boolean) common.get("doOrganizationTests");
        doProjectsTests = (boolean) common.get("doProjectsTests");
        doCollectionsTests = (boolean) common.get("doCollectionsTests");
        doDocumentsTests = (boolean) common.get("doDocumentsTests");

        organizationsDir = (String) common.get("organizations.dir");
        projectsDir = (String) common.get("projects.dir");
        collectionsDir = (String) common.get("collections.dir");
        documentsDir = (String) common.get("documents.dir");


        doProjectsOrganizationsTests = (boolean) common.get("doProjectsOrganizationsTests");
        doCollectionsProjectsTests = (boolean) common.get("doCollectionsProjectsTests");
        doDocumentsCollectionsTests = (boolean) common.get("doDocumentsCollectionsTests");
    }

    private void runTests(){
        if (doOrganizationTests) organizationTests();
        if (doProjectsTests) projectsTests();
        if (doCollectionsTests) collectionsTests();
        if (doDocumentsTests) documentsTests();
    }


    private void organizationTests(){
        JSONObject testsConfig = (JSONObject) config.get("organizationsConf");
        JSONArray tests = (JSONArray) config.get("organizationsTests");
        if (!doProjectsOrganizationsTests){
            executeTestsArray(tests, testsConfig);
        }
        else {
            JSONArray projectsOrganizationsTests = (JSONArray) config.get("projectsOrganizationsTests");
            JSONArray postponedTests = executeOrPostpone(tests, testsConfig);
            executeMixedTests(projectsOrganizationsTests, testsConfig, projectsDir);
            executeTestsArray(postponedTests, testsConfig);
        }
    }

    private void projectsTests(){
        JSONObject testsConfig = (JSONObject) config.get("projectsConf");
        JSONArray tests = (JSONArray) config.get("projectsTests");
        if (!doCollectionsProjectsTests){
            executeTestsArray(tests, testsConfig);
        }
        else {
            JSONArray collectionsProjectsTests = (JSONArray) config.get("collectionsProjectsTests");
            JSONArray postponedTests = executeOrPostpone(tests, testsConfig);
            executeMixedTests(collectionsProjectsTests, testsConfig, collectionsDir);
            executeTestsArray(postponedTests, testsConfig);
        }
    }

    private void collectionsTests(){
        JSONObject testsConfig = (JSONObject) config.get("collectionsConf");
        JSONArray tests = (JSONArray) config.get("collectionsTests");
        if (!doDocumentsCollectionsTests){
            executeTestsArray(tests, testsConfig);
        }
        else {
            JSONArray documentsCollectionsTests = (JSONArray) config.get("documentsCollectionsTests");
            JSONArray postponedTests = executeOrPostpone(tests, testsConfig);
            executeMixedTests(documentsCollectionsTests, testsConfig, documentsDir);
            executeTestsArray(postponedTests, testsConfig);
        }
    }

    private void documentsTests(){
        JSONObject testsConfig = (JSONObject) config.get("collectionsConf");
        JSONArray tests = (JSONArray) config.get("collectionsTests");
        executeTestsArray(tests, testsConfig);
    }

    private void executeTest(JSONObject testsConfig, JSONObject test){
        if ((boolean) test.get("enabled") == false) return;
        long iterationsCount = (long) testsConfig.get(test.get("iterations.count"));
        String path = scenariosRootDir + "/" + testsConfig.get("dir") + "/" + test.get("file");
        executeTest(iterationsCount, path);
    }

    private void executeTest(long iterationsCount, String path){
        Properties properties = new Properties();
        properties.setProperty("server.host", serverHost);
        properties.setProperty("thread.count", String.valueOf(threadsCount));
        properties.setProperty("iterations.count", String.valueOf(iterationsCount));
        properties.setProperty("charts.dir", chartsDir);
        try {
            System.out.println(path);
            ScenarioExecution.execute(path, properties);
        } catch (PerfCakeException e) {
            e.printStackTrace();
        }
    }

    private JSONArray executeOrPostpone(JSONArray tests, JSONObject testsConfig){
        JSONArray postponedTests = new JSONArray();
        for (int i = 0; i < tests.size(); i++){
            JSONObject test = (JSONObject) tests.get(i);
            if ("true".equals(test.get("postponed")))
                postponedTests.add(test);
            else
                executeTest(testsConfig, test);

        }
        return postponedTests;
    }

    private void executeMixedTests(JSONArray invadorTests, JSONObject invadedTestConf, String invadorDir){
        for (int i = 0; i < invadorTests.size(); i++) {
            JSONObject test = (JSONObject) invadorTests.get(i);
            long iterationCount = (long) invadedTestConf.get(test.get("iterations.count"));
            String path = scenariosRootDir + "/" + invadorDir + "/" + test.get("file");
            executeTest(iterationCount, path);
        }
    }

    private void executeTestsArray(JSONArray tests, JSONObject testsConfig){
        for (int i = 0; i < tests.size(); i++)
            executeTest(testsConfig, (JSONObject) tests.get(i));
    }

}
