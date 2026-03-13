#!/bin/sh
#***************************************************************************************#
# Convert jsp scriptlet to out.print and add NO OUTPUTENCODING comments within jsp tag  #
#***************************************************************************************#

# check one argument is given
[ $1 ] || { echo "Usage: $0 <SOURCE FOLDER>"; exit 1; }

main()
{
	processid=$$
	find $1 -name "*.jsp" > $$.jspfiles
	find $1 -name "*.jspf" >> $$.jspfiles
	cat $$.jspfiles | xargs egrep -i -l "no outputencoding|nooutputencoding" > $$.commentedfiles
	cat $$.commentedfiles | while read file
	do
		#perl -0777 -pi -e 's/<%=(.*)%>/<%out.print(\1);\/\/NO OUTPUTENCODING%>/g' $file
		tmpfile="$file.tmp"

		# last line ignored by while loop so putting this work-around.
		echo "" >> $file
		#cat $file | while read line
		# IFS for preserve space and tabs in the line
		cat $file | while IFS= read -r line
		do
			echo "$line" | egrep -i "no outputencoding|nooutputencoding" | grep "<%=" > /dev/null
			if [ $? -eq 0 ];then
				echo "$line" | sed -e "s/<%=\([^%>]*\)%>/<%out.print(\1);\/\/NO OUTPUTENCODING%>/g" >> $tmpfile
			else
				echo "$line" >> $tmpfile
			fi
		done
		mv -f $tmpfile $file
	done
	rm -f $$.*
}

main $*
