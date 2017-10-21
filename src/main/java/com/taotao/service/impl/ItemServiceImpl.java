package com.taotao.service.impl;

import java.util.Date;

import javax.annotation.Resource;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Service;

import com.taotao.common.utils.IDUtils;
import com.taotao.common.utils.JsonUtils;
import com.taotao.common.utils.TaotaoResult;
import com.taotao.jedis.JedisClient;
import com.taotao.mapper.TbItemDescMapper;
import com.taotao.mapper.TbItemMapper;
import com.taotao.pojo.TbItem;
import com.taotao.pojo.TbItemDesc;
import com.taotao.service.ItemService;

@Service
public class ItemServiceImpl implements ItemService{

	@Autowired
	private TbItemMapper itemMapper;
	@Autowired
	private TbItemDescMapper itemDescMapper;
	@Autowired
	private JmsTemplate jmsTemplate;
	@Resource(name="itemAddTopic")
	private Destination destination;
	@Autowired
	private JedisClient jedisClient;
	@Value("${ITEM_INFO}")
	private String ITEM_INFO;
	
	@Value("${ITEM_EXPIRE}")
	private Integer ITEM_EXPIRE;
	
	@Override
	public TbItem getItemById(long itemId) {
		// 查询数据库之前先查缓存
		try {
			String json = jedisClient.get(ITEM_INFO+":"+itemId+":BASE");
			if (StringUtils.isNotBlank(json)) {
				//把json数据转换为pojo
				TbItem tbItem = JsonUtils.jsonToPojo(json, TbItem.class);
				return tbItem;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 缓存中没有查询数据库
		TbItem item = itemMapper.selectByPrimaryKey(itemId);
		
		try {
			// 把查询结果添加到缓存
			jedisClient.set(ITEM_INFO+":"+itemId+":BASE", JsonUtils.objectToJson(item));
			// 设置缓存时间 提高缓存的利用率
			jedisClient.expire(ITEM_INFO+":"+itemId+":BASE", ITEM_EXPIRE);
			// : 的作用：相当于文件夹分隔的作用
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return item;
	}

	@Override
	public TaotaoResult addItem(TbItem item, String desc) {
		// 生成商品id
		final long itemId = IDUtils.genItemId();
		// 补全item属性
		item.setId(itemId);
		// 商品状态 1-正常 2-下架 3-删除
		item.setStatus((byte) 1);
		item.setCreated(new Date());
		item.setUpdated(new Date());
		// 向商品表插入数据
		itemMapper.insert(item);
		// 创建一个商品描述对应的pojo
		TbItemDesc itemDesc = new TbItemDesc();
		// 补全pojo
		itemDesc.setItemId(itemId);
		itemDesc.setItemDesc(desc);
		itemDesc.setCreated(new Date());
		itemDesc.setUpdated(new Date());
		// 向商品描述插入数据
		itemDescMapper.insert(itemDesc);
		
		//向Activemq发送商品添加消息
		jmsTemplate.send(destination, new MessageCreator() {
			
			@Override
			public Message createMessage(Session session) throws JMSException {
				// 发送商品id
				TextMessage message = session.createTextMessage(itemId+"");
				return message;
			}
		});
		
		// 返回结果
		return TaotaoResult.ok();
	}

	@Override
	public TbItemDesc getItemDescById(long itemId) {
		// 先查询缓存
		String json = jedisClient.get(ITEM_INFO+":"+itemId+":DESC");
		if (StringUtils.isNotBlank(json)) {
			// 将json转换成pojo
			TbItemDesc tbItemDesc = JsonUtils.jsonToPojo(json, TbItemDesc.class);
			return tbItemDesc;
		}
		
		
		
		// 查询数据库
		TbItemDesc itemDesc = itemDescMapper.selectByPrimaryKey(itemId);
		try {
			// 将描述存入到缓存
			jedisClient.set(ITEM_INFO+":"+itemId+":DESC", JsonUtils.objectToJson(itemDesc));
			// 设置缓存时间
			jedisClient.expire(ITEM_INFO+":"+itemId+":DESC", ITEM_EXPIRE);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
		return itemDesc;
	}

//	@Override
//	public EasyUIDataGridResult getItemList(int page, int rows) {
//		// 设置分页信息
//		PageHelper.startPage(page, rows);
//		TbItemExample example = new TbItemExample();
//		List<TbItem> list = itemMapper.selectByExample(example );
//		// 去查询结果
//		PageInfo<TbItem> pageInfo = new PageInfo<>(list);
//		EasyUIDataGrifResult result = new EasyUIDataGrifResult();
//		result.setRows(list);
//		result.setTotal(pageInfo.getTotal());
//		return result;
//	}
	
}
