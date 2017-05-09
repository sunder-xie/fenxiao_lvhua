package com.newwing.fenxiao.action;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.alibaba.fastjson.JSONObject;
import com.newwing.fenxiao.entities.Admin;
import com.newwing.fenxiao.entities.Config;
import com.newwing.fenxiao.entities.Financial;
import com.newwing.fenxiao.entities.Orders;
import com.newwing.fenxiao.entities.PhoneValidateCode;
import com.newwing.fenxiao.entities.User;
import com.newwing.fenxiao.service.IAdminService;
import com.newwing.fenxiao.service.IApiService;
import com.newwing.fenxiao.service.IConfigService;
import com.newwing.fenxiao.service.IFinancialService;
import com.newwing.fenxiao.service.IOrdersService;
import com.newwing.fenxiao.service.IPhoneValidateCodeService;
import com.newwing.fenxiao.service.IUserService;
import com.newwing.fenxiao.utils.BjuiJson;
import com.newwing.fenxiao.utils.BjuiPage;
import com.newwing.fenxiao.utils.FreemarkerUtils;
import com.newwing.fenxiao.utils.IpUtils;
import com.newwing.fenxiao.utils.Md5;

import freemarker.template.Configuration;

@Controller("userAction")
@Scope("prototype")
public class UserAction extends BaseAction {
	private static final long serialVersionUID = 1L;

	@Resource(name = "userService")
	private IUserService<User> userService;
	
	@Resource(name = "ordersService")
	private IOrdersService<Orders> ordersService;
	
	@Resource(name = "adminService")
	private IAdminService<Admin> adminService;
	
	@Resource(name = "apiService")
	private IApiService apiService;

	@Resource(name = "configService")
	private IConfigService<Config> configService;

	@Resource(name = "financialService")
	private IFinancialService<Financial> financialService;
	
	@Resource(name = "phoneValidateCodeService")
	private IPhoneValidateCodeService<PhoneValidateCode> phoneValidateCodeService;
	
	private User user;
	private String ftlFileName;

	public void list() {
		String key = this.request.getParameter("key");
		String countHql = "select count(*) from User where deleted=0 and type='0' ";
		String hql = "from User where deleted=0 and type='0' ";
		if (StringUtils.isNotEmpty(key)) {
			countHql = countHql + " and (name='" + key + "' or no='" + key + "' or phone='" + key + "')";
			hql = hql + " and (name='" + key + "' or no='" + key + "' or phone='" + key + "')";
		}
		hql = hql + " order by id desc";

		int count = 0;
		count = this.userService.getTotalCount(countHql, new Object[0]);
		this.page = new BjuiPage(this.pageCurrent, this.pageSize);
		this.page.setTotalCount(count);
		this.cfg = new Configuration();

		this.cfg.setServletContextForTemplateLoading(this.request.getServletContext(), "WEB-INF/templates/admin");
		List<User> userList = this.userService.list(hql, this.page.getStart(), this.page.getPageSize(), new Object[0]);
		
		if (userList != null && userList.size() > 0) {
			for (User user: userList) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
				String newHql = "select sum(money) from Orders where shop.id = " + user.getId() + " and dateMonth = '" + sdf.format(new Date()) + "'";
				List<Orders> ordersList = this.ordersService.list(newHql);
				user.setTradeAmtMonth(ordersList.get(0).getMoney());
			}
		}
		Map root = new HashMap();
		root.put("userList", userList);
		root.put("page", this.page);
		root.put("key", key);
		FreemarkerUtils.freemarker(this.request, this.response, this.ftlFileName, this.cfg, root);
	}

	public void add() {
		this.cfg = new Configuration();

		this.cfg.setServletContextForTemplateLoading(this.request.getServletContext(), "WEB-INF/templates/admin");
		Map root = new HashMap();
		FreemarkerUtils.freemarker(this.request, this.response, this.ftlFileName, this.cfg, root);
	}

	public void register() {
		PrintWriter out = null;
		JSONObject json = new JSONObject();
		try {
			out = this.response.getWriter();
			String code = this.request.getParameter("code");// 短信验证码
//			if(!this.validationCode(code, user.getPhone())) {// 校验短信验证码
//				throw new Exception("验证码错误或者已失效");
//			} 
			String phone = user.getPhone();
			String password = user.getPassword();
			String password2 = user.getPassword2();
			String source = "XM";
			if ("1".equals(user.getType())) {// 商家用户
				String tuijianren = this.request.getParameter("tuijianren");// 推荐人
				User tjrUser = this.userService.getUserByNo(tuijianren);
				if (tjrUser == null) {
					throw new Exception("推荐人不存在");
				}
//				if (tjrUser.getStatus().intValue() == 0) {
//					throw new Exception("推荐人未激活");
//				}
//				User userTemp = this.userService.getUserByNoAndType(user.getPhone(), "0");
//				if (userTemp != null) {
//					throw new Exception("该手机已注册会员，无法注册");
//				}
				if (StringUtils.isEmpty(tjrUser.getSuperior())) {
					this.user.setSuperior(";" + tuijianren + ";");
				} else if (tjrUser.getSuperior().split(";").length > 3) {
					this.user.setSuperior(";" + tjrUser.getSuperior().split(";", 3)[2] + tuijianren + ";");
				} else {
					this.user.setSuperior(tjrUser.getSuperior() + tuijianren + ";");
				}
				user.setSuperNo(tuijianren);
			} else {// 个人用户
				user.setType("0");
				// 判断是否已经被注册成了商家
//				User userTemp = this.userService.getUserByNoAndType(user.getPhone(), "1");
//				if (userTemp != null) {
//					throw new Exception("该手机已注册商家，无法注册");
//				}
			}
			
			if (this.user == null) {
				throw new Exception("参数错误");
			}
			if (this.userService.getUserByName(this.user.getPhone()) != null) {
				throw new Exception("手机号已存在");
			}
			if (this.userService.getUserByOpenId(this.user.getOpenId()) != null) {
				throw new Exception("该微信号已经是会员");
			}
			String ip = null;
			try {
				ip = IpUtils.getIpAddress(this.request);
			} catch (Exception e) {
				this.user.setRegisterIp("0.0.0.0");
			}
			this.user.setNo(user.getPhone());
			this.user.setName(user.getPhone());
			this.user.setRegisterIp(ip);
			this.user.setPassword(Md5.getMD5Code(this.user.getPassword()));
			this.user.setLoginCount(Integer.valueOf(0));
			this.user.setStatus(Integer.valueOf(0));
			this.user.setBalance(Double.valueOf(0.0D));
			this.user.setCommission(Double.valueOf(0.0D));
			this.user.setDeleted(false);
			this.user.setCreateDate(new Date());
			boolean res = this.userService.saveOrUpdate(this.user);
			if (res) {
				User loginUser = this.userService.getUserByName(this.user.getName());
				loginUser.setLoginCount(Integer.valueOf(loginUser.getLoginCount().intValue() + 1));
				loginUser.setLastLoginTime(new Date());
				this.userService.register(loginUser, user.getType(), phone, password, password2, source, request);
				HttpSession session = this.request.getSession();
				session.setAttribute("loginUser", loginUser);
			} else {
				throw new Exception("注册失败，请重试");
			}
			json.put("status", "1");
			json.put("message", "注册成功");
		} catch (Exception e) {
			json.put("status", "0");
			json.put("message", e.getMessage());
		} finally {
			out.print(json.toString());
			out.flush();
			out.close();
		}
		
	}
	
	// 验证短信有效性
	private boolean validationCode(String code, String phone) {
		PhoneValidateCode findPhoneValidateCode = this.phoneValidateCodeService.getPhoneValidateCode(phone, code);
		if (findPhoneValidateCode == null) {// 验证码错误或者失效
			return false;
		}  else {
			long nowTime = System.currentTimeMillis();
			Date codeDateTime = findPhoneValidateCode.getCreateDate();
			long codeTime = codeDateTime.getTime();
			if (nowTime > codeTime + 600000L) {
				return false;
			} 
		}
		return true;
	}
	
	public void info() throws JSONException {
		String idStr = this.request.getParameter("id");
		String callbackData = "";
		PrintWriter out = null;
		try {
			out = this.response.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if ((idStr == null) || ("".equals(idStr))) {
			callbackData = BjuiJson.json("300", "参数不能为空", "", "", "", "", "", "");
		} else {
			int id = 0;
			try {
				id = Integer.parseInt(idStr);
			} catch (Exception e) {
				callbackData = BjuiJson.json("300", "参数错误", "", "", "", "", "", "");
			}
			User findUser = (User) this.userService.findById(User.class, id);
			if (findUser == null) {
				callbackData = BjuiJson.json("300", "用户不存在", "", "", "", "", "", "");
			} else {
				this.cfg = new Configuration();

				this.cfg.setServletContextForTemplateLoading(this.request.getServletContext(),
						"WEB-INF/templates/admin");
				Map root = new HashMap();
				root.put("user", findUser);
				FreemarkerUtils.freemarker(this.request, this.response, this.ftlFileName, this.cfg, root);
			}
		}
		out.print(callbackData);
		out.flush();
		out.close();
	}

	public void update() {
		PrintWriter out = null;
		try {
			out = this.response.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String callbackData = "";
		try {
			if (this.user == null) {
				callbackData = BjuiJson.json("300", "参数错误", "", "", "", "", "", "");
			} else {
				User findUser = (User) this.userService.findById(User.class, this.user.getId().intValue());
				if (StringUtils.isNotEmpty(this.user.getPassword())) {
					findUser.setPassword(Md5.getMD5Code(this.user.getPassword()));
				}
				boolean result = this.userService.saveOrUpdate(findUser);

				if (result) {
					callbackData = BjuiJson.json("200", "修改成功", "", "", "", "true", "", "");
				} else
					callbackData = BjuiJson.json("300", "修改失败", "", "", "", "", "", "");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		out.print(callbackData);
		out.flush();
		out.close();
	}

	public void delete() {
		PrintWriter out = null;
		try {
			out = this.response.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String callbackData = "";
		try {
			String idStr = this.request.getParameter("id");

			if ((idStr == null) || ("".equals(idStr))) {
				callbackData = BjuiJson.json("300", "参数错误", "", "", "", "", "", "");
			} else {
				int id = 0;
				try {
					id = Integer.parseInt(idStr);
				} catch (Exception e) {
					callbackData = BjuiJson.json("300", "参数错误", "", "", "", "", "", "");
				}
				if (id == 1) {
					callbackData = BjuiJson.json("300", "该用户不能删除", "", "", "", "true", "", "");
				} else {
					User findUser = (User) this.userService.findById(User.class, id);
					if (findUser == null) {
						callbackData = BjuiJson.json("300", "用户不存在", "", "", "", "true", "", "");
					} else {
						boolean result = this.userService.delete(findUser);
						if (result)
							callbackData = BjuiJson.json("200", "删除成功", "", "", "", "", "", "");
						else
							callbackData = BjuiJson.json("300", "删除失败", "", "", "", "", "", "");
					}
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		out.print(callbackData);
		out.flush();
		out.close();
	}

	public void login() {
		PrintWriter out = null;
		try {
			out = this.response.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		HttpSession session = this.request.getSession();
		JSONObject json = new JSONObject();
		if (this.user == null) {
			json.put("status", "0");
			json.put("message", "参数错误");
		} else {
			User loginUser = this.userService.login(this.user.getName(), Md5.getMD5Code(this.user.getPassword()));
			if (loginUser == null) {
				json.put("status", "0");
				json.put("message", "用户名或者密码错误");
			} else {
				loginUser.setLoginCount(Integer.valueOf(loginUser.getLoginCount().intValue() + 1));
				session.setAttribute("loginUser", loginUser);
				try {
					String ip = IpUtils.getIpAddress(this.request);
					loginUser.setLastLoginIp(ip);
				} catch (Exception e) {
					loginUser.setLastLoginIp("0.0.0.0");
				}
				loginUser.setLastLoginTime(new Date());
				this.userService.saveOrUpdate(loginUser);
				json.put("status", "1");
				json.put("message", "登录成功");
				json.put("type", loginUser.getType());// 用户类别： 0-个人会员； 1-商家会员
			}
		}
		out.print(json.toString());
		out.flush();
		out.close();
	}

	public void promote() {
		String no = this.request.getParameter("no");
		User findUser = this.userService.getUserByNo(no);
		if (findUser == null) {
			this.request.setAttribute("status", "0");
			this.request.setAttribute("message", "推广链接无效");
		} else if (findUser.getStatus().intValue() == 0) {
			this.request.setAttribute("status", "0");
			this.request.setAttribute("message", "推广链接无效");
		} else {
			this.request.setAttribute("status", "1");
			this.request.setAttribute("no", no);
		}
		try {
			this.request.getRequestDispatcher("promoteRegister.jsp").forward(this.request, this.response);
		} catch (ServletException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public void logout() throws IOException {
		HttpSession session = this.request.getSession();
		session.setAttribute("loginUser", null);
		this.response.sendRedirect("../login.jsp");
	}

	public void changePassword() {
		PrintWriter out = null;
		try {
			out = this.response.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String oldPassword = this.request.getParameter("oldPassword");
		String newPassword = this.request.getParameter("newPassword");
		HttpSession session = this.request.getSession();
		User loginUser = (User) session.getAttribute("loginUser");
		User findUser = (User) this.userService.findById(User.class, loginUser.getId().intValue());
		JSONObject json = new JSONObject();
		try {
			if (!StringUtils.equals(findUser.getPassword(), Md5.getMD5Code(oldPassword))) {
				json.put("status", "0");
				json.put("message", "旧密码错误");
			} else {
				findUser.setPassword(Md5.getMD5Code(newPassword));
				this.userService.saveOrUpdate(findUser);
				json.put("status", "1");
				json.put("message", "密码修改成功");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		out.print(json);
		out.flush();
		out.close();
	}

	public void resetPassword() {
		PrintWriter out = null;
		JSONObject json = new JSONObject();
		try {
			out = this.response.getWriter();
			String password = this.request.getParameter("password");
			String phone = this.request.getParameter("phone");
			String code = this.request.getParameter("code");
			if(!this.validationCode(code, user.getPhone())) {// 校验短信验证码
				throw new Exception("验证码错误或者已失效");
			} 
			User findUser = this.userService.getUserByPhone(phone);
			if (findUser == null) {
				throw new Exception("用户不存在");
			} else {
				findUser.setPassword(Md5.getMD5Code(password));
				this.userService.saveOrUpdate(findUser);
				json.put("status", "1");
				json.put("message", "密码重置成功");
			}
		} catch (Exception e) {
			json.put("status", "0");
			json.put("message", e.getMessage());
		} finally {
			out.print(json);
			out.flush();
			out.close();
		}
	}

	public void createUserNo() {
		User findUser = null;
		String no = "";
		do {
			Random random = new Random();
			int num = random.nextInt(899999) + 100000;
			this.user = this.userService.getUserByNo("" + num);
		} while (findUser != null);
		PrintWriter out = null;
		try {
			out = this.response.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		out.print(no);
		out.flush();
		out.close();
	}

	public void userInfoJson() {
		PrintWriter out = null;
		try {
			out = this.response.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		HttpSession session = this.request.getSession();
		User loginUser = (User) session.getAttribute("loginUser");
		User findUser = (User) this.userService.findById(User.class, loginUser.getId().intValue());
		JSONObject json = (JSONObject) JSONObject.toJSON(findUser);
		out.print(json.toString());
		out.flush();
		out.close();
	}

	public void commissionToBalance() {
		PrintWriter out = null;
		try {
			out = this.response.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		JSONObject json = new JSONObject();
		String moneyStr = this.request.getParameter("money");
		Double money = Double.valueOf(0.0D);
		try {
			money = Double.valueOf(Double.parseDouble(moneyStr));
		} catch (Exception e) {
			json.put("status", "0");
			json.put("message", "参数错误");
			out.print(json.toString());
			out.flush();
			out.close();
			return;
		}
		if (money.doubleValue() <= 0.0D) {
			json.put("status", "0");
			json.put("message", "金额必须大于0");
		} else {
			HttpSession session = this.request.getSession();
			User loginUser = (User) session.getAttribute("loginUser");
			User findUser = (User) this.userService.findById(User.class, loginUser.getId().intValue());
			if (money.doubleValue() > findUser.getCommission().doubleValue()) {
				json.put("status", "0");
				json.put("message", "佣金额度不足");
			} else {
				findUser.setBalance(Double.valueOf(findUser.getBalance().doubleValue() + money.doubleValue()));
				findUser.setCommission(Double.valueOf(findUser.getCommission().doubleValue() - money.doubleValue()));
				this.userService.saveOrUpdate(findUser);

				Financial financial = new Financial();
				financial.setType(Integer.valueOf(1));
				financial.setMoney(money);
				financial.setNo("" + System.currentTimeMillis());

				financial.setOperator(loginUser.getName());

				financial.setUser(findUser);

				financial.setCreateDate(new Date());
				financial.setDeleted(false);

				financial.setBalance(findUser.getBalance());
				financial.setPayment("佣金转入");
				financial.setRemark("佣金转入");
				this.financialService.saveOrUpdate(financial);
				json.put("status", "1");
				json.put("message", "转入成功");
			}
		}
		out.print(json.toString());
		out.flush();
		out.close();
	}

	public void balanceToUser() {
		PrintWriter out = null;
		try {
			out = this.response.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		JSONObject json = new JSONObject();
		String moneyStr = this.request.getParameter("money");
		String userno = this.request.getParameter("userno");
		Double money = Double.valueOf(0.0D);
		try {
			money = Double.valueOf(Double.parseDouble(moneyStr));
		} catch (Exception e) {
			json.put("status", "0");
			json.put("message", "参数错误");
			out.print(json.toString());
			out.flush();
			out.close();
			return;
		}
		User toUser = this.userService.getUserByNo(userno);
		if (toUser == null) {
			json.put("status", "0");
			json.put("message", "会员编号不存在");
		} else if (money.doubleValue() <= 0.0D) {
			json.put("status", "0");
			json.put("message", "金额必须大于0");
		} else {
			HttpSession session = this.request.getSession();
			User loginUser = (User) session.getAttribute("loginUser");
			User findUser = (User) this.userService.findById(User.class, loginUser.getId().intValue());
			if (money.doubleValue() > findUser.getBalance().doubleValue()) {
				json.put("status", "0");
				json.put("message", "货币额度不足");
			} else {
				findUser.setBalance(Double.valueOf(findUser.getBalance().doubleValue() - money.doubleValue()));
				toUser.setBalance(Double.valueOf(toUser.getBalance().doubleValue() + money.doubleValue()));
				this.userService.saveOrUpdate(findUser);
				this.userService.saveOrUpdate(toUser);

				Financial financial = new Financial();
				financial.setType(Integer.valueOf(0));
				financial.setMoney(Double.valueOf(-money.doubleValue()));
				financial.setNo("" + System.currentTimeMillis());

				financial.setOperator(loginUser.getName());

				financial.setUser(findUser);

				financial.setCreateDate(new Date());
				financial.setDeleted(false);

				financial.setBalance(findUser.getBalance());
				financial.setPayment("会员转账");
				financial.setRemark("会员转账，转入到会员编号【" + userno + "】");
				this.financialService.saveOrUpdate(financial);

				Financial financial2 = new Financial();
				financial2.setType(Integer.valueOf(1));
				financial2.setMoney(money);
				financial2.setNo("" + System.currentTimeMillis());

				financial2.setOperator(loginUser.getName());

				financial2.setUser(toUser);

				financial2.setCreateDate(new Date());
				financial2.setDeleted(false);

				financial2.setBalance(toUser.getBalance());
				financial2.setPayment("会员转账");
				financial2.setRemark("由会员编号【" + loginUser.getNo() + "】转入");
				this.financialService.saveOrUpdate(financial2);
				json.put("status", "1");
				json.put("message", "转入成功");
			}
		}

		out.print(json.toString());
		out.flush();
		out.close();
	}

	public void levelUserList() {
		HttpSession session = this.request.getSession();
		User loginUser = (User) session.getAttribute("loginUser");
		List<User> levelUserList = this.userService.levelUserList(loginUser.getNo());

		int firstLevelNum = 0;

		int secondLevelNum = 0;

		int thirdLevelNum = 0;

		int allLevelNum = levelUserList.size();

		int unStatusUserNum = 0;

		int todayRegUserNum = 0;

		int todayStatusUserNum = 0;

		for (User user : levelUserList) {
			if (user.getStatus().intValue() == 0) {
				unStatusUserNum++;
			}
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

			String todayDate = sdf.format(new Date());

			String createDate = sdf.format(user.getCreateDate());

			String statusDate = "";
			if (user.getStatusDate() != null) {
				statusDate = sdf.format(user.getStatusDate());
			}

			if (StringUtils.equals(createDate, todayDate)) {
				todayRegUserNum++;
			}

			if (StringUtils.equals(statusDate, todayDate)) {
				todayStatusUserNum++;
			}

			String levelNos = user.getSuperior();
			if (!StringUtils.isEmpty(levelNos)) {
				String[] leverNoArr = levelNos.split(";");
				int i = leverNoArr.length - 1;
				for (int j = 1; i > 0; j++) {
					if (!StringUtils.isEmpty(leverNoArr[i])) {
						User levelUser = this.userService.getUserByNo(leverNoArr[i]);
						if (levelUser != null) {
							if ((j == 1) && (StringUtils.equals(loginUser.getNo(), leverNoArr[i])))
								firstLevelNum++;
							else if ((j == 2) && (StringUtils.equals(loginUser.getNo(), leverNoArr[i])))
								secondLevelNum++;
							else if ((j == 3) && (StringUtils.equals(loginUser.getNo(), leverNoArr[i])))
								thirdLevelNum++;
						}
					}
					i--;
				}

			}

		}

		JSONObject json = new JSONObject();
		json.put("firstLevelNum", Integer.valueOf(firstLevelNum));
		json.put("secondLevelNum", Integer.valueOf(secondLevelNum));
		json.put("thirdLevelNum", Integer.valueOf(thirdLevelNum));
		json.put("allLevelNum", Integer.valueOf(allLevelNum));
		json.put("unStatusUserNum", Integer.valueOf(unStatusUserNum));
		json.put("todayRegUserNum", Integer.valueOf(todayRegUserNum));
		json.put("todayStatusUserNum", Integer.valueOf(todayStatusUserNum));
		PrintWriter out = null;
		try {
			out = this.response.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		out.print(json.toString());
		out.flush();
		out.close();
	}

	public User getUser() {
		return this.user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getFtlFileName() {
		return this.ftlFileName;
	}

	public void setFtlFileName(String ftlFileName) {
		this.ftlFileName = ftlFileName;
	}
	
	public void shopList() {
		String shopType = this.request.getParameter("shopType");
		String key = this.request.getParameter("key");
		String flag = (String)this.request.getSession().getAttribute("flag");
		Admin loginAdmin = (Admin)this.request.getSession().getAttribute("loginAdmin");
		String countHql = "select count(*) from User where deleted=0 and type != '0' ";
		
		String hql = "from User where deleted=0 and type != '0' ";
		if (StringUtils.isNotEmpty(key)) {
			countHql = countHql + " and (name='" + key + "' or no='" + key + "' or phone='" + key + "')";
			hql = hql + " and (name='" + key + "' or no='" + key + "' or phone='" + key + "')";
		}
		if (shopType != null && !"".equals(shopType)) {
			countHql = countHql + " and shopType='" + shopType +  "'";
			hql = hql + " and shopType='" + shopType +  "'";
		}
		hql = hql + " order by id desc";

		int count = 0;
		count = this.userService.getTotalCount(countHql, new Object[0]);
		this.page = new BjuiPage(this.pageCurrent, this.pageSize);
		this.page.setTotalCount(count);
		this.cfg = new Configuration();

		this.cfg.setServletContextForTemplateLoading(this.request.getServletContext(), "WEB-INF/templates/admin");
		List userList = this.userService.list(hql, this.page.getStart(), this.page.getPageSize(), new Object[0]);
		Map root = new HashMap();
		root.put("userList", userList);
		root.put("page", this.page);
		root.put("key", key);
		root.put("flag", flag);
		root.put("shopType", shopType);
		
		FreemarkerUtils.freemarker(this.request, this.response, this.ftlFileName, this.cfg, root);
	}
	
	public void subShopList() {
		String key = this.request.getParameter("key");
		String flag = (String)this.request.getSession().getAttribute("flag");
		String subFlag = "0";
		Admin loginAdmin = (Admin)this.request.getSession().getAttribute("loginAdmin");
		User user = this.userService.getUserByNo(loginAdmin.getName());
		if (user != null && ("3".equals(user.getType()) || "4".equals(user.getType()))) {
			subFlag = "1";
		}
		String countHql = "select count(*) from User where deleted=0 and type != '0' ";
		String hql = "from User where deleted=0 and type != '0' and superNo = '" + loginAdmin.getName() + "'";
		if (StringUtils.isNotEmpty(key)) {
			countHql = countHql + " and (name='" + key + "' or no='" + key + "' or phone='" + key + "')";
			hql = hql + " and (name='" + key + "' or no='" + key + "' or phone='" + key + "')";
		}
		hql = hql + " order by id desc";

		int count = 0;
		count = this.userService.getTotalCount(countHql, new Object[0]);
		this.page = new BjuiPage(this.pageCurrent, this.pageSize);
		this.page.setTotalCount(count);
		this.cfg = new Configuration();

		this.cfg.setServletContextForTemplateLoading(this.request.getServletContext(), "WEB-INF/templates/admin");
		List userList = this.userService.list(hql, this.page.getStart(), this.page.getPageSize(), new Object[0]);
		Map root = new HashMap();
		root.put("userList", userList);
		root.put("page", this.page);
		root.put("key", key);
		root.put("flag", flag);
		root.put("subFlag", subFlag);
		
		FreemarkerUtils.freemarker(this.request, this.response, this.ftlFileName, this.cfg, root);
	}
	
	// 再下级
	public void subSubShopList() {
		String key = this.request.getParameter("key");
		String id = this.request.getParameter("id");
		String subCount = this.request.getParameter("subCount");
		String subFlag = "0";
		Integer subCountInt = new Integer(1);
		if (subCount != null && !"".equals(subCount)) {
			subCountInt = subCountInt + new Integer(subCount);
		}
		if (subCountInt < 2) {
			subFlag = "1";
		}
		
		User userTemp = this.userService.findById(User.class, new Integer(id));
		
		String flag = (String)this.request.getSession().getAttribute("flag");
		String countHql = "select count(*) from User where deleted=0 and type != '0' and superNo = '" + userTemp.getNo() + "'";
		String hql = "from User where deleted=0 and type != '0' and superNo = '" + userTemp.getNo() + "'";
		if (StringUtils.isNotEmpty(key)) {
			countHql = countHql + " and (name='" + key + "' or no='" + key + "' or phone='" + key + "')";
			hql = hql + " and (name='" + key + "' or no='" + key + "' or phone='" + key + "')";
		}
		hql = hql + " order by id desc";

		int count = 0;
		count = this.userService.getTotalCount(countHql, new Object[0]);
		this.page = new BjuiPage(this.pageCurrent, this.pageSize);
		this.page.setTotalCount(count);
		this.cfg = new Configuration();

		this.cfg.setServletContextForTemplateLoading(this.request.getServletContext(), "WEB-INF/templates/admin");
		List userList = this.userService.list(hql, this.page.getStart(), this.page.getPageSize(), new Object[0]);
		Map root = new HashMap();
		root.put("userList", userList);
		root.put("page", this.page);
		root.put("key", key);
		root.put("flag", flag);
		root.put("subFlag", subFlag);
		root.put("subCount", subCountInt);
		
		FreemarkerUtils.freemarker(this.request, this.response, this.ftlFileName, this.cfg, root);
	}
	
	public void toUpgrade() throws JSONException {
		String idStr = this.request.getParameter("id");
		String callbackData = "";
		PrintWriter out = null;
		try {
			out = this.response.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if ((idStr == null) || ("".equals(idStr))) {
			callbackData = BjuiJson.json("300", "参数不能为空", "", "", "", "", "", "");
		} else {
			int id = 0;
			try {
				id = Integer.parseInt(idStr);
			} catch (Exception e) {
				callbackData = BjuiJson.json("300", "参数错误", "", "", "", "", "", "");
			}
			User findUser = (User) this.userService.findById(User.class, id);
			if (findUser == null) {
				callbackData = BjuiJson.json("300", "用户不存在", "", "", "", "", "", "");
			} else {
				this.cfg = new Configuration();

				this.cfg.setServletContextForTemplateLoading(this.request.getServletContext(),
						"WEB-INF/templates/admin");
				Map root = new HashMap();
				root.put("user", findUser);
				FreemarkerUtils.freemarker(this.request, this.response, this.ftlFileName, this.cfg, root);
			}
		}
		out.print(callbackData);
		out.flush();
		out.close();
	}
	
	public void upgrade() {
		PrintWriter out = null;
		try {
			out = this.response.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String callbackData = "";
		try {
			if (this.user == null) {
				callbackData = BjuiJson.json("300", "参数错误", "", "", "", "", "", "");
			} else {
				User findUser = (User) this.userService.findById(User.class, this.user.getId().intValue());
				if (StringUtils.isNotEmpty(this.user.getPassword())) {
					findUser.setPassword(Md5.getMD5Code(this.user.getPassword()));
				}
				findUser.setType(this.user.getType());
				boolean result = this.userService.saveOrUpdate(findUser);
				Admin admin = this.adminService.getAdminName(user.getName());
				if (admin == null) {
					admin = new Admin();
					admin.setCreateDate(new Date());
					admin.setDeleted(false);
//					admin.setId(id);
//					admin.setJuri(juri);
//					admin.setLastLoginIp(lastLoginIp);
//					admin.setLastLoginTime(lastLoginTime);
					admin.setLoginCount(0);
					admin.setName(findUser.getName());
					admin.setPassword(findUser.getPassword());
					admin.setStatus(1);
//					admin.setVersion(version);
					this.adminService.saveOrUpdate(admin);
				}
				
				if (result) {
					callbackData = BjuiJson.json("200", "修改成功", "", "", "", "true", "", "");
				} else
					callbackData = BjuiJson.json("300", "修改失败", "", "", "", "", "", "");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		out.print(callbackData);
		out.flush();
		out.close();
	}
	
}