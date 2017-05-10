FROM inexus.ixxus.co.uk:28443/ixxus/alfresco-repository:5.1.2
MAINTAINER Alex Lu (alex.lu@ixxus.com)

ADD docker/dev-log4j.properties $ALF_HOME/tomcat/shared/classes/alfresco/extension

ADD target/*.jar /opt/alfresco/modules/platform
#ADD target/lib/*.jar /opt/alfresco/modules/platform
