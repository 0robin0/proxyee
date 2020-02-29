package com.github.monkeywie.proxyee;

import java.io.IOException;
import java.nio.charset.Charset;

import java.util.Vector;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline;
import com.github.monkeywie.proxyee.intercept.common.FullResponseIntercept;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpiderResponseIntercept extends FullResponseIntercept {

	protected static final Logger log = LoggerFactory.getLogger(SpiderResponseIntercept.class);

	private static String ua = "Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1";

	@Override
	public void beforeRequest(Channel clientChannel, HttpRequest httpRequest,
							  HttpProxyInterceptPipeline pipeline) throws Exception {
		//替换UA，伪装成手机浏览器
		httpRequest.headers().set(HttpHeaderNames.USER_AGENT,
                    "Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1");
		//转到下一个拦截器处理
		pipeline.beforeRequest(clientChannel, httpRequest);
	}

	 @Override
     public boolean match(HttpRequest httpRequest, HttpResponse httpResponse, HttpProxyInterceptPipeline pipeline) {

         //System.out.println("uri===="+pipeline.getHttpRequest().uri());
         return pipeline.getHttpRequest().uri().contains("/aweme/v1/general/search/single")||pipeline.getHttpRequest().uri().contains("/aweme/v1/user/profile/other");

     }

     @Override
     public void handelResponse(HttpRequest httpRequest, FullHttpResponse httpResponse, HttpProxyInterceptPipeline pipeline) {
         //打印原始响应信息
		 String content=httpResponse.content().toString(Charset.defaultCharset());
    	 if(pipeline.getHttpRequest().uri().contains("/aweme/v1/general/search/single")) {
			 log.info("search content====="+content);
	         Vector<String> ret = HttpProxyServerApp.parseAndAdd4Search(content);
    	 } else if(pipeline.getHttpRequest().uri().contains("/aweme/v1/user/profile/other")) {
	         log.info("user content====="+content);
	         Object object = JSON.parse(content);
	         if (object instanceof JSONObject||object instanceof JSONArray) {
	        	 HttpProxyServerApp.parseAndAdd4User(content);
	         }
    	 }
     }
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		RestHighLevelClient restHighClient=new RestHighLevelClient(RestClient.builder(new HttpHost("112.74.105.107", 9200, "http")));

		IndexRequest request = new IndexRequest("test");
        request.id("123");	//ID也可使用内部自动生成的 不过希望和数据库统一唯一业务ID
        request.source("{\"a\":656}", XContentType.JSON);
        try {
			restHighClient.index(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			try {
				restHighClient.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
