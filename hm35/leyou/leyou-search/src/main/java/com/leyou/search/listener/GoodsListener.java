package com.leyou.search.listener;

import com.leyou.search.service.SearchService;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GoodsListener {

    @Autowired
    private SearchService searchService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "LEYOU_SEARCH_SAVE_INDEX", durable = "true"),
            exchange = @Exchange(value = "LEYOU_ITEM_EXCHANGE", type = ExchangeTypes.TOPIC, ignoreDeclarationExceptions = "true"),
            key = {"item.insert", "item.update"}
    ))
    public void savelistener(Long id) throws IOException {
        if (id == null) {
            return;
        }
        this.searchService.createIndex(id);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "LEYOU_SEARCH_DELETE_INDEX", durable = "true"),
            exchange = @Exchange(value = "LEYOU_ITEM_EXCHANGE", type = ExchangeTypes.TOPIC, ignoreDeclarationExceptions = "true"),
            key = {"item.delete"}
    ))
    public void deletelistener(Long id) throws IOException {
        if (id == null) {
            return;
        }
        this.searchService.deleteIndex(id);
    }

}
