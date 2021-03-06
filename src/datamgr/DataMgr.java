package datamgr;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
  private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
  private static DateFormat dfQuote = new SimpleDateFormat("yyyyMMdd");
  private static String version = "0.96";
  private static int LIMIT = 500;

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
    } else {
      return usage();
    }
  }

  private Integer getcat(Integer catId, String format) {
    InputStream stream = getInputStream("/category", "category_id", catId.toString());
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
      if (nodeList.getLength() == 0) {
        log.error("category not found: " + catId);
        return 1;
      }
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
        if (id == null) {
          log.error("unexpected: id attribute not found");
          return 1;
        }
        Node parentId = map.getNamedItem("parent_id");
        if (parentId == null) {
          log.error("unexpected: parent_id attribute not found");
          return 1;
        }
        Node name = map.getNamedItem("name");
        if (name == null) {
          log.error("unexpected: name attribute not found");
          return 1;
        }
        String out = String.format(format + " (parent = %s)", id.getNodeValue(), name.getNodeValue(), parentId.getNodeValue());
        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.println(out);
        System.out.println("-----------------------------------------------------------------------------------------");
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
  
  private Integer lscat(List<String> argList) {
    Integer catId;
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
      if (getcat(catId, "%-5s  %s") != 0) {
        log.error("unexpected: could not retrieve category: " + catId);
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
          log.error("unexpected: name attribute not found");
          return 1;
        }
        String out = String.format("%-5s  %s", id.getNodeValue(), name.getNodeValue());
        System.out.println(out);
      }
      System.out.println();
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

  private Integer lsserChunk(List<String> result, Integer catId, Integer offset) {
    InputStream stream = getInputStream("/category/series", "category_id", catId.toString(), offset, null);
    if (stream == null) {
      log.error("cannot open input stream");
      return 1;
    }
    try {
      DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document doc = builder.parse(stream);
      Element root = doc.getDocumentElement();
      if (!root.getNodeName().equals("seriess")) {
        log.error("unexpected: root element is not 'seriess': " + root.getNodeName());
        return 1;
      }
      NodeList nodeList = root.getChildNodes();
      for (int i = 0; i < nodeList.getLength(); i++) {
        Node node = nodeList.item(i);
        if (node.getNodeType() != Node.ELEMENT_NODE) {
          continue;
        }
        if (!node.getNodeName().equals("series")) {
          log.error("unexpected: child node is not 'series': " + node.getNodeName());
          return 1;
        }
        NamedNodeMap map = node.getAttributes();
        Node id = map.getNamedItem("id");
        if (id == null || id.getNodeType() != Node.ATTRIBUTE_NODE) {
          log.error("unexpected: id attribute not found");
          return 1;
        }
        Node title = map.getNamedItem("title");
        if (title == null || title.getNodeType() != Node.ATTRIBUTE_NODE) {
          log.error("unexpected: title attribute not found");
          return 1;
        }
        String out = String.format("%-30s  %s", id.getNodeValue(), title.getNodeValue());
        result.add(out);
      }
    } catch(Exception e) {
      log.error("unable to get series chunk", e);
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
    Integer catId = 0;
    try {
      if (argList.size() == 1) {
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
    if (getcat(catId, "%-30s  %s") != 0) {
      log.error("unexpected: could not retrieve category: " + catId);
      return 1;
    }
    List<String> result = new ArrayList<String>();
    List<String> resultChunk = new ArrayList<String>();
    Integer offset = 0;
    while (true) {
      resultChunk.clear();
      Integer ret = lsserChunk(resultChunk, catId, offset);
      if (ret != 0) {
        log.error("could not read series chunk");
        return ret;
      }
      if (resultChunk.size() == 0) {
        break;
      }
      result.addAll(resultChunk);
      offset += LIMIT;
    }
    for(String res: result) {
      System.out.println(res);
    }
    System.out.println();
    return 0;
  }

  private Integer data(List<String> argList) {
    String serId;
    String quoteSymbol = null;
    boolean aggregate = false;
    String units = null;
    while (argList.size() >= 1  && argList.get(0).startsWith("--")) {
      if (argList.get(0).equals("--aggregate-days-to-weeks")) {
        aggregate = true;
      }
      else if (argList.get(0).startsWith("--units=")) {
        units = argList.get(0).substring(8);
      } else {
        return usage();
      }
      argList.remove(0);
    }
    if (argList.size() == 1) {
      serId = argList.get(0);
    } else if (argList.size() == 2) {
      serId = argList.get(0);
      quoteSymbol = argList.get(1);
    } else {
      return usage();
    }
    InputStream stream = getInputStream("/series/observations", "series_id", serId, null, units);
    if (stream == null) {
      log.warn("cannot open input stream, series might not exist");
      return 1;
    }
    try {
      DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document doc = builder.parse(stream);
      Element root = doc.getDocumentElement();
      if (!root.getNodeName().equals("observations")) {
        log.error("unexpected: root element is not 'observations': " + root.getNodeName());
        return 1;
      }
      NodeList nodeList = root.getChildNodes();
      for (int i = 0; i < nodeList.getLength(); i++) {
        Node node = nodeList.item(i);
        if (node.getNodeType() != Node.ELEMENT_NODE) {
          continue;
        }
        if (!node.getNodeName().equals("observation")) {
          log.error("unexpected: child node is not 'observation': " + node.getNodeName());
          return 1;
        }
        NamedNodeMap map = node.getAttributes();
        Node date = map.getNamedItem("date");
        if (date == null || date.getNodeType() != Node.ATTRIBUTE_NODE) {
          log.error("unexpected: date attribute not found");
          return 1;
        }
        Node value = map.getNamedItem("value");
        if (value == null || value.getNodeType() != Node.ATTRIBUTE_NODE) {
          log.error("unexpected: title attribute not found");
          return 1;
        }
        try {
          Double.parseDouble(value.getNodeValue());
          Date d = df.parse(date.getNodeValue());
          Calendar cal = Calendar.getInstance();
          cal.setTime(d);
          if (!aggregate || cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            if (quoteSymbol != null) {
              int year = cal.get(Calendar.YEAR);
              if (year >= 1970) {
                System.out.println(dfQuote.format(d) + ",*," + quoteSymbol + "," + value.getNodeValue());
              } else {
                log.debug("suppressing year prior to 1970: " + year);
              }
            } else {
              System.out.println(date.getNodeValue() + "," + value.getNodeValue());
            }
          }
        } catch(NumberFormatException e) {
          // don't include missing data
        } catch(ParseException e) {
          log.warn("cannot parse date", e);
        }
      }
    } catch(Exception e) {
      log.error("unable to get series data", e);
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

  private Integer meta(List<String> argList) {
    String serId;
    if (argList.size() == 1) {
      serId = argList.get(0);
    } else {
      return usage();
    }
    InputStream stream = getInputStream("/series", "series_id", serId);
    if (stream == null) {
      log.warn("cannot open input stream, series might not exist");
      return 1;
    }
    try {
      DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document doc = builder.parse(stream);
      Element root = doc.getDocumentElement();
      if (!root.getNodeName().equals("seriess")) {
        log.error("unexpected: root element is not 'seriess': " + root.getNodeName());
        return 1;
      }
      NodeList nodeList = root.getChildNodes();
      for (int i = 0; i < nodeList.getLength(); i++) {
        Node node = nodeList.item(i);
        if (node.getNodeType() != Node.ELEMENT_NODE) {
          continue;
        }
        if (!node.getNodeName().equals("series")) {
          log.error("unexpected: child node is not 'series': " + node.getNodeName());
          return 1;
        }
        NamedNodeMap map = node.getAttributes();
        Node id = map.getNamedItem("id");
        if (id == null || id.getNodeType() != Node.ATTRIBUTE_NODE) {
          log.error("unexpected: id attribute not found");
          return 1;
        }
        Node title = map.getNamedItem("title");
        if (title == null || title.getNodeType() != Node.ATTRIBUTE_NODE) {
          log.error("unexpected: title attribute not found");
          return 1;
        }
        Node start = map.getNamedItem("observation_start");
        if (start == null || start.getNodeType() != Node.ATTRIBUTE_NODE) {
          log.error("unexpected: start attribute not found");
          return 1;
        }
        Node end = map.getNamedItem("observation_end");
        if (end == null || end.getNodeType() != Node.ATTRIBUTE_NODE) {
          log.error("unexpected: end attribute not found");
          return 1;
        }
        Node frequency = map.getNamedItem("frequency");
        if (frequency == null || frequency.getNodeType() != Node.ATTRIBUTE_NODE) {
          log.error("unexpected: frequency attribute not found");
          return 1;
        }
        Node units = map.getNamedItem("units");
        if (units == null || units.getNodeType() != Node.ATTRIBUTE_NODE) {
          log.error("unexpected: units attribute not found");
          return 1;
        }
        Node seasonalAdjustment = map.getNamedItem("seasonal_adjustment");
        if (seasonalAdjustment == null || seasonalAdjustment.getNodeType() != Node.ATTRIBUTE_NODE) {
          log.error("unexpected: seasonal_adjustment not found");
          return 1;
        }
        Node notes = map.getNamedItem("notes");
        System.out.println("Id                 : " + id.getNodeValue());
        System.out.println("Title              : " + title.getNodeValue());
        System.out.println("Start              : " + start.getNodeValue());
        System.out.println("End                : " + end.getNodeValue());
        System.out.println("Frequency          : " + frequency.getNodeValue());
        System.out.println("Units              : " + units.getNodeValue());
        System.out.println("Seasonal Adjustment: " + seasonalAdjustment.getNodeValue());
        if (notes != null) {
          System.out.println();
          System.out.println(notes.getNodeValue());
        }
        System.out.println();
      }
    } catch(Exception e) {
      log.error("unable to get series metadata", e);
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

  private Integer usage() {
    System.out.println("datamgr version " + version);
    System.out.println("-------------------------------------------\n");
    System.out.println("usage:\n");
    System.out.println("datamgr lscat [cat-id]");
    System.out.println("  list child categories of cat-id (default 0)\n");
    System.out.println("datamgr lsser cat-id");
    System.out.println("  list child series of cat-id\n");
    System.out.println("datamgr data [--aggregate-days-to-weeks] [--units=<units>] series-id [quote-symbol]");
    System.out.println("        <units> is one of:");
    System.out.println("          lin = Levels (No transformation)");
    System.out.println("          chg = Change");
    System.out.println("          ch1 = Change from Year Ago");
    System.out.println("          pch = Percent Change");
    System.out.println("          pc1 = Percent Change from Year Ago");
    System.out.println("          pca = Compounded Annual Rate of Change");
    System.out.println("          cch = Continuously Compounded Rate of Change");
    System.out.println("          cca = Continuously Compounded Annual Rate of Change");
    System.out.println("          log = Natural Log");
    System.out.println("  show series data of series-id");
    System.out.println("  (if quote-symbol is present, exports in quote format)\n");
    System.out.println("datamgr meta series-id");
    System.out.println("  show series meta data of series-id\n");
    return 1;
  }
  
  private InputStream getInputStream(String relPath, String requestParamKey, String requestParamValue) {
    return getInputStream(relPath, requestParamKey, requestParamValue, null, null);
  }
  
  private InputStream getInputStream(String relPath, String requestParamKey, String requestParamValue, Integer offset, String units) {
    InputStream stream = null;
    String baseURL = System.getProperty("datamgr.baseurl");
    if (baseURL == null) {
      log.error("datamgr.baseurl not defined");
      return null;
    }
    String url = baseURL + relPath + "?" + requestParamKey + "=" + requestParamValue + "&api_key=" + System.getProperty("datamgr.apikey");
    if (offset != null) {
      url += "&limit=" + LIMIT + "&offset=" + offset;
    }
    if (units != null ) {
      url += "&units=" + units;
    }
    log.info("constructed url=" + url);
    try {
      URL myurl = new URL(url);
      HttpsURLConnection con = (HttpsURLConnection) myurl.openConnection();
      con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0");
      stream = con.getInputStream();
    } catch(Exception e) {
      log.info("caught exception", e);
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
