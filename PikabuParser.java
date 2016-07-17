package pikabu_parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

public class PikabuParser {
	CloseableHttpClient httpclient;

	public PikabuParser() {		
		
		BasicCookieStore cookieStore = new BasicCookieStore();
		RequestConfig globalConfig = RequestConfig.custom()
		        .setCookieSpec(CookieSpecs.STANDARD)
		        .build();		
		ArrayList<Header> headers = new ArrayList<Header>();
		headers.add(new BasicHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.87 Safari/537.36 OPR/37.0.2178.32"));
		headers.add(new BasicHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"));
		httpclient = HttpClients.custom()
				.setDefaultRequestConfig(globalConfig)
				.setDefaultCookieStore(cookieStore)
				.setDefaultHeaders(headers )
				.build();
				
        try {    
            HttpGet httpGet = new HttpGet("http://pikabu.ru/new");            
            CloseableHttpResponse response1 = httpclient.execute(httpGet); 
            try {
            	String page =  getResponse(response1.getEntity());  
            	String Csrf_token = page.substring(page.indexOf("sessionID")+12, page.indexOf("sessionID")+44); 
            	headers.add(new BasicHeader("X-Csrf-Token", Csrf_token));	
                EntityUtils.consume(response1.getEntity());
            } finally {
                response1.close();
            }  
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String get_page (String tab, int page_num) {		//tab = hot | best | new
		String page = "";
		try {    
            HttpGet httpGet = new HttpGet("http://pikabu.ru/best?twitmode=1&page=" + page_num);            
            CloseableHttpResponse response1 = httpclient.execute(httpGet); 
            try {
            	page = getResponse(response1.getEntity());            	
                EntityUtils.consume(response1.getEntity());
            } finally {
                response1.close();
            }            
		} catch (IOException e) {
			e.printStackTrace();
		}	
		return page;
	}
	
	public ArrayList<PikabuTextPost> get_text_posts(String tab, int page_num){
		ArrayList<PikabuTextPost> post_list = new ArrayList<PikabuTextPost>();
		String html_content = "";
		try {
			JSONObject job = new JSONObject(get_page(tab, page_num));
			html_content = job.getString("html");
		} catch (JSONException e) {
			e.printStackTrace();
		}
				
		Document doc = new Document("");	
		doc.append(html_content);
		Elements elements = doc.getElementsByAttributeValue("class", "story");
		Element element = null;
		for(int i = 0; i < elements.size()-1; i++){
			element = elements.get(i);			
			if(element.getElementsByAttribute("data-story-type").first().attr("data-story-type").equals("text")){
				PikabuTextPost post = new PikabuTextPost();
				post.id = Integer.parseInt( element.attr("data-story-id") );
				post.rating = Integer.parseInt( element.getElementsByAttributeValue("class", "story__rating-block").first().text() );
				post.URL = element.getElementsByAttributeValue("class", "story__header-title").first().getElementsByAttribute("href").first().attr("href");
				post.title =  element.getElementsByAttributeValue("class", "story__header-title").first().getElementsByAttribute("href").first().text();
				post.content = element.getElementsByAttributeValue("class", "story__wrapper").text();
				post.content = post.content.substring(0, post.content.length()-9);	//del свернуть in all posts
				if(post.content.length() >= 18 && post.content.substring(post.content.length()-18, post.content.length()).equals("Показать полностью")){
					post.content = post.content.substring(0, post.content.length()-18);	//del Показать полностью
				}
				post.author = element.getElementsByAttributeValue("class", "story__author").first().text();
				Elements tags = element.getElementsByAttributeValue("class", "story__tags").first().getElementsByAttribute("href");
				for(Element tag_el: tags){
					post.tags.add(tag_el.text());
				}
				post_list.add(post);
			}
		}		
		return post_list;
	}
	
	private String getResponse(HttpEntity entity){
        BufferedReader bufReader;
        String line = "";
	    StringBuilder stringBuilder = new StringBuilder();
		try {
			bufReader = new BufferedReader(new InputStreamReader(entity.getContent(), "UTF-8"));		
	        
			while ((line = bufReader.readLine()) != null){
		    	stringBuilder.append(line).append("\n");
		    }
		    bufReader.close();
		} catch (UnsupportedOperationException | IOException e) {
			e.printStackTrace();
		}
		return stringBuilder.toString();		
	}

}

class PikabuTextPost {
	public int id = -1;
	public String URL = "";
	public String title = "";
	public String content = "";
	public String author = "";
	public int rating = 0;
	public ArrayList<String> tags = new ArrayList<String>();
}
