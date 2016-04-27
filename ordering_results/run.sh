#!/bin/bash


KMOST=10

# thx to http://stackoverflow.com/questions/192249/
while [[ $# > 1 ]]
do
key="$1"

case $key in
    -f|--file)
    FILE="$2"
    shift # past argument
    ;;
    -t|--tool)
    TOOL="$2"
    shift # past argument
    ;;
    -k|--topk)
    KMOST="$2"
    shift # past argument
    ;;
    --default)
    DEFAULT=YES
    ;;
    *)
            # unknown option
    ;;
esac
shift # past argument or value
done

if [[ -n "$TOOL" ]] && [[ "${TOOL}" == "concepts" ]]; 
then
	echo "Extracts concepts from corpus."	
	./cue concepts -k $KMOST -f $FILE	
else
	if [[ -n "$TOOL" ]] && [[ "${TOOL}" == "typicality" ]]; then
		echo "Finds k most typical implementation in corpus."
		./cue typical -k $KMOST -f $FILE
	else	
		echo "Runs both typicality and concepts commands"
		./cue typical -k $KMOST -f $FILE		
		./cue concepts -k $KMOST -f $FILE		
	fi
fi	