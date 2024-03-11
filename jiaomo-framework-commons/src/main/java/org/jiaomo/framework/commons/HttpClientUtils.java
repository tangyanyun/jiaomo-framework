/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jiaomo.framework.commons;

import org.jiaomo.framework.commons.function.ThrowingSupplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.PrivateKeyDetails;
import org.apache.http.ssl.PrivateKeyStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 */

public class HttpClientUtils {
    private static final Logger log = LoggerFactory.getLogger(HttpClientUtils.class);

    public static volatile int MAX_TOTAL = 200;// 整个连接池最大连接数
    public static volatile int DEFAULT_MAX_PER_ROUTE = 20;// 每路由最大连接数，默认值是2

	private static volatile PoolingHttpClientConnectionManager defaultPoolingHttpClientConnectionManager = null;
	private static volatile CloseableHttpClient defaultHttpClient = null;

	public static PoolingHttpClientConnectionManager getDefaultPoolingHttpClientConnectionManager() {
		if (defaultPoolingHttpClientConnectionManager == null) {
			synchronized (HttpClientUtils.class) {
				if (defaultPoolingHttpClientConnectionManager == null) {
					defaultPoolingHttpClientConnectionManager = createDefaultPoolingHttpClientConnectionManager();
				}
			}
		}
		return defaultPoolingHttpClientConnectionManager;
	}

	private static PoolingHttpClientConnectionManager createDefaultPoolingHttpClientConnectionManager() {
		PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create()
			.register("http", PlainConnectionSocketFactory.INSTANCE)
			.register("https", new SSLConnectionSocketFactory(ThrowingSupplier.get(() -> {
				SSLContext sslContext = SSLContext.getInstance("SSLv3");
				sslContext.init(null,new TrustManager[] {
					new X509TrustManager() {
						@Override
						public void checkClientTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString) throws CertificateException {
						}

						@Override
						public void checkServerTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString) throws CertificateException {
						}

						@Override
						public X509Certificate[] getAcceptedIssuers() {
							return null;
						}
					}
				},null);
				return sslContext;
			}), new NoopHostnameVerifier())).build()
		);
		poolingHttpClientConnectionManager.setMaxTotal(MAX_TOTAL);
		poolingHttpClientConnectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE);

		startPoolingHttpClientConnectionManagerCloseConnectionsThread(poolingHttpClientConnectionManager);
		return poolingHttpClientConnectionManager;
	}

	public static SSLContext obtainSSLContext(String sslTrustStore,String sslTrustStorePassword,
		String sslKeyStore,String sslKeyStorePassword,String sslKeyPassword,String sslKeyAlias) {

		if (StringUtils.isBlank(sslTrustStore) || StringUtils.isBlank(sslTrustStorePassword) ||
				StringUtils.isBlank(sslKeyStore) || StringUtils.isBlank(sslKeyStorePassword) ||
				StringUtils.isBlank(sslKeyPassword) || StringUtils.isBlank(sslKeyAlias)) {
			return null;
		}

		return ThrowingSupplier.getWithoutThrowing(() -> new SSLContextBuilder()
				.loadTrustMaterial(ResourceUtils.getURL(sslTrustStore), sslTrustStorePassword.toCharArray())
				.loadKeyMaterial(ResourceUtils.getURL(sslKeyStore), sslKeyStorePassword.toCharArray(), sslKeyPassword.toCharArray(), new PrivateKeyStrategy() {
					@Override
					public String chooseAlias(Map<String, PrivateKeyDetails> aliases, Socket socket) {
						return sslKeyAlias;
					}
				}).build());
	}

	public static PoolingHttpClientConnectionManager obtainPoolingHttpClientConnectionManager(String sslTrustStore,String sslTrustStorePassword,
		String sslKeyStore,String sslKeyStorePassword,String sslKeyPassword,String sslKeyAlias) {
		return obtainPoolingHttpClientConnectionManager(obtainSSLContext(sslTrustStore,sslTrustStorePassword,sslKeyStore,sslKeyStorePassword,sslKeyPassword,sslKeyAlias));
	}

	public static PoolingHttpClientConnectionManager obtainPoolingHttpClientConnectionManager(SSLContext sslContext) {
		if (sslContext == null) {
			log.warn("sslContext is null, return defaultPoolingHttpClientConnectionManager");
			return getDefaultPoolingHttpClientConnectionManager();
		}

		PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.INSTANCE)
				.register("https", new SSLConnectionSocketFactory(sslContext,new NoopHostnameVerifier())).build());
		poolingHttpClientConnectionManager.setMaxTotal(MAX_TOTAL);
		poolingHttpClientConnectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE);

		startPoolingHttpClientConnectionManagerCloseConnectionsThread(poolingHttpClientConnectionManager);
		return poolingHttpClientConnectionManager;
	}

	private static void startPoolingHttpClientConnectionManagerCloseConnectionsThread(PoolingHttpClientConnectionManager poolingHttpClientConnectionManager) {
		Thread poolingHttpClientConnectionManagerThread = new Thread() {
			@Override
			public void run() {
				while (true) {
					LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(30L));

					// Close expired connections
					poolingHttpClientConnectionManager.closeExpiredConnections();
					// Optionally, close connections that have been idle longer than 30 sec
					poolingHttpClientConnectionManager.closeIdleConnections(30, TimeUnit.SECONDS);
				}
			}
		};
		poolingHttpClientConnectionManagerThread.setDaemon(true);
		poolingHttpClientConnectionManagerThread.start();
	}

	private static final ConnectionKeepAliveStrategy connectionKeepAliveStrategy = new ConnectionKeepAliveStrategy() {
		@Override
		public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
			HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
			while (it.hasNext()) {
				HeaderElement he = it.nextElement();
				String param = he.getName();
				String value = he.getValue();
				if (value != null && param.equalsIgnoreCase("timeout")) {
					try {
						return Long.parseLong(value) * 1000;
					} catch (NumberFormatException ignored) {
					}
				}
			}
			return 30 * 1000;//如果没有约定，则默认定义时长为30s
		}
	};

	public static CloseableHttpClient obtainHttpClient() {
		if (defaultHttpClient == null) {
			synchronized (HttpClientUtils.class) {
				if (defaultHttpClient == null) {
					defaultHttpClient = obtainHttpClient(null);
				}
			}
		}
		return defaultHttpClient;
	}
	public static CloseableHttpClient obtainHttpClient(PoolingHttpClientConnectionManager poolingHttpClientConnectionManager) {
	    //CloseableHttpClient httpClient = HttpClients.createDefault();//如果不采用连接池就是这种方式获取连接

		if (poolingHttpClientConnectionManager == null) {
			CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(getDefaultPoolingHttpClientConnectionManager()).setKeepAliveStrategy(connectionKeepAliveStrategy).build();
			log.debug("poolingHttpClientConnectionManager {}", getDefaultPoolingHttpClientConnectionManager().getTotalStats().toString());
			return httpClient;
		} else {
			CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(poolingHttpClientConnectionManager).setKeepAliveStrategy(connectionKeepAliveStrategy).build();
			log.debug("poolingHttpClientConnectionManager {}", poolingHttpClientConnectionManager.getTotalStats().toString());
			return httpClient;
		}
	}

	public static HttpGet obtainHttpGet(String url) {
		return new HttpGet(url);
	}

	public static HttpGet obtainHttpGet(String url, Map<String, String> params) throws URISyntaxException {
		return obtainHttpGet(url,null,params);
	}

	public static HttpGet obtainHttpGet(String url, Map<String, String> headers, Map<String, String> params) throws URISyntaxException {
		URIBuilder ub = new URIBuilder(url);

		if (params != null) {
			List<NameValuePair> pairs = covertParams2NVPS(params);
			ub.setParameters(pairs);
		}

		HttpGet httpGet = new HttpGet(ub.build());
		if (headers != null) {
			for (Map.Entry<String, String> param : headers.entrySet()) {
				httpGet.addHeader(param.getKey(), param.getValue());
			}
		}
		return httpGet;
	}

	public static HttpPost obtainHttpPost(String url) {
		return new HttpPost(url);
	}

	public static HttpPost obtainHttpPost(String url, Map<String, String> params) {
		return obtainHttpPost(url,null,params);
	}

	public static HttpPost obtainHttpPost(String url, Map<String, String> headers, Map<String, String> params) {
		HttpPost httpPost = new HttpPost(url);

		if (headers != null) {
			for (Map.Entry<String, String> param : headers.entrySet()) {
				httpPost.addHeader(param.getKey(), param.getValue());
			}
		}

		if (params != null) {
			List<NameValuePair> pairs = covertParams2NVPS(params);
			httpPost.setEntity(new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8));
		}

		return httpPost;
	}

	private static List<NameValuePair> covertParams2NVPS(Map<String, String> params) {
		return params.entrySet().stream().map(e -> new BasicNameValuePair(e.getKey(),e.getValue())).collect(ArrayList::new,List::add,List::addAll);
	}

	public static ResponseHandler<String> responseHandlerString = new BasicResponseHandler();
/*
	public static ResponseHandler<String> responseHandlerString = new ResponseHandler<String>() {
		@Override
		public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
			log.debug("httpResponse {} {}",response.getStatusLine().getStatusCode(),response.getStatusLine().getReasonPhrase());
			HttpEntity entity = response.getEntity();
			if (entity != null)
				return EntityUtils.toString(entity);
			else
				return null;
		}
	};
*/
	public static ResponseHandler<ApiResult<String>> responseHandlerApiResultString = new ResponseHandler<ApiResult<String>>() {
		@Override
		public ApiResult<String> handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
			ApiResult<String> apiResult = new ApiResult<String>(response.getStatusLine().getStatusCode(),response.getStatusLine().getReasonPhrase());
			log.debug("httpResponse {} {}",response.getStatusLine().getStatusCode(),response.getStatusLine().getReasonPhrase());
			HttpEntity entity = response.getEntity();
			if (entity != null) apiResult.setData(EntityUtils.toString(entity));
			return apiResult;
		}
	};

	public static String executeGetAndObtainResponse(String url) throws IOException {
		return executeRequestAndObtainResponse(obtainHttpGet(url));
	}

	public static String executeRequestAndObtainResponse(HttpRequestBase request) throws IOException {
		return obtainHttpClient().execute(request,responseHandlerString);
	}

	public static String executePostJsonAndObtainResponse(String url,String json) throws IOException {
		return executePostJsonAndObtainResponse(url,null,json);
	}
	public static String executePostJsonAndObtainResponse(String url,RequestConfig requestConfig,String json) throws IOException {
		HttpPost httpPost = new HttpPost(url);
		if (requestConfig != null) httpPost.setConfig(requestConfig);
		StringEntity stringEntity = new StringEntity(json, StandardCharsets.UTF_8);
		stringEntity.setContentType("application/json");
		httpPost.setEntity(stringEntity);
		return executeRequestAndObtainResponse(httpPost);
	}

	public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(30 * 1000)
				.setSocketTimeout(30 * 1000)
				.setConnectionRequestTimeout(30 * 1000)
				.setRedirectsEnabled(false)
				.build();

		CountDownLatch countDownLatch = new CountDownLatch(500);
		for (int icount=0;icount<countDownLatch.getCount();icount++) {
			new Thread("thread" + icount) {
				@Override
				public void run() {
					try {
						HttpGet httpGet = Integer.parseInt(getName().substring("thread".length())) % 2 == 0 ? new HttpGet("https://www.baidu.com/") : new HttpGet("https://www.sina.com.cn");
//						httpGet.setConfig(requestConfig);
						executeRequestAndObtainResponse(httpGet);
						System.out.println(Thread.currentThread().getName() + " " + getDefaultPoolingHttpClientConnectionManager().getTotalStats().toString());
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						countDownLatch.countDown();
					}
				}
			}.start();
		}
		countDownLatch.await();

		for (int icount=0;icount<10;icount++) {
		    Thread.sleep(5000);
            System.out.println(Thread.currentThread().getName() + " " + getDefaultPoolingHttpClientConnectionManager().getTotalStats().toString());
        }

//		System.out.println(httpGetRequest("https://www.sina.com.cn/",null,null,requestConfig));
//		System.out.println(httpGetRequest("https://www.sina.com.cn/"));
//		System.out.println(httpGetRequest("http://www.sohu.com/"));
	}
}