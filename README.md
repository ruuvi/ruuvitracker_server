# ruuvi-server

Clojure based implementation of *RuuviTrackerServer*.

See http://www.ruuvipenkki.fi/ruuvitracker for more details

Server works either with standalone servlet container (for example [Jetty](http://jetty.codehaus.org/jetty/)) or with [Heroku](http://www.heroku.com/) cloud.

## Prerequisites

* git: http://git-scm.com/

* Leiningen: https://github.com/technomancy/leiningen
 * Use 2.0 version. Use 2.0.0-preview8 or later
 * With Linux, try ```sudo apt-get install leiningen``` or ```sudo yum install leiningen```


## Implementation

Currently server implements [Tracker API](http://wiki.ruuvitracker.fi/wiki/Tracker-API) and [Client API](http://wiki.ruuvitracker.fi/wiki/Client-API) partially.

* Requests are saved to database, but they are not properly validated yet.

### Standalone usage with Jetty

1. Fetch sources
```
git clone git://github.com/jsyrjala/ruuvitracker_server.git
```
2. Setup database, see ```src/ruuvi_server/standalone/config.clj```
3. Fetch dependencies
```
lein deps
```
4. Create tables to database and populate database
<pre>
lein run -m ruuvi-server.standalone.migrate migrate
lein run -m ruuvi-server.standalone.migration populate-database
</pre>
5. Start server
```
lein ring server
```
6. Access web application
http://localhost:3001/api/v1-dev/events

### Heroku usage

1. Get sources
```
git clone git://github.com/RuuviTracker/ruuvitracker_server.git
```
2. Create Heroku account
  - Simply create an Heroku account and push code to Heroku. The server will start automatically.
3. Create heroku application
```
heroku create --stack cedar
```
4. Add heroku as git remote
```
git remote add heroku git@heroku.com:APPNAME.git
```
5. Configure heroku
```
heroku addons:add shared-database
heroku config:add RUUVISERVER_ENV=heroku --app APPNAME
```
6. Create tables to database and some content to tables
<pre>
heroku run lein run -m ruuvi-server.heroku.migration migrate
heroku run lein run -m ruuvi-server.heroku.migration populate-database
</pre>
7. Start heroku process
```
heroku scale web=1
```
8. Access the app 
http://APPNAME.herokuapp.com/api/v1-dev/events

## Database

Server uses [PostgreSQL](http://www.postgresql.org/) or [H2](http://www.h2database.com/) database engines. See `resources/server-XXX-config.clj` files for database configuration.

## UI develpment

Static html files are located in ```resources/public```. Add javascript ui there.

## Continuous Integration

[Travis](http://travis-ci.org/) is used as CI.

See http://travis-ci.org/#!/RuuviTracker/ruuvitracker_server

* master branch: <img src="https://secure.travis-ci.org/RuuviTracker/ruuvitracker_server.png?branch=master"/>

## Guide to source tree

Production code lives under 'src', unit-tests and other automatic tests live under 'test'. Directory 'test-utils' contains scripts usable in manual testing. Directory 'resources' contains data files that are not executable code.

Unit tests are implemented with [Midje](https://github.com/marick/Midje).

### src directory

* 'ruuvi_server' contains main functionality of the server.
 * 'core.clj' is the main starting point of the server.
 * 'api.clj' is the main starting point for API
 * 'client_api.clj' contains implementation of client part of API. Clients (web browsers, mobile devices) can get location data via JSON api.
 * 'tracker_api.clj' contains implementation of tracker API. Tracker device can send location and other data using JSON API. 
 * 'tracker_security.clj' implements security features used in tracker API. [HMAC](http://en.wikipedia.org/wiki/HMAC) based message verification. 
 * 'util.clj' contains generic utility functions.
 * 'parse.clj' contains input parsing functions.
* 'lobos' contains database migration files (a.k.a database schema changes). Migrations are implemented with [Lobos](https://github.com/budu/lobos) frameworks.
* 'ruuvi_server/database' contains database access layer, scripts for initally populating the database with test data and connection pooling. Database access is implemented with [Korma](http://sqlkorma.com/) library.
* 'ruuvi_server/heroku' contains wrappers for running server in [Heroku](http://www.heroku.com/)
* 'ruuvi_server/standalone' contains wrappers for running server as a standalone application with [Jetty](http://www.eclipse.org/jetty/) server. 

## License

Copyright (C) 2012 Juha Syrjälä

BSD License

