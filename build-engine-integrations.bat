@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.2"
i:\apache-maven-3.9.6\bin\mvn.cmd clean compile -f apps/engine/engine-integrations/pom.xml
