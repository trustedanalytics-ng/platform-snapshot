[![Dependency Status](https://www.versioneye.com/user/projects/573349d2a0ca35004cf77ca2/badge.svg?style=flat)](https://www.versioneye.com/user/projects/573349d2a0ca35004cf77ca2)

# platform-snapshot
Discovering artifact version from whole platform

# Calling platform-snapshot with REST API

* Trigger new platform snapshot

  Path: /rest/v1/snapshots/trigger

* Get platform snapshot configuration

  Path: /rest/v1/configuration

* Get latest snapshots

  Path: /rest/v1/snapshots

* Get platform snapshot by id

  Path: /rest/v1/snapshots/{id}

* Delete platform snapshots older then given date

  Path: /rest/v1/snapshots/delete?date={date}

* Get difference between snapshots to identify what has been changed

  Path: /rest/v1/snapshots/{idBefore}/diff/{idAfter} - returns all components metrics which has been changed

  Path: /rest/v1/snapshots/{idBefore}/diff/{idAfter}?aggregateBy=type - returns all components metrics which has
  been changed and aggregate changes by component type (Cloud Foundry Application, Cloud Foundry Service, Cloudera Service)

# Deployment

  You need to create PostgreSQL database instance. It's name should be equal to the name defined in platform-snapshot/manifest.yml
  which is generated during build.
  In manifest file environment variables should be defined:

  * `CLOUDERA_ADDRESS`
  * `CLOUDERA_PASSWORD`
  * `CLOUDERA_PORT`
  * `CLOUDERA_USER`

  Then you can push application to the Cloud Foundry.

# Development

  Testing: `mvn clean test`

  Building executable jar: `mvn clean package`
