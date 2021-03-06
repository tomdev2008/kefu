package com.ukefu.util.router;

import com.ukefu.core.UKDataContext;
import com.ukefu.service.acd.ServiceQuene;
import com.ukefu.util.UKTools;
import com.ukefu.util.client.NettyClients;
import com.ukefu.web.model.AgentService;
import com.ukefu.web.model.MessageDataBean;
import com.ukefu.web.model.MessageOutContent;

public class MessageRouter extends Router{

	@Override
	public MessageDataBean handler(MessageDataBean inMessage) {
		MessageOutContent outMessage = new MessageOutContent() ;
		try {
			outMessage.setOrgi(inMessage.getOrgi());
			outMessage.setFromUser(inMessage.getToUser());
			outMessage.setToUser(inMessage.getFromUser());
			outMessage.setId(UKTools.genID());
			outMessage.setMessageType(inMessage.getMessageType());
			outMessage.setUser(inMessage.getUser());
			outMessage.setAgentUser(inMessage.getAgentUser());
			/**
			 * 首先交由 IMR处理 MESSAGE指令 ， 如果当前用户是在 坐席对话列表中， 则直接推送给坐席，如果不在，则执行 IMR
			 */
			if(outMessage.getAgentUser()!=null && outMessage.getAgentUser().getStatus()!=null){
				if(outMessage.getAgentUser().getStatus().equals(UKDataContext.AgentUserStatusEnum.INQUENE.toString())){
					int queneIndex = ServiceQuene.getQueneIndex(inMessage.getFromUser(), inMessage.getOrgi(), inMessage.getAgentUser().getOrdertime()) ;
					if(UKDataContext.AgentUserStatusEnum.INQUENE.toString().equals(outMessage.getAgentUser().getStatus())){
						outMessage.setMessage(ServiceQuene.getQueneMessage(queneIndex));
					}
				}else if(outMessage.getAgentUser().getStatus().equals(UKDataContext.AgentUserStatusEnum.INSERVICE.toString())){
					
				}
			}else if(UKDataContext.MessageTypeEnum.NEW.toString().equals(inMessage.getMessageType())){
				/**
				 * 找到空闲坐席，如果未找到坐席， 则将该用户放入到 排队队列 
				 * 
				 */
				AgentService agentService = ServiceQuene.allotAgent(inMessage.getAgentUser(), inMessage.getOrgi()) ;
				if(agentService!=null && UKDataContext.AgentUserStatusEnum.INSERVICE.toString().equals(agentService.getStatus())){
					outMessage.setMessage(ServiceQuene.getSuccessMessage(agentService));
					NettyClients.getInstance().sendAgentEventMessage(agentService.getAgentid(), UKDataContext.MessageTypeEnum.NEW.toString(), inMessage.getAgentUser());
				}else{
					int queneIndex = ServiceQuene.getQueneIndex(inMessage.getFromUser(), inMessage.getOrgi(), inMessage.getAgentUser().getOrdertime()) ;
					outMessage.setMessage(ServiceQuene.getNoAgentMessage(queneIndex));
				}
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return outMessage ;
	}

}
