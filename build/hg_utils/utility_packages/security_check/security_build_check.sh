main ()
{
  [ $3 ] || { echo "Usage : $0 <Check Name : 0- CodeCheck | 1- FilterCheck | 2- Both Check ><XML Reader Jar> <PROD_HOME/webapps>" ; exit 1 ; }
	WORK_DIR=`pwd`
	CHECKNAME=${1}
	JAR=${2}
	DIRECTORYS=${3}
	CONF_PATH=${4}
	export CLASSPATH=${JAR}
	CheckFileExistence
}
CheckFileExistence()
{
	if [ ! -f ${JAR} ]
	then
		echo "${JAR} is not present"
	        exit 1
	fi

	for dir in `echo ${DIRECTORYS} | tr "," " "`
	do
		if [ -d ${dir} ]
		then
			for web in `find ${dir} -iname "web.xml" | tr "\n" " "`
			do
					echo "${web}" | grep -w "WEB-INF/web.xml" > /dev/null 2>&1
					if [ $? -eq 0 ]
					then
						SecurityCheck
					fi
			done

		else
			echo "${dir} is not directory ..."
			exit 1
		fi
		PRD_HOME=`dirname ${dir}`
		CheckSasliteVersion
	done
}
CheckSasliteVersion()
{
	SASLITE_VERSIONS="SASLITE_M17_5 SASLITE_M17_6_1"
	SASLITE_VERSION_SEARCH=`cat ${PRD_HOME}/blog/saslite.txt | grep "^BuildLabel="`
	if [ -n "${SASLITE_VERSION_SEARCH}" ]
	then
		SASLITE_VERSION=`echo ${SASLITE_VERSION_SEARCH} | cut -d "=" -f2`
		echo ${SASLITE_VERSIONS} | grep -w "${SASLITE_VERSION}" > /dev/null
		if [ $? -ne 0 ]
		then
			if [ -z "${CONF_PATH}" ]
			then
				ServerXmlOrigFileCheckForAdditionalPath ${PRD_HOME}
			else
				ServerXmlOrigFileCheckForAdditionalPath ${CONF_PATH}
			fi
		fi
	else
		echo "${PRD_HOME}/blog/saslite.txt is not available ..."
	fi
}
ServerXmlOrigFileCheckForAdditionalPath()
{
	SERVER_XML_PARENT_DIR=$1
	if [ -d "${SERVER_XML_PARENT_DIR}" ]
	then
		for server_xml_orig in `find ${SERVER_XML_PARENT_DIR} -iname "server.xml.orig"`
		do
			echo "${server_xml_orig}" | grep -w "conf/server.xml.orig" > /dev/null 2>&1
			if [ $? -eq 0 ]
			then
				ServerXmlOrigFileCheck
			fi
		done
	else
		echo "${SERVER_XML_PARENT_DIR} is not a directory ..."
		exit 1
	fi
}
ServerXmlOrigFileCheck()
{
	UTIL_XSL=`dirname $0`/server_orig.xsl
	SERVER_XML_OUTPUT=`xsltproc --novalid ${UTIL_XSL} ${server_xml_orig}`
	SERVER_XML_MSG=`echo ${SERVER_XML_OUTPUT} | sed 's@<?xml.*?> @@g'`
	if [ -n "${SERVER_XML_MSG}" ]
	then
		echo "${SERVER_XML_MSG}"
		exit 1
	fi
}
SecurityCheck()
{
	XML_OUTPUT=${WORK_DIR}/xml.txt
	${JAVA_HOME}/bin/java XMLReader ${web} > ${XML_OUTPUT}
	if [ "$?" -gt 0 ]
	then
		echo "While Executing XMLReader java class problem exist - Please refer :"
		echo "------------------------------------------------------------------"
		cat ${XML_OUTPUT}
		exit 1
	else
		errCode=`cat ${XML_OUTPUT} | grep ERR_CODES | cut -d "=" -f2`
		filterMap=`cat ${XML_OUTPUT} | grep FM_VALUE | cut -d "=" -f2`
		fname=`basename ${web}`
		dname=`dirname ${web}`
		if [ -d ${dname}/CVS ]
		then
			dirname=`cat ${dname}/CVS/Repository`
		else
			dirname=$dname
		fi
		export req_filename=${dirname}/${fname}
		export ref_filename="<zohoservices>/webapps/<module>/WEB-INF/web.xml"

		case ${CHECKNAME} in
			"0" )
			echo "Only Security Code Check"
			securityCodeCheck
			;;

			"1" )
			echo "Only Security Filter Check"
		        securityFilterCheck	
			;;
			* )
			echo "Security Code & Filter Check"
			securityCodeCheck
			securityFilterCheck
			;;
		esac

	fi
}
securityCodeCheck()
{
	ERR_CODES="400 403 404 408 413 414 500 505 java.lang.Throwable"
	if [ ! -z "${errCode}" ]
	then
		for slist in ${ERR_CODES}
		do
			echo "${errCode}" | grep -w "${slist}" > /dev/null 2>&1
			if [ $? -ne 0 ]
			then
				FLAGS=${FLAGS}:${slist}
			fi
		done
	REQ_FLAGS=`echo ${FLAGS} | sed -e "s/^://g"`
	else
		REQ_FLAGS=${ERR_CODES}
	fi

	if [ ! -z "${REQ_FLAGS}" ]
	then
		printSecurityCodeError
		exit 1
	else
		echo "*** security code check is success ***"
	fi
}
securityFilterCheck()
{
	if [ "${filterMap}" == "TRUE" ]
	then
		echo "*** security filter check is success ***"
	else
		printSecurityFilterError
		exit 1
	fi


}
printSecurityCodeError()
{
	cat << EOF

	Caution : File not checked in due to Security Code Exception
	-----------------------------

	${req_filename} file does contains the following error-codes : ${REQ_FLAGS}

	FYI : The following error-page are mandatory for ${req_filename} (Ex : ${ref_filename}).

	error-code : 400 403 404 408 413 414 500 505

	exception-type : java.lang.Throwable

	Please refer further details : https://security.wiki.zoho.com/How-To.html#Exception_and_Error_Handling

	Please contact Security-team for further details :
	_______________________________________________________________________________

EOF

}


printSecurityFilterError()
{
	cat << EOF

	Caution : Build Stopped due to Security Filter Exception
	-----------------------------

	Please add the Security Filter mapping entry at the top in ${req_filename} (after <web-app ..> tag)

	Please refer further details : https://security.wiki.zoho.com/How-To.html#Security_Filter_Configuration 

	Contact Security-team for further details
	_______________________________________________________________________________

EOF

}

main $*
