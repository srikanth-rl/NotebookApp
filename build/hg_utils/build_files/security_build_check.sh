main()
{
	[ $3 ] || { echo "Usage : $0 <Check Name : 0- CodeCheck | 1- FilterCheck | 2- Both Check ><XML Reader Jar> <PROD_HOME/webapps>" ; exit 1 ; }
	WORK_DIR=`pwd`
	CHECKNAME=${1}
	JAR=${2}
	DIRECTORYS=${3}
	CONF_PATH=${5}
	export CLASSPATH=${JAR}
	ADDITIONAL_FILTER_PRODUCTS="TESTINTEG"
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
				echo "${web}" | grep -wv "grid" | grep -w "WEB-INF/web.xml" > /dev/null 2>&1
				if [ $? -eq 0 ]
				then
					SecurityCheck
				fi
			done
		else
			echo "${dir} is not directory ..."
			exit 1
		fi
		doServerXmlOrigCheck
	done
}
doServerXmlOrigCheck()
{
	if [ -z "${CONF_PATH}" ]
	then
		ServerXmlOrigFileCheckForAdditionalPath ${dir}
	else
		ServerXmlOrigFileCheckForAdditionalPath ${CONF_PATH}
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
	UTIL_XSL=${WORK_DIR}/server_orig.xsl
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
		echo "While Executing java class problem exist - Please refer :"
		echo "------------------------------------------------------------------"
		cat ${XML_OUTPUT}
		exit 1
	else
		errCode=`cat ${XML_OUTPUT} | grep ERR_CODES | cut -d "=" -f2`
		accesslog_filterMap=`cat ${XML_OUTPUT} | grep ACCESS_LOG_FM_VALUE | cut -d "=" -f2`
		sec_filterMap=`cat ${XML_OUTPUT} | grep SECURITY_FM_VALUE | cut -d "=" -f2`
		instrumentation_filterMap=`cat ${XML_OUTPUT} | grep INSTRUMENTATION_FM_VALUE | cut -d "=" -f2`
		serverhealthcheck_filterMap=`cat ${XML_OUTPUT} | grep SERVER_HEALTH_CHECK_FM_VALUE | cut -d "=" -f2`
		crmplus_filterMap=`cat ${XML_OUTPUT} | grep CRM_PLUS_FM_VALUE | cut -d "=" -f2`
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
				instrumentationCheck
				accessLogFilterCheck
				securityFilterCheck
				serverHealthCheckFilterCheck
				crmPlusFilterCheck
				;;
			* )
				echo "Security Code & Filter Check"
				securityCodeCheck
				instrumentationCheck
				accessLogFilterCheck
				securityFilterCheck
				serverHealthCheckFilterCheck
				crmPlusFilterCheck
				;;
		esac
		cat ${WORK_DIR}/xmloutval.txt | grep -w "FAILED"
		if [ $? -eq 0 ]
		then
			printFilterError
			exit 1
		fi
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
accessLogFilterCheck()
{
	if [ "${accesslog_filterMap}" == "TRUE" ]
	then
		echo "*** accesslog filter check is success ***"
	else
		echo "AccessLogFilter=FAILED" >> ${WORK_DIR}/xmloutval.txt
	fi
}
securityFilterCheck()
{
	if [ "${sec_filterMap}" == "TRUE" ]
	then
		echo "*** security filter check is success ***"
	else
		echo "SecurityFilter=FAILED" >> ${WORK_DIR}/xmloutval.txt
	fi
}
instrumentationCheck()
{
	if [ "${instrumentation_filterMap}" == "TRUE" ]
	then
		echo "*** instrumentation check is success ***"
	else
		echo "Instrumentation=FAILED" >> ${WORK_DIR}/xmloutval.txt
	fi
}
serverHealthCheckFilterCheck()
{
	if [ "${serverhealthcheck_filterMap}" == "TRUE" ]
	then
		echo "*** serverhealthcheck filter check is success ***"
	else
		echo "ServerHealthCheckFilter=FAILED" >> ${WORK_DIR}/xmloutval.txt
	fi
}
crmPlusFilterCheck()
{
	if [ "${crmplus_filterMap}" == "TRUE" -o "${crmplus_filterMap}" == "NOT_CONFIGURED" ]
	then
		echo "*** crmplus filter check is success ***"
	else
		echo "CrmPlusFilter=FAILED" >> ${WORK_DIR}/xmloutval.txt
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

	Please refer : https://security.wiki.zoho.com/How-To.html#Exception_and_Error_Handling

	Please contact Security-team for further details
	_______________________________________________________________________________
EOF
}
printFilterError()
{
	if [ "${crmplus_filterMap}" == "TRUE" ]
	then
		FILTER_ORDER="Instrumentation, AccessLogFilter, CrmPlusFilter, SecurityFilter, ServerHealthCheckFilter"
	else
		FILTER_ORDER="Instrumentation, AccessLogFilter, SecurityFilter, ServerHealthCheckFilter"
	fi
	cat << EOF

	Caution : Build Stopped due to Filter Exception
	-----------------------------------------------

	Either one of the filters are missing or filter order is invalid in ${req_filename}

	Order of filters : ${FILTER_ORDER}

	Please refer further details : http://framework.zorrowiki.zohonoc.com/SAS/SaSLiteM20-Filter-Ordering.html

	Contact sas-team for further details
	_______________________________________________________________________________
EOF
}
main $*
