package datamgr;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

public class DataMgr {
  private static Log log = LogFactory.getFactory().getInstance(DataMgr.class);

    public static void main(String[] args) {
      try {
        String httpsURL = "https://api.stlouisfed.org/fred/category/children?category_id=0&api_key=24cb6f4b1f53b610140642e733526507";

        URL myurl = new URL(httpsURL);
        HttpsURLConnection con = (HttpsURLConnection) myurl.openConnection();
        con.setRequestProperty ( "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0" );
        InputStream ins = con.getInputStream();
        InputStreamReader isr = new InputStreamReader(ins);
        BufferedReader in = new BufferedReader(isr);
        String inputLine;

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(ins);
        System.out.println(doc.getDocumentElement().getNodeName());
        /*
        while ((inputLine = in.readLine()) != null) {
            System.out.println(inputLine);
        }
        */
        in.close();
  } catch(Exception e) {
    e.printStackTrace();
  }
    }
}
