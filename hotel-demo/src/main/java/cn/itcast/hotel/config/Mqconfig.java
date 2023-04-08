package cn.itcast.hotel.config;

import cn.itcast.hotel.Constans.MqConstans;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Mqconfig {
    @Bean
    public TopicExchange topicExchange(){  //交换机定义
        /*
        durable:指示是否应该将该交换机声明为持久化的。如果设置为true，则表示该交换机在RabbitMQ服务器重新启动时也将继续存在。
        autoDelete：指示是否应该将该交换机声明为持久化的。如果设置为true，则表示该交换机在RabbitMQ服务器重新启动时也将继续存在。
        * */
        return  new TopicExchange(MqConstans.HOTEL_EXCHANGE,true,false);
    }
    @Bean
    public Queue InsertQueue(){
        return  new Queue(MqConstans.HOTEL_INSERT_QUEUE,true);
    }
    @Bean
    public Queue DeleteQueue(){
        return  new Queue(MqConstans.HOTEL_DELETE_QUEUE,true);
    }
    @Bean
    public Binding InsertQueueBinding(){
        return BindingBuilder.bind(InsertQueue()).to(topicExchange()).with(MqConstans.HOTEL_INSERT_KEY);
    }
    @Bean
    public Binding DeleteQueueBinding(){
        return BindingBuilder.bind(DeleteQueue()).to(topicExchange()).with(MqConstans.HOTEL_DELETE_KEY);
    }
}
