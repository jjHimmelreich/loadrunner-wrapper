set WORKSPACE=C:\temp\workspace
set BUILD_NUMBER=7

c:\Apache-maven-3.2.3\bin\mvn.bat -X ^
  -f C:\GitHub\loadrunner-wrapper\src\test\resources\pom.xml ^
  -D=LoadRunnerNode ^
  "-DloadRunner.Bin=C:\Program Files (x86)\HP\LoadRunner\bin" ^
  "-Dscenario=C:\Program Files (x86)\HP\LoadRunner\scenario\Scenario1.lrs" ^
  "-DadditionalAttributes=-attrName attrValue" ^
  -Dresults.Folder=%WORKSPACE%\%BUILD_NUMBER% ^
  -DanalysisTemplateName=DefaultTemplate1 ^
  -DhtmlReport.Folder=%WORKSPACE%\%BUILD_NUMBER%\An_Report1 ^
  -Dsummary.File=%WORKSPACE%\lr_jUnit_report.xml ^
  perflab:loadrunner-wrapper:1.1-SNAPSHOT:run-lr
