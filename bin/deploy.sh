#!/bin/bash

set -e
cd $(dirname $0)
lein with-profile uberjar do clean, cljsbuild once
cd ../resources/public
rm -fr .git
git init
git add .
git commit -m "Deploy to GitHub Pages"
git push --force --quiet "git@github.com:timothypratley/leaderboardx.git" main:gh-pages
rm -fr .git
echo https://timothypratley.github.io/leaderboardx
