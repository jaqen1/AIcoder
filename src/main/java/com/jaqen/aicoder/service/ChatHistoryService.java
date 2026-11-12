package com.jaqen.aicoder.service;

import com.jaqen.aicoder.model.entity.User;
import com.jaqen.aicoder.model.entity.dto.chathistory.ChatHistoryQueryRequest;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.jaqen.aicoder.model.entity.ChatHistory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 *  服务层。
 *
 * @author <a href="https://github.com/jaqen1">jaqen</a>
 */
public interface ChatHistoryService extends IService<ChatHistory> {


    /**
     *  添加消息记录
     *
     * @param appId
     * @param message
     * @param messageType
     * @param userId
     * @return
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);


    /**
     *  分页 查询 appid 的 历史对话
     * @param appId
     * @param pageSize
     * @param lastCreateTime
     * @param loginUser
     * @return
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);

    /**
     *  删除应用时，删除历史对话
     * @param appId
     * @return
     */
    boolean deleteByAppId(Long appId);


    /**
     *  获取 查询条件 的包装 类
     * @param chatHistoryQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    /**
     *  创建 AI服务实例时，将历史对话 加载到内存
     *
     * @param appId
     * @param chatMemory
     * @param maxCount
     * @return  加载成功的数量
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}
