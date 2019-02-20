package com.uwiseone.swp;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class SampleController {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	private static final String AUTH_SERVER = "http://auth.uwiseone.net:8480";
	private static final String API_SERVER = "http://api.uwiseone.net:8580";
	private static final String REDIRECT_URI = "http://my.website.co.kr:9999/callback";
	private static final String CLIENT_ID = "smartrunner-bearworld-app3";
	private static final String CLIENT_SECRET = "smartrunner-bearworld-app-secret";
	
	@GetMapping("/greeting")
	public ResponseEntity<String> greeting() {
		return new ResponseEntity<String>("HelloWorld", HttpStatus.OK);
	}
	
	@GetMapping("/sso")
	public String goSso(HttpServletRequest request) throws Exception {
		log.info("============================[SSO 연결시도]============================");
		String state = UUID.randomUUID().toString();
		request.getSession().setAttribute("oauthState", state);
		
		StringBuilder builder = new StringBuilder();
		builder.append("redirect:");
		builder.append(AUTH_SERVER + "/oauth/authorize");
		builder.append("?response_type=code");
		builder.append("&client_id=" + CLIENT_ID);
		builder.append("&redirect_uri=" + REDIRECT_URI);
		builder.append("&scope=read");
		builder.append("&state=" + state);
		
		return builder.toString();
	}
	
	@GetMapping("/callback")
	public ResponseEntity<String> callback(
			@RequestParam("code") String code,
			@RequestParam(name="state") String state,
			HttpServletRequest request
	) throws Exception {
		ResponseEntity<String> response = null;
		log.info("Authorization code------" + code);
		log.info("state------" + state);

		// state값은 호출 전 사용자 서버에 저장해 두고 리턴퇴는 값과 비교하여 자체 검증처리
		String oauthState = (String)request.getSession().getAttribute("oauthState");
		if(state == null || "".equals(state)) {
			// do something exception
		}
		
		RestTemplate restTemplate = new RestTemplate();

		String credentials = CLIENT_ID + ":" + CLIENT_SECRET;
		String encodedCredentials = new String(Base64.encodeBase64(credentials.getBytes()));
		
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.add("Authorization", "Basic " + encodedCredentials);
		
		HttpEntity<String> httpEntity = new HttpEntity<String>(headers);

		String access_token_url = AUTH_SERVER + "/oauth/token"
								+ "?code=" + code
								+ "&grant_type=authorization_code"
								+ "&redirect_uri=" + REDIRECT_URI;

		response = restTemplate.exchange(access_token_url, HttpMethod.POST, httpEntity, String.class);

		log.info("Access Token Response ---------" + response.getBody());

		// Get the Access Token From the recieved JSON response
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(response.getBody());
		String token = node.path("access_token").asText();

		String url = API_SERVER + "/admin/v1/domains/cloud.uwiseone.net/status";
		
		// Use the access token for authentication
		HttpHeaders headers2 = new HttpHeaders();
		headers2.add("Authorization", "Bearer " + token);
		HttpEntity<String> entity = new HttpEntity<>(headers2);

		ResponseEntity<String> result = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		
		return result;
	}
	
	public static void main(String[] args) throws IOException {
		ResponseEntity<String> response = null;

		RestTemplate restTemplate = new RestTemplate();
		String credentials = "smartrunner-bearworld-app1:smartrunner-bearworld-app-secret";
		String encodedCredentials = new String(Base64.encodeBase64(credentials.getBytes()));
		
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.add("Authorization", "Basic " + encodedCredentials);
		
		HttpEntity<String> httpEntity = new HttpEntity<String>(headers);

		String access_token_url = AUTH_SERVER + "/oauth/token?grant_type=client_credentials";
		response = restTemplate.exchange(access_token_url, HttpMethod.POST, httpEntity, String.class);

		System.out.println("토큰호출 결과:\t" + response.getBody());

		// Get the Access Token From the recieved JSON response
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(response.getBody());
		String token = node.path("access_token").asText();

		
		// 호출된 토큰을 이용하여 도메인 상태 체크
		String url = API_SERVER + "/admin/v1/domains/cloud.uwiseone.net/status";
		
		// Use the access token for authentication
		HttpHeaders headers2 = new HttpHeaders();
		headers2.add("Authorization", "Bearer " + token);
		HttpEntity<String> entity = new HttpEntity<>(headers2);

		ResponseEntity<String> result = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		
		System.out.println("도메인상태 호출 결과:\t" + result.getBody());
	}
}
