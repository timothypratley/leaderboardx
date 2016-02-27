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


## Deployment

`./bin/deploy.sh`


## License

Copyright © 2015 Timothy Pratley
