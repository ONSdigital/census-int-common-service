package uk.gov.ons.ctp.common.message.rabbit;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class SimpleMessageSender extends SimpleMessageBase {

    public SimpleMessageSender(String host, int port, String username, String password) {
        super(host, port, username, password);
    }

    public void sendMessage(String exchange, String routingKey, String message){
        RabbitTemplate rabbitTemplate = getRabbitTemplate();

        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }

    public void sendMessage(String exchange, String message){
        RabbitTemplate rabbitTemplate = getRabbitTemplate();

        rabbitTemplate.convertAndSend(exchange, message);
    }
}
