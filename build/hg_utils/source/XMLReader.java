import java.io.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLReader
{
	public static void main(String args[]) throws ParserConfigurationException, IOException, org.xml.sax.SAXException
	{
		try
		{
			String file = args [0];
			File inputFile = new File(file);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			db.setEntityResolver(new EntityResolver()
					{
						public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException
			{
				return new InputSource(new StringReader(""));
			}
			});
			Document doc = db.parse(inputFile);
			doc.getDocumentElement().normalize();
			NodeList nodeLst = doc.getElementsByTagName("error-page");
			String errcode = "";
			String expcode = "";
			for (int s = 0; s < nodeLst.getLength(); s++)
			{
				Node fstNode = nodeLst.item(s);
				if (fstNode.getNodeType() == Node.ELEMENT_NODE)
				{
					Element fstElmnt = (Element) fstNode;
					NodeList fstElmntLst = fstElmnt.getElementsByTagName("error-code");
					if ( fstElmntLst.getLength() != 0)
					{
						Node fstElmntNode = fstElmntLst.item(0);
						errcode=errcode+ " " + fstElmntNode.getTextContent();
					}
					NodeList lstElmntLst = fstElmnt.getElementsByTagName("exception-type");
					if ( lstElmntLst.getLength() != 0)
					{
						Node lstElmntNode = lstElmntLst.item(0);
						expcode=expcode + " "+ lstElmntNode.getTextContent();
					}
				}
			}
			NodeList fmnodeLst = doc.getElementsByTagName("filter-mapping");
			String SECURITY_MAP = "FALSE";
			String ACCESS_LOG_MAP = "FALSE";
			String INSTRUMENTATION_MAP = "FALSE";
			String SERVER_HEALTH_CHECK_MAP = "FALSE";
			String CRM_PLUS_MAP = "NOT_CONFIGURED";
			if (fmnodeLst.getLength() > 0)
			{
				String fName1 = "SecurityFilter";
				String fName2 = "Security Filter";
				String accesslog = "AccessLogFilter";
				String instrumentation = "Instrumentation";
				String serverhealthcheck = "ServerHealthCheckFilter";
				String crmplus = "CrmPlusFilter";
				Node af_fstNode = fmnodeLst.item(0);
				if (af_fstNode.getNodeType() == Node.ELEMENT_NODE)
				{
					Element fstElmnt = (Element) af_fstNode;
					NodeList fstElmntLst = fstElmnt.getElementsByTagName("filter-name");
					if ( fstElmntLst.getLength() != 0)
					{
						Node fstElmntNode = fstElmntLst.item(0);
						String fm_map = fstElmntNode.getTextContent();
						if(fm_map.equals(instrumentation))
						{
							INSTRUMENTATION_MAP="TRUE";
						}
					}
				}
				if (fmnodeLst.getLength() > 1)
				{
					Node fstNode = fmnodeLst.item(1);
					if (fstNode.getNodeType() == Node.ELEMENT_NODE)
					{
						Element fstElmnt = (Element) fstNode;
						NodeList fstElmntLst = fstElmnt.getElementsByTagName("filter-name");
						if ( fstElmntLst.getLength() != 0)
						{
							Node fstElmntNode = fstElmntLst.item(0);
							String fm_map = fstElmntNode.getTextContent();
							if(fm_map.equals(accesslog))
							{
								ACCESS_LOG_MAP="TRUE";
							}
						}
					}
				}
				if (fmnodeLst.getLength() > 2)
				{
					Node fstNode = fmnodeLst.item(2);
					if (fstNode.getNodeType() == Node.ELEMENT_NODE)
					{
						Element fstElmnt = (Element) fstNode;
						NodeList fstElmntLst = fstElmnt.getElementsByTagName("filter-name");
						if ( fstElmntLst.getLength() != 0)
						{
							Node fstElmntNode = fstElmntLst.item(0);
							String fm_map = fstElmntNode.getTextContent();
							if(fm_map.equals(crmplus))
							{
								CRM_PLUS_MAP="TRUE";
							}
							else if ((fm_map.equals(fName1)) || (fm_map.equals(fName2)))
							{
								SECURITY_MAP="TRUE";
							}
						}
					}
				}
				if (fmnodeLst.getLength() > 3)
				{
					Node fstNode = fmnodeLst.item(3);
					if (fstNode.getNodeType() == Node.ELEMENT_NODE)
					{
						Element fstElmnt = (Element) fstNode;
						NodeList fstElmntLst = fstElmnt.getElementsByTagName("filter-name");
						if ( fstElmntLst.getLength() != 0)
						{
							Node fstElmntNode = fstElmntLst.item(0);
							String fm_map = fstElmntNode.getTextContent();
							if(fm_map.equals(serverhealthcheck))
							{
								SERVER_HEALTH_CHECK_MAP="TRUE";
							}
							else if(CRM_PLUS_MAP == "TRUE" && ((fm_map.equals(fName1)) || (fm_map.equals(fName2))))
							{
								SECURITY_MAP="TRUE";
							}
						}
					}
				}
				if (fmnodeLst.getLength() > 4)
				{
					Node fstNode = fmnodeLst.item(4);
					if (fstNode.getNodeType() == Node.ELEMENT_NODE)
					{
						Element fstElmnt = (Element) fstNode;
						NodeList fstElmntLst = fstElmnt.getElementsByTagName("filter-name");
						if ( fstElmntLst.getLength() != 0)
						{
							Node fstElmntNode = fstElmntLst.item(0);
							String fm_map = fstElmntNode.getTextContent();
							if(CRM_PLUS_MAP == "TRUE" && SECURITY_MAP == "TRUE" && fm_map.equals(serverhealthcheck))
							{
								SERVER_HEALTH_CHECK_MAP="TRUE";
							}
						}
					}
				}
			}
			else
			{
				ACCESS_LOG_MAP="NOT_CONFIGURED";
				SECURITY_MAP="NOT_CONFIGURED";
				INSTRUMENTATION_MAP="NOT_CONFIGURED";
				SERVER_HEALTH_CHECK_MAP="NOT_CONFIGURED";
				CRM_PLUS_MAP="NOT_CONFIGURED";
			}
			System.out.println("ERR_CODES=" + errcode.trim() + " "+ expcode.trim());
			System.out.println("INSTRUMENTATION_FM_VALUE=" + INSTRUMENTATION_MAP );
			System.out.println("ACCESS_LOG_FM_VALUE=" +  ACCESS_LOG_MAP);
			System.out.println("SECURITY_FM_VALUE=" + SECURITY_MAP );
			System.out.println("SERVER_HEALTH_CHECK_FM_VALUE=" + SERVER_HEALTH_CHECK_MAP );
			System.out.println("CRM_PLUS_FM_VALUE=" + CRM_PLUS_MAP );
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
