#!/bin/bash
set -e

if [ -z $1 ] || [ "$1" = "--help" ] || [ -z $2 ]; then
	echo "$0 <host> <dump directory> [dbname] [schema] [arguments]"
	exit
fi

HOST=$1
shift
DIRECTORY=$1
shift
if [ ! -z "$1" ]; then
	DATABASE=$1
	shift
else
	DATABASE="gros"
fi
if [ ! -z "$1" ]; then
	SCHEMA=$1
	shift
else
	SCHEMA="gros"
fi
ARGUMENTS=$@

ERROR_TEXT="syntax error|current transaction is aborted"

function import() {
	local table=$1
	local error_text="syntax error"

	if [ -f "$DIRECTORY/$table.sql.gz" ]; then
		echo "Importing $table from SQL"
		if [ "$(gzcat "$DIRECTORY/$table.sql.gz" | mclient -d "$DATABASE" -h "$HOST" $ARGUMENTS 2>&1 | tee >(cat>&2) | grep -m 1 -E "$ERROR_TEXT")" ]; then
			exit 1
		fi
	elif [ -f "$DIRECTORY/$table.csv.gz" ]; then
		echo "Importing $table from CSV"
		if [ "$(gzcat "$DIRECTORY/$table.csv.gz" | mclient -d "$DATABASE" -h "$HOST" $ARGUMENTS -s "COPY INTO $SCHEMA.$table FROM STDIN USING DELIMITERS ',', '\n', '\"' NULL AS '\\007NUL\\007'" - 2>&1 | tee >(cat>&2) | grep -m 1 -E "$ERROR_TEXT")" ]; then
			exit 1
		fi
	else
		echo "Could not find $DIRECTORY/$table.sql.gz or $DIRECTORY/$table.csv.gz"
		exit 1
	fi
}

echo "Importing schema"
if [ "$(cat "$DIRECTORY/schema.sql" | mclient -d "$DATABASE" -h "$HOST" $ARGUMENTS 2>&1 | tee >(cat>&2) | grep -m 1 -E "$ERROR_TEXT")" ]; then
	exit 1
fi

LINES=`cat "$DIRECTORY/schema.sql" | grep "^CREATE TABLE \"$SCHEMA\"" | \
	sed -e 's/CREATE TABLE ".*"\."\(.*\)" (/\1/'`

for line in $LINES
do
	import "$line"
done
