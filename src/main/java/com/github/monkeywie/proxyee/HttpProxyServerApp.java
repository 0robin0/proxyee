package com.github.monkeywie.proxyee;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptInitializer;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline;
import com.github.monkeywie.proxyee.intercept.common.CertDownIntercept;
import com.github.monkeywie.proxyee.server.HttpProxyServer;
import com.github.monkeywie.proxyee.server.HttpProxyServerConfig;



import java.util.Queue;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @Author LiWei
 * @Description
 * @Date 2019/9/23 17:30
 */
public class HttpProxyServerApp  {

    protected static final Logger log = LoggerFactory.getLogger(HttpProxyServerApp.class);

    public static Queue<Vector<String>> queue4Search = new ConcurrentLinkedQueue<Vector<String>>();
    public static Queue<String> queue4User = new ConcurrentLinkedQueue<String>();

    public static Vector<String> parseAndAdd4Search(String content){
        Vector<String> ret = new Vector<String>();
        try {
            JSONObject outJson = JSONObject.parseObject(content);
            Set<String> jsonSet = outJson.keySet();
            if (jsonSet.contains("status_code") && jsonSet.contains("data")) {
                int status = outJson.getIntValue("status");
                JSONArray arrayData = outJson.getJSONArray("data");
                for (int i = 0; i < arrayData.size(); i++) {
                    JSONObject jsonobj = arrayData.getJSONObject(i);
                    if (jsonobj != null ) {
                        if(jsonobj.containsKey("aweme_info")
                                && jsonobj.getJSONObject("aweme_info").containsKey("author")
                                && jsonobj.getJSONObject("aweme_info").getJSONObject("author").getString("sec_uid")!=null){
                            try {
                                String value = jsonobj.getJSONObject("aweme_info").getJSONObject("author").getString("sec_uid");
                                ret.add(value);
                            }catch(Exception e){
                                log.info(arrayData.getJSONObject(i).toJSONString());
                                continue;
                            }
                        }
                    }

                }

                if(ret.size() != 0) {
                    queue4Search.offer(ret);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
            return ret;
        }
        return ret;
    }
    
    public static void parseAndAdd4User(String content) {
    	if(content != null||!content.isEmpty()) {
            queue4User.offer(content);
        }
    }
    
    ///aweme/v1/user/profile/other/
    public static void main(String[] args) {

        ScheduledExecutorService ex = Executors.newScheduledThreadPool(4);
        Task4Search task4Search = new Task4Search();
        RestHighLevelClient restHighClient=new RestHighLevelClient(RestClient.builder(new HttpHost("112.74.105.107", 9200, "http")));
        Task4User task4User = new Task4User(restHighClient);
        //参数1：目标对象   参数2：隔多长时间开始执行线程，    参数3：执行周期       参数4：时间单位
        ex.scheduleAtFixedRate(task4Search, 500, 1, TimeUnit.MILLISECONDS);
        ex.scheduleAtFixedRate(task4User, 200, 1, TimeUnit.MILLISECONDS);


        int port = 9876;
        if (args.length > 0) {
            port = Integer.valueOf(args[0]);
        }
        HttpProxyServerConfig config =  new HttpProxyServerConfig();
        config.setHandleSsl(true);
        config.setWorkerGroupThreads(10);
        new HttpProxyServer().serverConfig(config)
                .proxyInterceptInitializer(new HttpProxyInterceptInitializer() {

                    @Override
                    public void init(HttpProxyInterceptPipeline pipeline) {
                        pipeline.addLast(new CertDownIntercept());
                        pipeline.addLast(new SpiderResponseIntercept());
                    }
                })
                .start(port);

    }
}



