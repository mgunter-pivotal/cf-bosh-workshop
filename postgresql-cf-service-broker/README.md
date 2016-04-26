# Cloud Foundry Service Broker for a PostgreSQL instance [![Build Status](https://travis-ci.org/cloudfoundry-community/postgresql-cf-service-broker.svg?branch=master)](https://travis-ci.org/cloudfoundry-community/postgresql-cf-service-broker)

A Cloud Foundry Service Broker for a PostgreSQL instance built based on [Spring Cloud Service Broker](https://github.com/spring-cloud/spring-cloud-cloudfoundry-service-broker).

The broker currently publishes a single service and plan for provisioning PostgreSQL databases.

## Design 

The broker uses a PostgreSQL table for it's meta data. It does not maintain an internal database so it has no dependencies besides PostgreSQL.

Capability with the Cloud Foundry service broker API is indicated by the project version number. For example, version 2.4.0 is based off the 2.4 version of the broker API.

## Running

Simply run the JAR file

### Locally

```
edit application.properties so that JDBC points to your test instance of Postgres
mvn package -DskipTests &&  java -jar target/postgresql-cf-service-broker-2.4.0-SNAPSHOT.jar
```

### In Cloud Foundry

Find out the database subnet and create a security group rule (postgresql.json):
```
[{"protocol":"tcp","destination":"10.10.8.0/24","ports":"5432"}]
```

import this into CF with:
```
cf create-security-group postgresql-service postgresql.json
```

Bind to the full cf install:
```
cf bind-running-security-group postgresql-service
```


Build the package with `mvn package` then push it out:
```
cf push postgresql-cf-service-broker -p target/postgresql-cf-service-broker-2.4.0-SNAPSHOT.jar --no-start
```

Export the following environment variables:

```
cf set-env postgresql-cf-service-broker JAVA_OPTS "-Dsecurity.user.password=mysecret"
```

Start the service broker:
```
cf start postgresql-cf-service-broker
```

Create Cloud Foundry service broker:
```
cf create-service-broker postgresql-cf-service-broker user mysecret http://postgresql-cf-service-broker.cfapps.io
```

Add service broker to Cloud Foundry Marketplace:
```
cf enable-service-access PostgreSQL -p "Basic PostgreSQL Plan" -o ORG
```

## Registering a Broker with the Cloud Controller

See [Managing Service Brokers](http://docs.cloudfoundry.org/services/managing-service-brokers.html).

Routes
======
|Routes|Method|Description|
|------|------|-----------|
|/v2/catalog|GET|Service and its plan details by this broker|
|/v2/service_instances/:id|PUT|create a dedicated database for this service|
|/v2/service_instances/:id|DELETE|delete previously created database for this service|
|/v2/service_instances/:id/service_bindings/:id|PUT|create user and grant privilege for the database associated with service.|
|/v2/service_instances/:id/service_bindings/:id|DELETE|delete the user created previously for this binding.|

