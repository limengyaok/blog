package com.hmdp;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class ElasticSearchTest {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private IShopService shopService;

    @BeforeEach
    void  setUp() {
        client = new RestHighLevelClient(RestClient.builder(HttpHost.create("http://localhost:9200")));
    }
    @AfterEach
    void tearDown() throws IOException {
        client.close();
    }

    @Test
    public void testElasticSearch() {
        List<Shop> list = shopService.list();

        for (Shop shop : list) {
            Map<String, Object> sourceMap = new HashMap<>();
            sourceMap.put("id", shop.getId());
            sourceMap.put("name", shop.getName());
            sourceMap.put("type_id", shop.getTypeId());
            sourceMap.put("images", shop.getImages());
            sourceMap.put("area", shop.getArea());
            sourceMap.put("address", shop.getAddress());
            sourceMap.put("x", shop.getX());
            sourceMap.put("y", shop.getY());
            sourceMap.put("avg_price", shop.getAvgPrice());
            sourceMap.put("sold", shop.getSold());
            sourceMap.put("comments", shop.getComments());
            sourceMap.put("score", shop.getScore());
            sourceMap.put("open_hours", shop.getOpenHours());
            sourceMap.put("create_time", shop.getCreateTime());
            sourceMap.put("update_time", shop.getUpdateTime());
            sourceMap.put("distance", shop.getDistance());

            // 使用传统方式创建地理位置 Map
            if (shop.getX() != null && shop.getY() != null) {
                Map<String, Object> locationMap = new HashMap<>();
                locationMap.put("lon", shop.getX());  // 经度
                locationMap.put("lat", shop.getY());  // 纬度
                sourceMap.put("location", locationMap);
            }

            IndexRequest request = new IndexRequest("shop").id(shop.getId().toString());
            request.source(sourceMap, XContentType.JSON);

            try {
                client.index(request, RequestOptions.DEFAULT);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
