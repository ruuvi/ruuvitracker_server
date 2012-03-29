# ruuvi-server

Clojure based implementation of *RuuviTrackerServer*.

See http://www.ruuvipenkki.fi/ruuvitracker for more details

Server works either with standalone servlet container (for example [Jetty](http://jetty.codehaus.org/jetty/)) or with [Heroku](http://www.heroku.com/) cloud.

## Prerequisites

* git: http://git-scm.com/

* Leiningen: https://github.com/technomancy/leiningen
 * With Linux, try ```sudo apt-get install leiningen``` or ```sudo yum install leiningen```


## Implementation

Currently server implements [Tracker API](http://www.ruuvipenkki.fi/ruuvitracker/Tracker-API) partially.

* Requests are saved to database, but they are not properly validated.

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
5. Enable database in heroku
```
heroku addons:add shared-database
```
6. Create tables to database and some content to tables
```
heroku run lein run -m ruuvi-server.heroku.migration migrate
heroku run lein run -m ruuvi-server.heroku.migration populate-database
```
7. Start heroku process
```
heroku scale web=1
```
8. Access you app 
http://APPNAME.herokuapp.com/api/v1-dev/events

## Database

Server uses [PostgreSQL](http://www.postgresql.org/) database engine. Currently only hardcodes database engine specific code is in *entities.clj*.


## UI develpment

Static html files are located in ```resources/public```. Add javascript ui there.

## Continuous Integration

[Travis](http://travis-ci.org/) is used as CI.

See http://travis-ci.org/#!/RuuviTracker/ruuvitracker_server

* <img src="https://secure.travis-ci.org/RuuviTracker/ruuvitracker_server.png?branch=master"/>

## License

Copyright (C) 2012 Juha Syrjälä

BSD License
