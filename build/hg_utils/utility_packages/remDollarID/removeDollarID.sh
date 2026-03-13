#!/bin/sh
#Script used to delete dollarid ($Id) as requested files

main()
{
	[ $1 ] || { echo "Usage : $0 <ENTER SOURCE_DIR> "; exit 1; }
	SOURCE_DIR=`echo $1 | tr "," " "`
	removeDollarID
}

removeDollarID()
{
	for i in `find $SOURCE_DIR -iname "*.*"`
	do
		exttype=`echo $i | awk -F "." '{print $NF}'`
		case ${exttype} in
		"sh" )
			comment="\\#:\\$"
			;;
		"bat" )
			comment="rem:\\$"
			;;
		"conf" )
			comment="\\#:\\$,//:\\$"
			;;
		"properties" )
                        comment="\\#:\\$"
                        ;;
		"tag" )
                        comment="<\\%--:--\\%>"
                        ;;
#		"java" )
#			comment="//:\\$,/\\*:\\*/"
#			;;
                "js" )
		        comment="//:\\$,/\\*:\\*/"
		        ;;
                "html" )
                        comment="<\\!--:-->,<\\!:-->"
                        ;;		
		"xml" )
			comment="<\\!--:-->,//:\\$"
			;;
		"css" )
			comment="/\\*:\\*/,//:\\$"
			;;
		"jsp" )
			comment="<\\%--:--\\%>,<\\!--:-->,//:\\$,/\\*:\\*/"
			;;
		"jspf" )
			comment="<\\%--:--\\%>,<\\!--:-->,//:\\$,/\\*:\\*/"
			;;
		* )
			comment=""
			;;
		esac
		if [ "$comment" != "" ]
		then
			for j in `echo $comment | tr "," " "`
			do
				start=`echo $j | cut -d ":" -f1`
				end=`echo $j | cut -d ":" -f2`
         			perl -w -e "s@$start.*\\\$Id.*$end(\r)?(\\n)?@@g" -p -i $i
			done
		fi
		
	done
}
main $*
