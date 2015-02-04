# loadrunner-wrapper
LoadRunner wrapper is maven plugin designed to run LoadRunner controller and Analysis as part of CI procedure.
Additional functionality is exporting selected matrics from the analysis to simple csv file for future visualization using Plot Jenkins plugin

### pom.xml for execution configuration:
```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>my</groupId>
  <artifactId>plugin-usage</artifactId>
  <version>1.0</version>
  <name>my plugin usage</name>
  <build>
	<plugins>
		
		<!-- Run controller, analysis and extract summary report -->
		<plugin>
			<groupId>perflab</groupId>
			<artifactId>loadrunner-wrapper</artifactId>
			<version>1.0</version>
			<configuration>
				<loadRunnerBin>C:\\Program Files (x86)\\HP\\LoadRunner\\bin</loadRunnerBin>
				<scenario>C:\\Program Files (x86)\\HP\\LoadRunner\\scenario\\Scenario1.lrs</scenario>
				<resultsFolder>C:\\Jenkins\\workspace\\ZZZ\\88</resultsFolder>
				<htmlReportFolder>C:\\Jenkins\\workspace\\ZZZ\\88\\An_Report1</htmlReportFolder>
				<summaryFile>C:\\Jenkins\\workspace\\ZZZ\\lr_report.csv</summaryFile>
			</configuration>
		</plugin>
	</plugins>
  </build>

</project>
```

### This pom.xml can be parametrized for Jenkins usage as following:
```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>my</groupId>
  <artifactId>plugin-usage</artifactId>
  <version>1.0</version>
  <name>my plugin usage</name>
  <build>
	<plugins>
		
		<!-- Run controller, analysis and extract summary report -->
		<plugin>
			<groupId>perflab</groupId>
			<artifactId>loadrunner-wrapper</artifactId>
			<version>1.0</version>
			<configuration>
				<loadRunnerBin>${loadRunner.Bin}</loadRunnerBin>
				<scenario>${scenario}</scenario>
				<resultsFolder>${results.Folder}</resultsFolder>
				<htmlReportFolder>${htmlReport.Folder}</htmlReportFolder>
				<summaryFile>${summary.File}</summaryFile>
			</configuration>
		</plugin>
	</plugins>
  </build>

</project>
```
### Fields description:
${loadRunner.Bin - bin folder of LoadRunner installation folder
${scenario} - fill path to LoadRunner scenario (.lrs file)
${results.Folder} - LoadRunner scenario results folder (as defined in scenario)
${htmlReport.Folder} - root folder of analysis html report (as defined in Analysis template)
${summary.File} - path to short summary .csv file (average response times only) to be generated out of html analysis files

### Example of executing plugin from Jenkins:
- Add "Invoke top-level Maven Target" build step
- Use goal: perflab:loadrunner-wrapper:1.0:run-lr
- Set properties:
    loadRunner.Bin=C:\\Program Files (x86)\\HP\\LoadRunner\\bin
    scenario=C:\\Program Files (x86)\\HP\\LoadRunner\\scenario\\Scenario1.lrs
    results.Folder=%WORKSPACE%\\%BUILD_NUMBER%
    htmlReport.Folder=%WORKSPACE%\\%BUILD_NUMBER%\\An_Report1
    summary.File=%WORKSPACE%\\lr_report.csv

Enjoy :)
