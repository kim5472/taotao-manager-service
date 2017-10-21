package com.taotao.activemq;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

public class SpringActiveMQ {
	// 使用jmsTemplate 发送消息
	@Test
	public void testJmsTemplate()throws Exception{
		// 初始化spring容器
		ApplicationContext ac = 
				new ClassPathXmlApplicationContext(
						"classpath:spring/applicationContext-activemq.xml");
		// 从容器中获得JmsTemplate对象
		JmsTemplate jmsTemplate = ac.getBean(JmsTemplate.class);
		// 从容器中获得Destination对象
		Destination destination = (Destination)ac.getBean("test-queue");
		// 发送消息
		jmsTemplate.send(destination, new MessageCreator() {
			
			@Override
			public Message createMessage(Session session) throws JMSException {
				TextMessage message = session.createTextMessage("spring activemq send queue message");
				return message;
			}
		});
		
	}
}
