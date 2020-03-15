package datamgr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DataMgr {
  private static Log log = LogFactory.getFactory().getInstance(DataMgr.class);

  public DataMgr(List<String> args) {
    loadProperties();

    try {
      log.info("using apikey=" + System.getProperty("datamgr.apikey"));
      String httpsURL = "https://api.stlouisfed.org/fred/category/children?category_id=0&api_key=" + System.getProperty("datamgr.apikey");

      URL myurl = new URL(httpsURL);
      HttpsURLConnection con = (HttpsURLConnection) myurl.openConnection();
      con.setRequestProperty ( "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0" );
      InputStream ins = con.getInputStream();
      InputStreamReader isr = new InputStreamReader(ins);
      BufferedReader in = new BufferedReader(isr);

      DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document doc = builder.parse(ins);
      Element root = doc.getDocumentElement();
      if (!root.getNodeName().equals("categories")) {
        log.error("unexpected: root element is not 'categories'");
        System.exit(1);
      }
      NodeList nodeList = root.getChildNodes();
      for (int i = 0; i < nodeList.getLength(); i++) {
        Node node = nodeList.item(i);
        if (!node.getNodeName().equals("category")) {
          log.error("unexpected: child node is not 'category'");
          System.exit(1);
        }
      }
      System.out.println(doc.getDocumentElement().getNodeName());
      
      System.out.println("name=" + doc.getNodeName());
      NodeList nList = doc.getElementsByTagName("category");
      System.out.println("----------------------------");
      
      for (int temp = 0; temp < nList.getLength(); temp++) {
         Node nNode = nList.item(temp);
         System.out.println("\nCurrent Element :" + nNode.getNodeName()); 
      }
      in.close();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
  
  private void loadProperties() {
    ClassLoader cl = ClassLoader.getSystemClassLoader();  
    try {
      InputStream is = cl.getResourceAsStream("datamgr.properties");
      if (is == null) {
        log.error("panic: cannot load properties");
        System.exit(1);
      }
      System.getProperties().load(is);    
    } catch(IOException e) {
      log.error("panic: cannot load properties", e);
      System.exit(1);
    }
  }

  public static void main(String[] args) {
    ArrayList<String> argList = new ArrayList<String>(args.length);
    for (int i = 0; i < args.length; i++) {
      argList.add(args[i]);
    }
    new DataMgr(argList);
    System.exit(0);
  }
}
