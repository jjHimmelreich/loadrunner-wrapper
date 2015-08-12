package perflab;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.*;
import java.util.ArrayList;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


/**
 * Goal which executes loadrunner scenario, analyze it and extract short summary
 *
 * @goal run-lr
 * @phase process-sources
 */
public class LoadrunnerWrapper extends AbstractMojo {
    /**
     * @parameter property="run-lr.scenario" default-value=""
     */
    private String scenario;

    /**
     * @parameter property="run-lr.htmlReportFolder" default-value=""
     */
    private String htmlReportFolder;

    /**
     * @parameter property="run-lr.resultsFolder" default-value=""
     */
    private String resultsFolder;

    /**
     * @parameter property="run-lr.summaryFile" default-value=""
     */
    private String summaryFile;

    /**
     * @parameter property="run-lr.loadRunnerBin" default-value="c:\Program Files (x86)\HP\LoadRunner\bin"
     */
    private String loadRunnerBin;

    /**
     * @parameter property="run-lr.analysisTemplateName" default-value="DefaultTemplate1"
     */
    private String analysisTemplateName;

    /**
     * @parameter property="run-lr.additionalAttributes" default-value=""
     */
    private String additionalAttributes;

    private HashMap<String, ConfigurationValue> LRSFlags;
    private HashMap<String, ConfigurationValue> AnalysisTemplatesFlags;

    /**
     *
     */
    private ArrayList<LoadRunnerTransaction> transactions = new ArrayList<LoadRunnerTransaction>();

    /* (non-Javadoc)
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("run scenario = " + scenario);
        getLog().info("results-folder = " + resultsFolder);
        getLog().info("html-results-folder = " + htmlReportFolder);
        getLog().info("summary-file = " + summaryFile);
        getLog().info("analysisTemplateName = " + analysisTemplateName);
        getLog().info("loadRunnerBin = " + loadRunnerBin);
        getLog().info("additionalAttributes = " + additionalAttributes);

        setConfigurationFlags();

        boolean okay = true;

        getLog().info("======================= Checking Scenario and Analysis template configurations ======================= ");
        getLog().info("");

        /* check for files exists */
        //Check if lrs exists
        boolean lrsExists = checkIfScenarioExists(scenario);
        if (!lrsExists) {
            getLog().error("Scenario file " + scenario + " was not found on slave. Aborting job");
            okay = false;
        }

        //Check if LRS configured correctly
        if (!isFileWellConfigured(scenario, LRSFlags)) {
            okay = false;
        }

        //Check if Analysis template exists
        boolean analysisTemplateExists = checkIfTemplateExists(analysisTemplateName);
        if (!analysisTemplateExists) {
            getLog().error("Template file " + analysisTemplateName + " was not found on slave. Aborting job");
            okay = false;
        }

        if (!isFileWellConfigured(System.getenv("LR_PATH") + "\\AnalysisTemplates\\" + analysisTemplateName + "\\" + analysisTemplateName + ".tem",
                AnalysisTemplatesFlags)) {
            okay = false;
        }

        getLog().info("");
        getLog().info("====================================================================================================== ");

        if(!okay){
            throw new MojoExecutionException("Scenario and/or template are not configured well");
        }


    	/*Build controller command line*/
        StringBuilder sb = new StringBuilder("\"").append(loadRunnerBin).append("\\").append("Wlrun.exe").append("\"")
                .append(" -Run ")
                .append(" -TestPath ").append("\"").append(scenario).append("\"")
                .append(" -ResultName ").append("\"").append(resultsFolder).append("\"");

        if (additionalAttributes != null && !additionalAttributes.isEmpty()) {
            sb.append(" ").append(additionalAttributes);
        }

        String controllerCommand = sb.toString();
        //getLog().info("Run controller " + controllerCommand);

        runCommand("taskkill /f /im Wlrun.exe");

        /* Run controller */
        int controllerRC = runCommand(controllerCommand);

        if (controllerRC == 1) {
            /* Run Analysis if controller return code is okay */

            String resultsFile = getResultsFile(resultsFolder);

            if (resultsFile.isEmpty()) {
                getLog().error("Analysis session file (.lrr) was not  found in " + resultsFile + " folder. Aborting job");
                okay = false;
            }

            /*Build Analysis command line*/
            String analysisCommand = "\"" + loadRunnerBin + "\\AnalysisUI.exe\" -RESULTPATH " + resultsFile;
            if (analysisTemplateName != null && !analysisTemplateName.isEmpty()) {
                analysisCommand = analysisCommand + " -TEMPLATENAME " + analysisTemplateName;
            }

            int analysisRC = runCommand(analysisCommand);
			
			/* Parse analysis results and extract short report if analysis return code is okay */
            if (analysisRC == 0) {
                extractKPIs(resultsFolder, htmlReportFolder);
            }

        } else {
            getLog().error("Controller failed. Exit code is: " + controllerRC);
            throw new MojoExecutionException("Controller failed. Exit code is: " + controllerRC);
        }

    }

    /*Set LRS and LRR configuration flags*/
    private void setConfigurationFlags() {

        if (LRSFlags == null && AnalysisTemplatesFlags == null) {
            LRSFlags = new HashMap<String, ConfigurationValue>();
            AnalysisTemplatesFlags = new HashMap<String, ConfigurationValue>();

            LRSFlags.put("AutoSetResults", new ConfigurationValue("0", "Please clear 'Automatically create a results directory for each scenario execution' option at Controller->Results->Results Settings..."));
            LRSFlags.put("AutoOverwriteResults", new ConfigurationValue("0", "Please clear 'Automatically overwrite existing results directory without prompting for confirmation' option at Controller->Results->Results Settings..."));

            AnalysisTemplatesFlags.put("AutoHtml", new ConfigurationValue("1", "Please set 'Generate the following automatic HTML report' option at Analysis->Tools->Templates->" + analysisTemplateName));
            AnalysisTemplatesFlags.put("AutoSave", new ConfigurationValue("1", "Please set 'Automatically save the session at' option at Analysis->Tools->Templates->" + analysisTemplateName));
            AnalysisTemplatesFlags.put("AutoClose", new ConfigurationValue("1", "Please set 'Automatically close Analysis after saving session' option at Analysis->Tools->Templates->" + analysisTemplateName));
        }
    }

    /**
     * check if loadrunner scenario exists
     *
     * @return - true if LRS file exists
     */
    private boolean checkIfScenarioExists(String scenario) {
        boolean okay = false;

        File lrs = new File(scenario);
        if (lrs.exists() && !lrs.isDirectory()) {
            okay = true;
        }

        return okay;
    }

    private boolean isFileWellConfigured(String fileName, HashMap configurations) {
        boolean okay = true;

        try {
            String fileContent = FileUtils.readFileToString(new File(fileName));

            for (Object key : configurations.keySet()) {
                String keyStr = key.toString();

                ConfigurationValue p = (ConfigurationValue) configurations.get(key);
                String strToFind = keyStr + "=" + p.getValue();

                getLog().info("checking for " + strToFind);

                if (!fileContent.contains(strToFind)) {
                    okay = false;
                    getLog().error(fileName + ":" + strToFind + "  is missing or misconfigured . Aborting job");
                    getLog().error(p.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return okay;
    }

    /**
     * check if this.loadRunnerAnalysisTemplateName exists on filesystem
     *
     * @return - true if template file exists
     */
    private boolean checkIfTemplateExists(String analysisTemplateName) {
        boolean okay = false;

        File template = new File(System.getenv("LR_PATH") + "\\AnalysisTemplates\\" + analysisTemplateName);
        if (template.exists() && template.isDirectory()) {
            okay = true;
        }
        return okay;
    }

    /**
     * @param resultsFolder
     * @return name of lrr file name in results folder
     */
    private String getResultsFile(String resultsFolder) {

        getLog().info("Looking for lrr file in " + resultsFolder);

        String lrrFile = findFilebyRegex(resultsFolder, "*.lrr");

        return lrrFile;
    }

    /**
     * @param path
     * @param pattern
     * @return first file according to the pattern
     */
    private String findFilebyRegex(String path, String pattern) {
        String foundFile = "";

        try {

            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                FileFilter fileFilter = new WildcardFileFilter(pattern);
                File[] files = dir.listFiles(fileFilter);

                getLog().info("Length: " + files.length);

                foundFile = files[0].getAbsolutePath();
            } else if (dir.isFile()) {
                getLog().error(path + " not exists or not a folder...");
            }

        } catch (Exception e) {
            getLog().error("Can't find lrr file " + e.getMessage());
        }
        return foundFile;
    }

    /**
     * @param command - command to execute
     * @return command exit code
     */
    private int runCommand(String command) {
        int exitCode = -1;
        getLog().info("Command to run: " + command);

        try {
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
            exitCode = p.exitValue();
        } catch (Exception err) {
            err.printStackTrace();
        }

        getLog().info("Exit value: " + exitCode);
        return exitCode;
    }

    /**
     * Generates report in format expected by https://wiki.jenkins-ci.org/display/JENKINS/PerfPublisher+Plugin
     * examples here: file:///C:/Users/i046774/Downloads/master-s-thesis-designing-and-automating-dynamic-testing-of-software-nightly-builds.pdf
     */
    protected void extractKPIs(String resultsFolder, String htmlReportFolder) {
        parseSummaryFile(htmlReportFolder + "\\summary.html", summaryFile);

        //generatePerfPublisherReport(this.transactions, xmlSummaryFile);
        //generatePlotCSVReport(this.transactions, summaryFile);
        generatejUnitReport(this.transactions, summaryFile);
    }

    /**
     * @param htmlSummaryFile - load runner analysis html report file to parse
     * @param summaryFile     - location of summary file to be generated out of loadrunner html analysis
     */
    protected void parseSummaryFile(String htmlSummaryFile, String summaryFile) {
        try {

            File input = new File(htmlSummaryFile);
            Document document = Jsoup.parse(input, "UTF-8");
            Document parse = Jsoup.parse(document.html());
            Elements table = parse.select("table").select("[summary=Transactions statistics summary table]");
            Elements rows = table.select("tr");

            getLog().info("number of rows in summary file=" + rows.size());

            for (Element row : rows) {

                //getLog().info("table element = " + row.toString());

                String name = row.select("td[headers=LraTransaction Name]").select("span").text();

                if (!name.isEmpty()) {

                    float avgRT = Float.valueOf(row.select("td[headers=LraAverage]").select("span").text());
                    float minRT = Float.valueOf(row.select("td[headers=LraMinimum]").select("span").text());
                    float maxRT = Float.valueOf(row.select("td[headers=LraMaximum]").select("span").text());
                    int passed = Integer.valueOf(row.select("td[headers=LraPass]").select("span").text().replace(".", "").replace(",", ""));
                    int failed = Integer.valueOf(row.select("td[headers=LraFail]").select("span").text().replace(".", "").replace(",", ""));
                    int failedPrecentage = failed / (failed + passed) * 100;

                    getLog().info("Saving Transaction [" + name + "]");
                    this.transactions.add(new LoadRunnerTransaction(name, minRT, avgRT, maxRT, passed, failed, failedPrecentage));
                }
            }

        } catch (IOException e) {
            getLog().error("Can't read LoadRunner Analysis html report " + e.getMessage());
        }

    }


    /**
     * @param transactions - ArrayList of LoadRunnerTransaction objects
     * @param summaryFile  - location of xml summary file to be generated out of transaction objects in jUnit format
     * @return
     */
    private void generatejUnitReport(ArrayList<LoadRunnerTransaction> transactions, String summaryFile) {

        getLog().info("Transformation to jUnit XML started ...");

        String stringReport = "";

        try {
				/*
				 * http://llg.cubic.org/docs/junit/
				 <testsuite tests="3" time="42.5">
    					<testcase classname="ZZZ_1" name="ZZZ_1" time="10.1"/>
    					<testcase classname="ZZZ_2" name="ZZZ_2" time="11.7"/>
    					<testcase classname="ZZZ_3" name="ZZZ_3" time="12.2">
        				<!--failure type="NotEnoughFoo"> too slow </failure-->
    					</testcase>
				</testsuite>
				 */

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            org.w3c.dom.Document doc = (org.w3c.dom.Document) docBuilder.newDocument();
            org.w3c.dom.Element testsuiteElement = (org.w3c.dom.Element) doc.createElement("testsuite");
            testsuiteElement.setAttribute("tests", String.valueOf(transactions.size()));
            //testsuiteElement.setAttribute("time", "total test duration");
            doc.appendChild(testsuiteElement);

            ////////////////////////////////////////////////////////////////////////////

            for (LoadRunnerTransaction tr : this.transactions) {

                getLog().info("Dump " + tr.getName());

                org.w3c.dom.Element testcaseElement = doc.createElement("testcase");
                testcaseElement.setAttribute("classname", "load.ResponseTime");
                testcaseElement.setAttribute("name", tr.getName());
                testcaseElement.setAttribute("time", String.valueOf(tr.getAvgRT()));

                testsuiteElement.appendChild(testcaseElement);
            }

            ////////////////////////////////////////////////////////////////////////////
            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            DOMSource source = new DOMSource(doc);

            File lr_report = new File(summaryFile);
            StreamResult result = new StreamResult(lr_report);

            transformer.transform(source, result);

            getLog().info("File saved in " + lr_report.getAbsolutePath());

        } catch (ParserConfigurationException pce) {
            getLog().error(pce.getMessage());
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            getLog().error(tfe.getMessage());
            tfe.printStackTrace();
        }
    }

    /**
     * @param transactions - ArrayList of LoadRunnerTransaction objects
     * @param summaryFile  - locatiob of SCV summary file to be generated out of transaction objects
     * @return
     */
    private boolean generatePlotCSVReport(ArrayList<LoadRunnerTransaction> transactions, String summaryFile) {

        boolean rc = false;

        getLog().info("Transformation CSV started ...");

        ArrayList<String> headers = new ArrayList<String>();
        ArrayList<String> averages = new ArrayList<String>();

        for (LoadRunnerTransaction tr : this.transactions) {
            headers.add("\"" + tr.getName() + "\"");
            averages.add(String.valueOf(tr.getAvgRT()));
        }

        String scvReport = org.apache.commons.lang3.StringUtils.join(headers, ",") + System.getProperty("line.separator") +
                org.apache.commons.lang3.StringUtils.join(averages, ",");


        try {
            FileUtils.writeStringToFile(new File(summaryFile), scvReport);
            getLog().info(scvReport);
            getLog().info("Report is saved to " + summaryFile);
            rc = true;
        } catch (IOException e) {
            e.printStackTrace();
            rc = false;
            getLog().error("Can't write custom csv report for plotting " + e.getMessage());
        }

        getLog().info("Transformation CSV finished ...");
        return rc;
    }

    /**
     * @param transactions - ArrayList of LoadRunnerTransaction objects
     * @return
     */
    private void generatePerfPublisherReport(ArrayList<LoadRunnerTransaction> transactions, String xmlFile) {
        getLog().info("Transformation to XML started ...");
        try {

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            org.w3c.dom.Document doc = (org.w3c.dom.Document) docBuilder.newDocument();
            org.w3c.dom.Element reportElement = (org.w3c.dom.Element) doc.createElement("report");
            doc.appendChild(reportElement);

            ////////////////////////////////////////////////////////////////////////////

            //<categories>
            //   <category name="memory" scale="mb">
            //       <observations>
            //           <observation name="Server 1">100</observation>
            //           <observation name="Server 2">200</observation>
            //       </observations>
            //   </category>

            //   <category name="disk" scale="gb">
            //       <observations>
            //           <observation name="Server 1">41</observation>
            //           <observation name="Server 2">58</observation>
            //       </observations>
            //   </category>
            //</categories>

            // start element
            org.w3c.dom.Element startElement = (org.w3c.dom.Element) doc.createElement("start");
            reportElement.appendChild(startElement);

            // date element
            org.w3c.dom.Element date = (org.w3c.dom.Element) doc.createElement("date");
            startElement.appendChild(date);

            date.setAttribute("format", "YYYYMMDD");
            date.setAttribute("val", "20100922");

            // time element
            org.w3c.dom.Element time = (org.w3c.dom.Element) doc.createElement("date");
            startElement.appendChild(time);

            time.setAttribute("format", "HHMMSS");
            time.setAttribute("val", "215935");

            ////////////////////////////////////////////////////////////////////////////

            //<test name="Smoke test" executed="yes" categ="Smoke test">

            //<description>Tests if ATE LAN socket and communication works.</description>

            //<result>
            //	<success passed="yes" state ="100" hasTimedOut="no" />
            //	<compiletime unit="s" mesure="0" isRelevant="yes" />
            //	<performance unit="%" mesure="0" isRelevant="yes" />
            //	<executiontime unit="s" mesure="12" isRelevant="yes" />
            //	<metrics>
            //		<006_My_Benefits unit="sec" mesure="0.115" isRelevant="yes"/>
            //		<007_My_Timesheet unit="sec" mesure="1.247" isRelevant="yes"/>
            //	</metrics>
            //</result>
            //</test>
            //</report>

            ////////////////////////////////////////////////////////////////////////////
            // test element
            org.w3c.dom.Element testElement = doc.createElement("test");
            reportElement.appendChild(testElement);

            testElement.setAttribute("name", "Smoke test");
            testElement.setAttribute("executed", "yes");
            testElement.setAttribute("categ", "Smoke test");

            // description element
            org.w3c.dom.Element descriptionElement = doc.createElement("description");
            descriptionElement.appendChild(doc.createTextNode("This is the best Load test ever executed..."));
            reportElement.appendChild(descriptionElement);

            ////////////////////////////////////////////////////////////////////////////
            // result
            org.w3c.dom.Element resultElement = doc.createElement("result");
            reportElement.appendChild(resultElement);

            org.w3c.dom.Element successElement = doc.createElement("success");
            resultElement.appendChild(successElement);

            org.w3c.dom.Element compiletimeElement = doc.createElement("compiletime");
            resultElement.appendChild(compiletimeElement);

            org.w3c.dom.Element performanceElement = doc.createElement("performance");
            resultElement.appendChild(performanceElement);

            org.w3c.dom.Element executiontimeElement = doc.createElement("executiontime");
            resultElement.appendChild(executiontimeElement);

            org.w3c.dom.Element metricsElement = doc.createElement("metrics");
            resultElement.appendChild(metricsElement);

            ////////////////////////////////////////////////////////////////////////////

            for (LoadRunnerTransaction tr : this.transactions) {
                //<006_My_Benefits unit="sec" mesure="0.115" isRelevant="yes"/>
                String trName = "tr_" + tr.getName();
                getLog().info("Dump " + trName);

                org.w3c.dom.Element trElement = doc.createElement(trName);
                trElement.setAttribute("unit", "sec");
                trElement.setAttribute("mesure", String.valueOf(tr.getAvgRT()));
                trElement.setAttribute("isRelevant", "yes");
                metricsElement.appendChild(trElement);
            }

            ////////////////////////////////////////////////////////////////////////////
            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            DOMSource source = new DOMSource(doc);

            File lr_report = new File(xmlFile);
            StreamResult result = new StreamResult(lr_report);

            transformer.transform(source, result);

            getLog().info("File saved in " + lr_report.getAbsolutePath());

        } catch (ParserConfigurationException pce) {
            getLog().error(pce.getMessage());
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            getLog().error(tfe.getMessage());
            tfe.printStackTrace();
        }
    }

    private class ConfigurationValue {
        private String value;
        private String message;

        public ConfigurationValue(String value, String message) {
            this.value = value;
            this.message = message;
        }

        public String getValue() {
            return this.value;
        }

        public String getMessage() {
            return this.message;
        }
    }
}

