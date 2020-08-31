#!/bin/bash
set -e
cd $(dirname $0)
lein with-profile uberjar do clean, cljsbuild once
cd ../resources/public
mv index.html index.html.bak
awk -v n=3 -v s='<h3>This is the development version of LeaderboardX.<br>If you would prefer the stable version, please visit <a href="http://leaderboardx.herokuapp.com/">http://leaderboardx.herokuapp.com</a></h3>' 'NR == n {print s} {print}' index.html.bak > index.html
git init
git add .
git commit -m "Deploy to GitHub Pages"
git push --force --quiet "git@github.com:timothypratley/leaderboardx.git" master:gh-pages
mv index.html.bak index.html
rm -fr .git
echo http://timothypratley.github.io/leaderboardx
