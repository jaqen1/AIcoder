package com.jaqen.aicoder.service;

import com.jaqen.aicoder.model.entity.dto.app.AppQueryRequest;
import com.jaqen.aicoder.model.entity.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.jaqen.aicoder.model.entity.App;

import java.util.List;

/**
 *  服务层。
 *
 * @author <a href="https://github.com/jaqen1">jaqen</a>
 */
public interface AppService extends IService<App> {

    /**
     *  获取应用 封装类
     * @param app
     * @return
     */
    public AppVO getAppVO(App app);


    /**
     *  构造应用 查询条件
     * @param appQueryRequest
     * @return
     */
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);


    /**
     *  获取应用封装列表
     * @param appList
     * @return
     */
    List<AppVO> getAppVOList(List<App> appList);
}
