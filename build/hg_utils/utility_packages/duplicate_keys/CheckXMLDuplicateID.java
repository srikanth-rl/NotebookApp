import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;

/**
 * Created by girish-0568 on 29/5/14.
 */
public class CheckXMLDuplicateID {
    private static String folder = "";

    public static void main(String[] args) {
        if (args.length == 3) {
            folder = args[0];
            processFiles(readFileAsHash(args[1]), readFileAsHash(args[2]));
        } else {
            System.out.println("Invalid/No input specified.. Kindly give the arguments in the following format <product_package/conf path> <split-xml.conf> <xml-id.conf>");
            System.exit(1);
        }
    }

    private static void processFiles(Hashtable<String, String> filesList, Hashtable<String, String> tableDetails) {
        ArrayList<String> parentList = new ArrayList<String>(filesList.keySet());
        for (int a = 0; a < parentList.size(); a++) {
            ArrayList<String> patterns = new ArrayList<String>();
            String file = parentList.get(a);
            String[] files = filesList.get(file).split(",");
            String[] tableColumns = tableDetails.get(file).split(",");
            for (int d = 0; d < tableColumns.length; d++) {
                String[] tableColumnName = tableColumns[d].split("\\.");
                String tableName = tableColumnName[0];
                String columnName = tableColumnName[1];
                for (int s = 0; s < files.length; s++) {
                    String fileName = folder + files[s] + ".xml";
                    getIDValues(fileName, tableName, columnName, patterns);
                }
                boolean hasDuplicates = checkDuplicates(patterns, filesList, file, tableName, columnName);
                if (hasDuplicates) {
                    System.exit(-1);
                }
                patterns = new ArrayList<String>();
            }
        }
        System.out.println("Validation of split XML completed successfully..");
        System.exit(0);
    }

    private static boolean checkDuplicates(ArrayList<String> patterns, Hashtable<String, String> filesList, String file, String tableName, String columnName) {
        HashSet set = new HashSet(patterns);
        if (set.size() == patterns.size()) {
            return false;
        } else {
            ArrayList list = new ArrayList(set);
            for (int a = 0; a < list.size(); a++) {
                patterns.remove(list.get(a));
            }
            System.out.println("One of the listed files contains duplicate ID \"" + patterns + "\" for the table \"" + tableName + "\" and column \"" + columnName + "\". Kindly modify the values and try again..");
            System.out.println(filesList.get(file));
            return true;
        }
    }

    private static ArrayList<String> getIDValues(String fileName, String tableName, String columnName, ArrayList<String> patterns) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(new File(fileName));
            NodeList nodeList = document.getElementsByTagName(tableName);
            for (int x = 0, size = nodeList.getLength(); x < size; x++) {
                patterns.add(nodeList.item(x).getAttributes().getNamedItem(columnName).getNodeValue());
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return patterns;
    }

    private static Hashtable<String, String> readFileAsHash(String confFileName) {
        Hashtable<String, String> table = new Hashtable<String, String>();
        try {
            Properties props = new Properties();
            FileInputStream fin = new FileInputStream(confFileName);
            props.load(fin);
            for (String name : props.stringPropertyNames()) {
                table.put(name, props.getProperty(name));
            }
            fin.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return table;
    }
}
