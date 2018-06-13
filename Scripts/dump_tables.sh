#!/bin/bash -e
set -o pipefail

function do_help() {
	echo "$0 <-h host|--help|--dry-run> [-o|--output directory]"
	echo "   [-s|--skip pattern] [-p|--dumper path] [-c|--config properies]"
	echo "   [-d|--defines ...]"
	echo ""
	echo "Perform a (partial) database dump of the GROS database."
	echo "Each table is written into a separate file in a timestamped output"
	echo "directory; depending on whether it is considered a large table,"
	echo "the file is named <table>.sql.gz or <table>.csv.gz. Additionally,"
	echo "the database schema is dumped to schema.sql."
	exit
}

if [ -z $1 ]; then
	do_help
fi

# Check if an element is in a space-separated list.
# Returns 0 if the element is in the list, and 1 otherwise.
function is_in_list() {
	local element=$1
	shift
	local list=$*

	set +e
	echo $list | grep -w -q "\b$element\b"
	local status=$?
	set -e

	return $status
}

SCRIPT_DIR=$(dirname "${BASH_SOURCE[0]}")
SKIP_PATTERN="^$"

while [[ $# -gt 0 ]]; do
	key="$1"
	case $key in
		-h)
			if [[ $# -lt 2 || "$2" == -* ]]; then
				do_help
			fi

			HOST="$2"
			shift
			shift
			;;
		--help)
			do_help
			;;
		--dry-run)
			HOST="$1"
			shift
			;;
		-o|--output)
			OUTPUT_DIRECTORY="$2"
			shift
			shift
			;;
		-s|--skip)
			SKIP_PATTERN="$2"
			shift
			shift
			;;
		-p|--dumper)
			DUMPER_PATH="$2"
			shift
			shift
			;;
		-c|--config)
			DUMPER_CONFIG="$2"
			shift
			shift
			;;
		-d|--defines)
			shift
			while [[ $# -gt 0 && "$1" != -* ]]; do
				DEFINES="$DEFINES -D$1"
				shift
			done
			;;
		*)
			do_help
			;;
	esac
done

if [ ! -z "$2" ]; then
	OUTPUT_DIRECTORY="$2"
fi
if [ ! -z "$3" ]; then
	SKIP_PATTERN="$3"
fi
if [ ! -z "$4" ]; then
	DUMPER_PATH="$4"
fi
if [ ! -z "$5" ]; then
	DUMPER_CONFIG="$5"
fi

if [ -z "$OUTPUT_DIRECTORY" ]; then
	OUTPUT_DIRECTORY=`pwd`
fi
if [ -z "$DUMPER_PATH" ]; then
	DUMPER_PATH="$SCRIPT_DIR/../../monetdb-dumper/dist/databasedumper.jar"
	DUMPER_CONFIG="$SCRIPT_DIR/../../monetdb-dumper/nbproject/private/config.properties"
fi

TIMESTAMP=`date +%Y-%m-%d`
DUMP_DIRECTORY="$OUTPUT_DIRECTORY/gros-$TIMESTAMP"
large_tables=$(cat "$SCRIPT_DIR/large_tables.txt")

echo "HOST=$HOST"
echo "SCRIPT_DIR=$SCRIPT_DIR"
echo "DUMPER_PATH=$DUMPER_PATH"
echo "DUMP_DIRECTORY=$DUMP_DIRECTORY"
echo "DEFINES=$DEFINES"

if [ "$HOST" = "--dry-run" ]; then
	echo "DRY RUN"
else
	if [ ! -z "$DUMPER_CONFIG" ]; then
		if [ ! $(grep "^databasedumper.url" "$DUMPER_CONFIG" | grep -- "$HOST") ]; then
			echo "Make sure the database dumper is configured for the host."
			exit 1
		fi
	fi

	if [ -d "$DUMP_DIRECTORY" ]; then
		rm -rf "$DUMP_DIRECTORY"
	fi
	mkdir "$DUMP_DIRECTORY"
fi

LINES=`cat "$SCRIPT_DIR/create-tables.sql" | grep '^CREATE TABLE' | \
	sed -e 's/CREATE TABLE ".*"\."\(.*\)" (/\1/'`

for line in $LINES
do
	if [ $(echo $line | grep -E "$SKIP_PATTERN") ]; then
		echo "Not dumping $line (skip pattern); adding an empty placeholder"
		echo | gzip -c > "$DUMP_DIRECTORY/$line.sql.gz"
	else
		echo "Dumping $line"
		if [ "$HOST" != "--dry-run" ]; then
			if is_in_list $line $large_tables; then
				msqldump -d gros -h $HOST -t "gros.$line" | gzip -c > "$DUMP_DIRECTORY/$line.sql.gz"
			else
				java $DEFINES -jar $DUMPER_PATH $line "$DUMP_DIRECTORY/$line.csv.gz"
			fi
		fi
	fi
done

echo "Dumping schema"
if [ "$HOST" != "--dry-run" ]; then
	msqldump -d gros -h $HOST -D > "$DUMP_DIRECTORY/schema.sql"
fi
