package introsde.adapter.ws;

import javax.jws.WebParam;
import javax.jws.WebService;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.xml.bind.DatatypeConverter;
import java.util.List;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import introsde.adapter.ws.Interface;

import introsde.adapter.model.*;

@WebService(endpointInterface = "introsde.adapter.ws.Interface",serviceName="AdapterService")
public class Implementation implements Interface{
	
	final static private String APP_KEY="88b487231aca46a799ef013f4351ae13";
	final static private String APP_URL="http://platform.fatsecret.com/rest/server.api";
	final static private String APP_SECRET = "59ef712fa4384fda92f260410660e9e4";
	final static private String HMAC_SHA1_ALGORITHM = "HmacSHA1";
	WebTarget service;
	ObjectMapper mapper = new ObjectMapper();
	ClientConfig clientConfig = new ClientConfig();
	Client client = ClientBuilder.newClient(clientConfig);
	String[] template = new String[1];
	JsonNode node;
	
	
	@Override
	public Food getFood(int food_id) {
		List<String> params = new ArrayList<>(Arrays.asList(generateOauthParams()));
		
        params.add("method=food.get");
        params.add("food_id="+food_id);
        params.add("oauth_signature=" + sign("GET", params.toArray(template)));
        
		
		service = client.target(APP_URL +"?" + paramify(params.toArray(template)));
		Response resp = service.request().get();
	    String json = resp.readEntity(String.class);
	    
	    Food f = new Food();
	    try {
			node = mapper.readTree(json);
			f.setFood_id(node.path("food").path("food_id").asInt());
			f.setFood_name(node.path("food").path("food_name").asText());
			f.setFood_type(node.path("food").path("food_type").asText());
			f.setFood_url(node.path("food").path("food_url").asText());
			f.setFood_description(node.path("food").path("food_description").asText());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return f;
	    
	}
	
	@Override
	public List<Food> searchFood(String s) {
		List<String> params = new ArrayList<>(Arrays.asList(generateOauthParams()));

        params.add("method=foods.search");
		params.add("search_expression="+ s.replaceAll(" ", "%20"));
        params.add("oauth_signature=" + sign("GET", params.toArray(template)));
        
	
		service = client.target(APP_URL +"?" + paramify(params.toArray(template)));
		Response resp = service.request().get();
	    String json = resp.readEntity(String.class);

	    List<Food> foods = new ArrayList<>();
	    try {
			node = mapper.readTree(json);
			JsonNode foodsNode = node.path("foods").path("food");
			for (JsonNode n : foodsNode) {
				Food f = new Food();
				f.setFood_id(n.path("food_id").asInt());
				f.setFood_name(n.path("food_name").asText());
				f.setFood_type(n.path("food_type").asText());
				f.setFood_url(n.path("food_url").asText());
				f.setFood_description(n.path("food_description").asText());
				foods.add(f);
			}
			
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return foods;
	}
	
	// <--------- EXERCISE
	
	@Override
	public List<Exercise> getExercises() {
		List<String> params = new ArrayList<>(Arrays.asList(generateOauthParams()));

        params.add("method=exercises.get");
        params.add("oauth_signature=" + sign("GET", params.toArray(template)));
        
	
		service = client.target(APP_URL +"?" + paramify(params.toArray(template)));
		Response resp = service.request().get();
	    String json = resp.readEntity(String.class);
	    
	    List<Exercise> exercises = new ArrayList<>();
	    try {
			node = mapper.readTree(json);
			JsonNode exercisesNode = node.path("exercises").path("exercise");
			for (JsonNode n : exercisesNode) {
				Exercise e = new Exercise();
				e.setId(n.path("exercise_id").asInt());
				e.setName(n.path("exercise_name").asText());
				exercises.add(e);
			}
			
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return exercises;
	}
	
	private static String[] generateOauthParams(){
		return new String[]{
				"oauth_consumer_key=" + APP_KEY,
				"oauth_signature_method=HMAC-SHA1",
				"oauth_version=1.0",
				"oauth_nonce=" + oauth_nonce(),
				"oauth_timestamp=" + Long.valueOf(System.currentTimeMillis()*2).toString(),
				"format=json"};
	}
	
	// Generate a unique random string (Timestamp combined with random string)
	private static String oauth_nonce(){
		Random r = new Random(System.currentTimeMillis());
        StringBuilder n = new StringBuilder();
        for (int i = 0; i < r.nextInt(8) + 2; i++)
            n.append(r.nextInt(26) + 'a');
		return n.toString();
	}
	
	
	
	private static String sign(String method, String[] params){
		String s;
		try {
			s = method + "&" + URLEncoder.encode(APP_URL, "ISO-8859-1") + "&" + URLEncoder.encode(paramify(params), "ISO-8859-1");
			SecretKey sk = new SecretKeySpec((APP_SECRET+"&").getBytes(), HMAC_SHA1_ALGORITHM);
			Mac m = Mac.getInstance(HMAC_SHA1_ALGORITHM);
			m.init(sk);
	        return URLEncoder.encode(new String(DatatypeConverter.printBase64Binary(m.doFinal(s.getBytes()))).trim(), "ISO-8859-1");
		} catch (UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeyException e) {
			System.err.println("FatSecret_TEST FAIL error:" + e.getMessage());
            return null;
		}
        
    }
	
	private static String sign(String method, String[] params, String auth_secret){
		String s;
		try {
			s = method + "&" + URLEncoder.encode(APP_URL, "ISO-8859-1") + "&" + URLEncoder.encode(paramify(params), "ISO-8859-1");
			SecretKey sk = new SecretKeySpec((APP_SECRET+"&"+auth_secret).getBytes(), HMAC_SHA1_ALGORITHM);
			Mac m = Mac.getInstance(HMAC_SHA1_ALGORITHM);
			m.init(sk);
	        return URLEncoder.encode(new String(DatatypeConverter.printBase64Binary(m.doFinal(s.getBytes()))).trim(), "ISO-8859-1");
		} catch (UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeyException e) {
			System.err.println("FatSecret_TEST FAIL error:" + e.getMessage());
            return null;
		}
        
    }
	
    private static String paramify(String[] params) {
        String[] p = Arrays.copyOf(params, params.length);
        Arrays.sort(p);
        String result = p[0];
        for (int i = 1; i < p.length; i++) {
            result += "&"+p[i];
        }
        return result;
    }

}
