# ruuvi-server

Clojure based implementation of *RuuviTrackerServer*.

See http://www.ruuvipenkki.fi/ruuvitracker for more details

Server works either with standalone jetty or with [Heroku](http://www.heroku.com/) cloud.

## Implementation

Currently server implements [Tracker API](http://www.ruuvipenkki.fi/ruuvitracker/API) partially.

* Requests are saved to database, but they are not properly validated.

### Standalone usage

1. Setup database, see ```ruuvi_server/standalone/config.clj```
2. Create tables to database
    
```
lein run -m ruuvi-server.standalone.migrate migrate 
```

3. Start server

```
lein run -m ruuvi-server.standalone.starter
```

### Heroku usage

1. Create Heroku account
  - Simply create an Heroku account and push code to Heroku. The server will start automatically.

2. Create heroku application

     heroku create --stack cedar


3. Add heroku as git remote

     git remote add heroku git@heroku.com:APPNAME.git


3. Enable database in heroku

     heroku addons:add shared-database


4. Create tables to database and some content to tables

     heroku run lein run -m ruuvi-server.heroku.migration migrate
     heroku run lein run -m ruuvi-server.heroku.migration populate-database

5. Start heroku process

     heroku scale web=1

6. Access you app 

http://app-name.herokuapp.com/api/v1/events

## Database

Server uses [PostgreSQL](http://www.postgresql.org/) database engine. Currently only hardcodes database engine specific code is in *entities.clj*.


## License

Copyright (C) 2012 Juha Syrjälä

BSD License
