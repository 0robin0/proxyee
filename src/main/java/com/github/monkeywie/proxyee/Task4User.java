package com.github.monkeywie.proxyee;

import com.alibaba.fastjson.JSONObject;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

public class Task4User implements Runnable{

    protected static final Logger log = LoggerFactory.getLogger(Task4User.class);

    RestHighLevelClient restHighClient = null;

    public Task4User(RestHighLevelClient restHighClient) {
        this.restHighClient = restHighClient;
    }

    public void run(){
        //System.out.println(Thread.currentThread().getName());
        String content = HttpProxyServerApp.queue4User.poll();
        if(content == null||content.isEmpty()){
            return ;
        }

        JSONObject outJson = JSONObject.parseObject(content);
        Set<String> jsonSet = outJson.keySet();
        if (!jsonSet.contains("user") ) {
            log.info("=====Task4User err! no user!! content:"+content);
            return ;
        }

        if(!outJson.getJSONObject("user").containsKey("sec_uid")) {
            log.info("=====Task4User err! no user.sec_uid!! content:"+content);
            return ;
        }

        String id = outJson.getJSONObject("user").getString("sec_uid");
        IndexRequest request = new IndexRequest("tiktok");
        request.timeout(TimeValue.timeValueMinutes(5));//超时,等待所有节点被确认(使用TimeValue方式)

        request.id(id);	//ID也可使用内部自动生成的 不过希望和数据库统一唯一业务ID
        request.source(content, XContentType.JSON);

        try {
            IndexResponse indexResponse = restHighClient.index(request, RequestOptions.DEFAULT);
            if (indexResponse.getResult() == DocWriteResponse.Result.CREATED) {
                //处理（如果需要）第一次创建文档的情况
            } else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {
                //处理（如果需要）文档被重写的情况
            }
            System.out.println(indexResponse.getResult());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                restHighClient.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
