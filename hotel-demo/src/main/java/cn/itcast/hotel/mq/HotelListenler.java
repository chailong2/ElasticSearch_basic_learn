package cn.itcast.hotel.mq;

import cn.itcast.hotel.Constans.MqConstans;
import cn.itcast.hotel.service.IHotelService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HotelListenler {
    @Autowired
    private IHotelService hotelService;
    /**
     * 监听酒店新增或修改的业务
     * @param id 酒店id
     */
    @RabbitListener(queues = MqConstans.HOTEL_INSERT_QUEUE)
    public void listenHotelInsertOrUpdate(Long id){
        hotelService.insertByid(id);
    }
    /**
     * 监听酒店删除的业务
     * @param id 酒店id
     */
    @RabbitListener(queues = MqConstans.HOTEL_DELETE_QUEUE)
    public void listenHotelDelete(Long id){
         hotelService.deleteByid(id);
    }
}