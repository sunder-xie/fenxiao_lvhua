package com.newwing.fenxiao.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.newwing.fenxiao.dao.IUserDao;
import com.newwing.fenxiao.entities.User;
import com.newwing.fenxiao.service.IApiService;
import com.newwing.fenxiao.service.IUserService;

@Service("userService")
@Scope("prototype")
public class UserServiceImpl<T extends User> extends BaseServiceImpl<T> implements IUserService<T> {

	@Resource(name = "userDao")
	private IUserDao userDao;
	@Resource(name = "apiService")
	private IApiService apiService;

	public User getUserByName(String name) {
		return this.userDao.getUserByName(name);
	}

	public User login(String name, String password) {
		return this.userDao.login(name, password);
	}

	public User getUserByPhone(String phone) {
		return this.userDao.getUserByPhone(phone);
	}

	public User getUserByNo(String no) {
		return this.userDao.getUserByNo(no);
	}
	
	public User getUserByNoAndType(String no, String type) {
		return this.userDao.getUserByNoAndType(no, type);
	}
	
	public User getUserByOpenId(String openId) {
		return this.userDao.getUserByOpenId(openId);
	}

	public List<User> levelUserList(String no) {
		return this.userDao.levelUserList(no);
	}

	public List<User> levelUserTodayList(String no) {
		return this.userDao.levelUserTodayList(no);
	}

	public List<User> levelUserTodayStatusList(String no) {
		return this.userDao.levelUserTodayStatusList(no);
	}

	public User getUserByNameAndPhone(String name, String phone) {
		return this.userDao.getUserByNameAndPhone(name, phone);
	}
	
	public List<User> findProfitUserList() {
		return this.userDao.findProfitUserList();

	}
	
	public void register(User user, String type, String phone, String password, 
			String password2, String source) throws Exception {
		// TODO 
//		if ("1".equals(type)) {
//			this.apiService.agencyRegister(phone, password, source);
//		} else {
//			this.apiService.userRegister(phone, password, password2, source);
//		}
		this.userDao.saveOrUpdate(user);
	}
	
	public void getSuperUser(int counter, String type, User user) throws Exception {
		User superUser = this.userDao.getUserByNo(user.getSuperNo());
		if (superUser != null && !type.equals(superUser.getType())) {
			counter ++;
			this.getSuperUser(counter, type, superUser);
		}
	}
	
	public String getSuperUserids(User user) {
		String ids = ",";
		String levelNos = user.getSuperior();
		if (!StringUtils.isEmpty(levelNos)) {
			String[] leverNoArr = levelNos.split(";");
			for (int i = 0; i < leverNoArr.length; i++) {
				User tempUser = this.userDao.getUserByNo(leverNoArr[i]);
				if (tempUser != null) {
					ids = ids + tempUser.getId() + ",";
				}
			}
		}
		return ids.substring(1,ids.length() - 1);
	}
	
}