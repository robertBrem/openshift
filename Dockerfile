FROM jboss/wildfly:13.0.0.Final
MAINTAINER Robert Brem <robert.brem@adesso.ch>
ADD target/backend.war $JBOSS_HOME/standalone/deployments/