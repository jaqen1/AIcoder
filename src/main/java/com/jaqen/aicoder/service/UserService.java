package com.jaqen.aicoder.service;

import com.jaqen.aicoder.model.entity.dto.UserQueryRequest;
import com.jaqen.aicoder.model.entity.vo.LoginUserVO;
import com.jaqen.aicoder.model.entity.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.jaqen.aicoder.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 *  服务层。
 *
 * @author <a href="https://github.com/jaqen1">jaqen</a>
 */
public interface UserService extends IService<User> {


    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);


    public String getEncryptPassword(String userPassword);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);


    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     *查询用户，获取脱敏后的，用户信息
     * @param user
     * @return
     */
    public UserVO getUserVO(User user);


    /**
     *查询用户，获取脱敏后的，用户信息列表（分页）
     * @param userList
     * @return
     */
    public List<UserVO> getUserVOList(List<User> userList);


    /**
     * 查询请求的参数，转化为 查询条件
     * @param userQueryRequest
     * @return
     */
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);
}
