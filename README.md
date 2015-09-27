# LeaderboardX

Peer ranking
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

For interactive development:

`lein figwheel`
http://localhost:3449


To test advanced compilation:

`lein with-profile uberjar do clean, cljsbuild auto`
`lein run 8080`
http://localhost:8080

To start datomic:
bin/transactor config/samples/free-transactor-template.properties


## Thoughts

Bind is one way with datascript.
Reactions are cool for propigating change, but what about pushing change?

## License

Copyright © 2015 Timothy Pratley
