package cn.itcast.hotel;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
@SpringBootTest
public class aggtest {
    @Autowired
    private IHotelService hotelService;


    RestHighLevelClient restHighLevelClient;

    @BeforeEach
    void setUp(){
        restHighLevelClient= new RestHighLevelClient(RestClient.builder(
                //这里可以加多个HttpHost
                HttpHost.create("http://172.16.23.152:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        restHighLevelClient.close();
    }
    @Test
    void testInit(){
        System.out.println(restHighLevelClient);
    }
    @Test
    void BulkaddDocument() throws IOException {
        //批量查询酒店数据
        List<Hotel> hotelList=hotelService.list();
        //创建Request
        BulkRequest request=new BulkRequest();
        //准备参数，添加多个新增的Request对象
        for (Hotel hotel : hotelList) {
            //转换文档类型为HotelDoc
            HotelDoc hotelDoc=new HotelDoc(hotel);
            //创建新增文档的request对象
            request.add(new IndexRequest("hotel")
                    .id(hotelDoc.getId().toString())
                    .source(JSON.toJSONString(hotelDoc), XContentType.JSON));
        }
        //发送请求
        restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
    }
    @Test
    void testSuggest() throws IOException {
        // 1.准备request
        SearchRequest request=new SearchRequest("hotel");
        // 2. 准备DSL
        request.source().suggest(new SuggestBuilder()
                .addSuggestion(
                        "suggestions",
                        SuggestBuilders.completionSuggestion("suggestion").prefix("h").skipDuplicates(true).size(10)
                )
        );
        // 3. 发起请求
        SearchResponse res = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        //4. 解析结果
        Suggest suggest=res.getSuggest();
        CompletionSuggestion suggestion=suggest.getSuggestion("suggestions");
        List<CompletionSuggestion.Entry.Option> options1 = suggestion.getOptions();
        for (Suggest.Suggestion.Entry.Option options : options1) {
            String text = options.getText().string();
            System.out.println(text);
        }
    }
}
