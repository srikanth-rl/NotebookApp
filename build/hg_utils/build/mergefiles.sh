main()
{
        [ $2 ] || { echo " Usage: <BUILD_DIR> ; <PROD_HOME(directory path, before starting path of js(or)css folder)>"; exit 1;}

	SET_VARIABLES $*
	CATJS_FILES
	CATCSS_FILES
}

SET_VARIABLES()
{
	BUILD_HOME=$1
	PROD_HOME=$2
	BUILD_DIR=`pwd`
	JAVA_HOME=$(dirname $(dirname $(which java)))
	#JAR=${BUILD_HOME}/thirdparty_packages/custom_rhino/custom_rhino.jar
	COMP_JS=${BUILD_HOME}/comp.js
	COMP_CSS=${BUILD_HOME}/comp.css
}
CATJS_FILES()
{
cd ${BUILD_DIR}
for i in `cat mergefiles/jslist.conf | grep -v "#"`
do
	INPUT_LIST=`echo $i | cut -d ":" -f1`
	OUTPUT_JS=`echo $i | cut -d ":" -f2`
        OUTPUT_JSDIR=`echo $i | cut -d ":" -f3`
	cd ${PROD_HOME}
	touch ${BUILD_DIR}/combine.js
	cat `cat ${BUILD_DIR}/$INPUT_LIST | grep -v "#"` >> ${BUILD_DIR}/combine.js
	#${JAVA_HOME}/bin/java -jar $JAR -opt -1 -c  ${BUILD_DIR}/combine.js > ${COMP_JS}
	uglifyjs -nc -o ${COMP_JS} ${BUILD_DIR}/combine.js
	if [ $? != 0 ]
	then
		echo "Error occurred in js compression ...."
		exit
	fi
	cat ${COMP_JS} >> ${PROD_HOME}/${OUTPUT_JSDIR}/${OUTPUT_JS}
	rm -rf ${COMP_JS}
	rm -rf ${BUILD_DIR}/combine.js
done
}

CATCSS_FILES()
{
cd ${BUILD_DIR}
for i in `cat mergefiles/csslist.conf | grep -v "#"`
do
     INPUT_CSS=`echo $i | cut -d ":" -f1`
     OUTPUT_CSS=`echo $i | cut -d ":" -f2`
     OUTPUT_CSSDIR=`echo $i | cut -d ":" -f3`
     cd ${PROD_HOME}
     cat `cat ${BUILD_DIR}/$INPUT_CSS | grep -v "#"` > ${COMP_CSS}
     perl -w -e "s/\/\*.*\*\///g" -p -i ${COMP_CSS}
     perl -w -e "s/\n//g" -p -i ${COMP_CSS}
     cat ${COMP_CSS} >> ${PROD_HOME}/${OUTPUT_CSSDIR}/${OUTPUT_CSS}
     rm -rf ${COMP_CSS}
done
}

main $*
