package datamgr;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DataMgr {
  private static Log log = LogFactory.getFactory().getInstance(DataMgr.class);

  private Integer dispatch(List<String> argList) {
    if (argList.size() == 0) {
      return usage();
    } else if ("lscat".equals((String) argList.get(0))) {
      argList.remove(0);
      return lscat(argList);
    } else if ("lsser".equals((String) argList.get(0))) {
      argList.remove(0);
      return lsser(argList);
    } else if ("data".equals((String) argList.get(0))) {
      argList.remove(0);
      return data(argList);
    } else if ("meta".equals((String) argList.get(0))) {
      argList.remove(0);
      return meta(argList);
    } else if ("export".equals((String) argList.get(0))) {
      argList.remove(0);
      return export(argList);
    } else {
      return usage();
    }
  }

  private Integer lscat(List<String> argList) {
    Integer catId = 0;
    try {
      if (argList.size() == 0) {
        catId = 0;
      } else if (argList.size() == 1) {
        catId = Integer.parseInt(argList.get(0));
        if (catId < 0) {
          throw new NumberFormatException();
        }
      } else {
        return usage();
      }
    } catch(NumberFormatException e) {
      log.error("invalid argument: " + argList.get(0));
      return 1;
    }
    InputStream stream = getInputStream("/category/children", "category_id", catId.toString());
    if (stream == null) {
      log.error("cannot open input stream");
      return 1;
    }
    try {
      DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document doc = builder.parse(stream);
      Element root = doc.getDocumentElement();
      if (!root.getNodeName().equals("categories")) {
        log.error("unexpected: root element is not 'categories': " + root.getNodeName());
        return 1;
      }
      NodeList nodeList = root.getChildNodes();
      for (int i = 0; i < nodeList.getLength(); i++) {
        Node node = nodeList.item(i);
        if (node.getNodeType() != Node.ELEMENT_NODE) {
          continue;
        }
        if (!node.getNodeName().equals("category")) {
          log.error("unexpected: child node is not 'category': " + node.getNodeName());
          return 1;
        }
        NamedNodeMap map = node.getAttributes();
        Node id = map.getNamedItem("id");
        if (id == null || id.getNodeType() != Node.ATTRIBUTE_NODE) {
          log.error("unexpected: id attribute not found");
          return 1;
        }
        Node name = map.getNamedItem("name");
        if (name == null || name.getNodeType() != Node.ATTRIBUTE_NODE) {
          log.error("unexpected: id attribute not found");
          return 1;
        }
        String out = String.format("%-5s  %s", id.getNodeValue(), name.getNodeValue());
        System.out.println(out);
      }
    } catch(Exception e) {
      log.error("unable to list categories", e);
      return 1;
    } finally {
      if (stream != null) {
        try {
          stream.close();
        } catch(IOException e) {
          log.warn("cannot close input stream", e);
        }
      }
    }
    return 0;
  }

  private Integer lsser(List<String> argList) {
    return 0;
  }

  private Integer data(List<String> argList) {
    return 0;
  }

  private Integer meta(List<String> argList) {
    return 0;
  }

  private Integer export(List<String> argList) {
    return 0;
  }
  
  private Integer usage() {
    System.out.println("datamgr version 1.00\n");
    System.out.println("usage:\n");
    System.out.println("datamgr lscat [cat-id]");
    System.out.println("  list child categories of category cat-id (default 0)\n");
    System.out.println("datamgr lsser [cat-id]");
    System.out.println("  list child series of category cat-id\n");
    System.out.println("datamgr data series-id");
    System.out.println("  show series data of series series-id\n");
    System.out.println("datamgr meta series-id");
    System.out.println("  show series meta data of series series-id\n");
    System.out.println("datamgr export series-id");
    System.out.println("  export series data of series series-id in quote format\n");
    return 1;
  }
  
  private InputStream getInputStream(String relPath, String requestParamKey, String requestParamValue) {
    InputStream stream = null;
    String baseURL = System.getProperty("datamgr.baseurl");
    if (baseURL == null) {
      log.error("datamgr.baseurl not defined");
      return null;
    }
    String url = baseURL + relPath + "?" + requestParamKey + "=" + requestParamValue + "&api_key=" + System.getProperty("datamgr.apikey");
    log.info("constructed url=" + url);
    try {
      URL myurl = new URL(url);
      HttpsURLConnection con = (HttpsURLConnection) myurl.openConnection();
      con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0");
      stream = con.getInputStream();
    } catch(Exception e) {
      log.error("caught exception", e);
    }
    return stream;
  }
  
  public DataMgr(List<String> args) {
    loadProperties();
    Integer ret = dispatch(args);
    System.exit(ret);
  }
  
  private void loadProperties() {
    ClassLoader cl = ClassLoader.getSystemClassLoader();  
    try {
      InputStream is = cl.getResourceAsStream("datamgr.properties");
      if (is == null) {
        log.error("cannot load properties");
        System.exit(1);
      }
      System.getProperties().load(is);    
    } catch(IOException e) {
      log.error("cannot load properties", e);
      System.exit(1);
    }
  }

  public static void main(String[] args) {
    ArrayList<String> argList = new ArrayList<String>(args.length);
    for (int i = 0; i < args.length; i++) {
      argList.add(args[i]);
    }
    new DataMgr(argList);
  }
}
