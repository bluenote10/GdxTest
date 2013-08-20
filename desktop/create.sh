#!/bin/bash

if [ $# -eq 0 ]
then
	echo "Usage: `basename $0` <directory_to_create>"
	exit 65
fi

dir="$1"

if [ -d "$dir" ]; then
  echo "Error: directory $dir already exists"
  exit
fi


cd `dirname $0`

mkdir -p "$dir"
mkdir -p "$dir/libs"
mkdir -p "$dir/src"
mkdir -p "$dir/src/com"
mkdir -p "$dir/src/com/fablauncher"

cp ./libs/*.jar               "$dir/libs/"
cp ../libs/gdx.jar            "$dir/libs/"
cp ../libs/gdx-sources.jar    "$dir/libs/"

cp "./LauncherDesktop.java" "$dir/src/com/fablauncher/LauncherDesktop.java"
cp "./.project"             "$dir/.project"
cp "./.classpath"           "$dir/.classpath"

ln -s `cd ../assets/data && pwd` "$dir/data"
ln -s `cd ../src/com/fab && pwd` "$dir/src/com/fab"







