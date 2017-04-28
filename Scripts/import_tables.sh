#!/bin/bash -e

if [ -z $1 ] || [ "$1" = "--help" ] || [ -z $2 ]; then
	echo "$0 <host> <dump directory>"
	exit
fi

HOST=$1
DIRECTORY=$2

function import() {
	local table=$1
	local error_text="current transaction is aborted"

	if [ ! -f "$DIRECTORY/$table.csv.gz" ]; then
		echo "Could not find $DIRECTORY/$table.csv.gz"
		exit
	fi

	if [ -f "$DIRECTORY/$file.csv.gz" ]; then
		echo "Importing $table from CSV"
		if [ "$(gunzip $DIRECTORY/$table.csv.gz | mclient -a -d gros -h $HOST -s \"COPY INTO gros.$table FROM STDIN USING DELIMITERS ',', '\n', '\"'\" - 2>&1 | tee /dev/stderr | grep -m 1 $error_text)" ]; then
			exit
		fi
		return
	fi
}

echo "Importing schema"
if [ "$(mclient -a -d gros -h $HOST $DIRECTORY/schema.sql 2>&1 | tee /dev/stderr | grep -m 1 "current transaction is aborted")" ]; then
	exit
fi

LINES=`cat create-tables.sql | grep '^CREATE TABLE' | \
	sed -e 's/CREATE TABLE ".*"\."\(.*\)" (/\1/'`

for line in $LINES
do
	import "$line"
done
