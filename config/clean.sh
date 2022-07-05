#!/bin/bash
if [ "$#" -ne 1 ] || ! [ -d "$1" ]; then
	echo "USAGE : $0 [DIR]" >&2
	exit 1
fi
DIR="$1"

cp $DIR/align/* $DIR/ >/dev/null 2>&1
cp $DIR/tree/* $DIR/ >/dev/null 2>&1
cp $DIR/misc/* $DIR/ >/dev/null 2>&1

rm -r $DIR/align/ $DIR/tree/ $DIR/misc/ >/dev/null 2>&1
mkdir $DIR/align/
mkdir $DIR/tree/
mkdir $DIR/misc/

mv $DIR/aligned_*.fasta $DIR/align/
mv $DIR/*.nwk $DIR/tree/
mv $DIR/*.fasta $DIR/misc/

mv $DIR/align/*concatenated* $DIR/
mv $DIR/tree/*concatenated* $DIR/

