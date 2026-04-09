<%@ page import="com.adventnet.iam.IAMProxy" %>
<%@ page import="java.net.URLEncoder" %>

<%
    // Get service name from system property (set in security-properties.xml)
    String serviceName = System.getProperty("service.name");
    if (serviceName == null || serviceName.isEmpty()) {
        serviceName = "Notebook";
    }

    String iamServer = IAMProxy.getIAMServerURL();

    // Service url
    String serviceurl = "https://sri-24601.csez.zohocorpin.com:8443/index.html";

    // IAM signin URL
    String loginUrl = iamServer + "/signin?servicename=" + serviceName;
    loginUrl += "&serviceurl=" + URLEncoder.encode(serviceurl, "UTF-8");

    // Redirect directly to IAM sign-in page
    response.sendRedirect(loginUrl);
%>
