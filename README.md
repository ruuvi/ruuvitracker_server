# ruuvi-server

Clojure based implementation of *RuuviTrackerServer*.

See http://www.ruuvitracker.fi/ for more details.

Related HTML5 based user interface is available https://github.com/RuuviTracker/ruuvitracker_web.

# Demo servers

You can find a demo of  this software running at http://dev-server.ruuvitracker.fi/ .

# General information

Server works as standalone application. Using a HTTP Servlet container (like Tomcat or JBoss) may be possible, but that will not support WebSockets.

This software provides only REST and WebSocket APIs. User interface is available in separate project 
[RuuviTracker/ruuvitracker_web](https://github.com/RuuviTracker/ruuvitracker_web).

# Discussion

Most of the discussions about development happen on IRCNet channel #ruuvitracker.

There is also a mailing list at https://groups.google.com/forum/?fromgroups#!forum/ruuvitracker-dev

## Prerequisites

* git: http://git-scm.com/

* Leiningen: https://github.com/technomancy/leiningen
 * Use 2.0 version.
 * With Linux, try ```sudo apt-get install leiningen``` or ```sudo yum install leiningen```. Most of the distributions will currently have old 1.x.x version of leiningen.

## Implementation

Currently server implements [Tracker API](http://wiki.ruuvitracker.fi/wiki/Tracker-API) and [Client API](http://wiki.ruuvitracker.fi/wiki/Client-API) partially.

* Requests are saved to database, but they are not properly validated yet.

### Standalone application

1. Fetch sources
```
git clone git://github.com/jsyrjala/ruuvitracker_server.git
```
2. Setup database, see ```src/resources/server-dev-config.clj```
3. Fetch dependencies
```
lein deps
```
4. Create tables to database
<pre>
lein run -m ruuvi-server.launcher migrate
</pre>

5. (Optional) Import example data to database
<pre>
lein run -m ruuvi-server.launcher load-example-data
</pre>
6. Start server
```
lein ring server
```
7. Access web application
http://localhost:3001/api/v1-dev/events

## Database

Server uses [PostgreSQL](http://www.postgresql.org/) or [H2](http://www.h2database.com/) database engines. See `resources/server-XXX-config.clj` files for database configuration.

## UI develpment

Static html files are located in ```resources/public```.

## Continuous Integration

[Travis](http://travis-ci.org/) is used as CI.

See http://travis-ci.org/#!/RuuviTracker/ruuvitracker_server

* master branch: <img src="https://secure.travis-ci.org/RuuviTracker/ruuvitracker_server.png?branch=master"/>

## Guide to source tree

Production code lives under 'src', unit-tests and other automatic tests live under 'test'. Directory 'test-utils' contains scripts usable in manual testing. Directory 'resources' contains data files that are not executable code.

Unit tests are implemented with [Midje](https://github.com/marick/Midje).

### src directory

* 'ruuvi_server/' contains main functionality of the server.
 * 'launcher.clj' is the starting point. Handles configuration and starts up serveres.
 * 'configuration.clj' contains functions to handle configuration data. It also contains atom that holds current configuration.
 * 'core.clj' sets up basic REST routes for whole software.
 * 'rest_routes.clj' is creates routes for REST API.
 * 'event_service.clj' contains service to handle event. Clients (web browsers, mobile devices) can get location data via JSON api.
 * 'tracker_service.clj' contains service to handle events sent from a tracker device. Tracker device can send location and other data using JSON API. 
 * 'tracker_security.clj' implements security features used in tracker API. [HMAC](http://en.wikipedia.org/wiki/HMAC) based message verification. 
 * 'user_service.clj'  contains service to handle users, groups and authentication.
 * 'util.clj' contains generic utility functions.
 * 'parse.clj' contains input parsing functions.
* 'lobos/' contains database migration files (a.k.a database schema changes). Migrations are implemented with [Lobos](https://github.com/budu/lobos) frameworks.
* 'ruuvi_server/database/' contains database access layer, scripts for initally populating the database with test data and connection pooling. Database access is implemented with [Korma](http://sqlkorma.com/) library (events) and direct java.jdbc (users and groups).
 * 'entities.clj' contains definitions for ORM entities. Currently entities are provided only for event related tables.
 * 'event_dao.clj' contains DAO functions for manipulating event data.
 * 'user_dao.clj' contains DAO functions for manipulating user and group data.
 * 'pool.clj' constructs a connection pool.

## License

Copyright (C) 2012 Juha Syrjälä

BSD License

