# What to do next
* Please change namespace and prefix of the [content model file](repo-ext-content-model/src/resources/alfresco/module/repo-ext-content-model/model/content-model.xml)
* Bookmark [README TOC generator tool](http://ecotrust-canada.github.io/markdown-toc/)

# alf-wf-tutorial Project

- [License](#license)
- [Overview](#overview)
- [Architecture](#architecture)
- [Best Practices](#best-practices)
- [Feature](#feature)
- [Set up development environment](#set-up-development-environment)
- [How to run/build project](#how-to-run-build-project)
- [Troubleshooting guide](#troubleshooting-guide)

# License
All rights reserved. Copyright (c) Ixxus Ltd 2017.

# Overview
A short description that include:
* who the client is
* what the project is for

# Architecture
A short description of latest the solution architecture.

# Best Practices
Project specific best practices if any. Remove if there's not.

# Feature
This section will be maintained by developers. Changes must be included as part of pull requests.

# Set up development environment
Project specific instructions for setting up local environment. Remove if there's not.

## Integration-tests module
The generated projects include the module `integration-tests` that centralizes the management of tests for all modules.
It manages Scala tests based on Flat Spec style.

Scala configuration is distributed between the following files:
* _integration-tests/pom.xml_ includes the dependency `org.scalatest:scalatest_2.12` and the plugins `net.alchim31.maven:scala-maven-plugin` and `org.scalatest:scalatest-maven-plugin`
* _pom.xml_ provides the profile `ixxus-it` to run tests

Take a look at the module structure:
![Alt text](documentation/integration-tests-module.png?raw=true "Integration tests module structure")

* **SECTION 1**: it provides the default Java class `TestUtils` that defines utilities for tests. You can extend this class adding new features.
* **SECTION 2**: the alfresco-global.properties file includes the module's properties. You can customize it adding/removing/modifying them.
* **SECTION 3**: the default resources folder for tests
* **SECTION 4**: it provides the suite `ScalaIntegrationSuite.scala` to manage tests execution. When you generate a skeleton the suite include the example test `HelloWorldIT`.

### How to run Scala tests
The follow command runs tests included into the suite `ScalaIntegrationSuite.scala`.
```bash
cd path/skeletonProject
mvn clean test -Pixxus-it
```
If you don't use Maven profile `ixxus-it` it doesn't work.

# How to run/build project
## run.sh
A set of commands to run/build the project with [run.sh](run.sh)

### Build and start all containers
```bash
./run.sh build
```

### If you want to introduce changes to repository and rebuild the repository container
```bash
./run.sh repo
```

### If you want to introduce changes to share when and rebuild the share container
```bash
./run.sh share
```

## Alias/Doskey
If you do not wish to use [run.sh](run.sh), alternatively you could use Alias/Doskey to wrap each set of commands.

### Alias (macOS/Linux)
Change the following function names to your own liking and save to ~/.bash_profile
```bash
dalf() {
    mvn clean install -Pdocker -DskipTests=true
    docker-compose up --build -d
    docker-compose logs -f
}

drepo() {
    mvn clean install -Penterprise,docker -DskipTests=true
    docker-compose kill repository
    docker-compose rm -f repository
    docker-compose up --build -d repository
    docker-compose logs -f
}

dshare() {
    mvn clean install -Penterprise,docker -DskipTests=true
    docker-compose kill share
    docker-compose rm -f share
    docker-compose up --build -d share
}
```

### Doskey (Windows)
```bash
doskey dshare=docker-compose stop share ^& docker-compose rm -f share ^& mvn clean install -Penterprise,docker -DskipTests=true ^& docker-compose up -d share
doskey drepo=docker-compose stop repository ^& docker-compose rm -f repository ^& mvn clean install -Penterprise,docker -DskipTests=true ^& docker-compose up -d repository
doskey dalf=docker-compose stop ^& docker-compose rm -f ^& mvn clean install -Penterprise,docker -DskipTests=true ^& docker-compose up -d
```

# Troubleshooting guide
Document any issue regarding local development.

