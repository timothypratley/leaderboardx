#!/bin/sh
set -e
git push heroku HEAD:master
echo http://leaderboardx.herokuapp.com/
