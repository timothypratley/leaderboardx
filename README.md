# LeaderboardX

Build Sociograms, and other Pageranked graphs quickly:
http://leaderboardx.herokuapp.com/


## Why?

Analysis


## What?

A graph of social relationships.


## How?

* Build networks quickly
* Rename nodes
* Combine nodes
* Relink edges
* Pagerank
* Force layout
* Move nodes and links
* Save files


## Development

Start Datomic (from datomic directory):
`bin/transactor config/dev-leaderboardx.properties`

Start the server:
`lein run`

For browser reloading:
`lein figwheel app devcards`

Open http://localhost:3000

To test advanced compilation:
`lein with-profile uberjar do clean, cljsbuild auto`
`lein run 8080`
http://localhost:8080

If the schema changes:
`lein run migrate`

To reset the database:
datomic delete
`lein run migrate`

To run the Datomic console:
`./bin/console -p 8088 dev datomic:dev://localhost:4334/leaderboardx`
Open http://localhost:8088

## Thoughts

Bind is one way with datascript.
Reactions are cool for propigating change, but what about pushing change?
Om-next will be awesome.

## Deployment

`./bin/deploy.sh`


## License

Copyright © 2015 Timothy Pratley
