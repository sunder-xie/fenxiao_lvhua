<div class="bjui-pageHeader">
    <form id="pagerForm" data-toggle="ajaxsearch" action="subShopList" method="post">
        <ul class="bjui-searchBar">
            <li><label>关键词：</label>
            <input type="text" id="key" name="key"  size="15" value="${key!''}"/>
            <label>商户类型：</label>
            <select name="shopType">
            	<option value="">请选择</option>
            	<option value="4" <c:if test="${shopType==4}">selected</c:if>>铂金商户</option>
            	<option value="3" <c:if test="${shopType==3}">selected</c:if>>白金商户</option>
            	<option value="2" <c:if test="${shopType==2}">selected</c:if>>VIP商户</option>
            	<option value="1" <c:if test="${shopType==1}">selected</c:if>>普通商户</option>
            </select>
            <button type="submit" class="btn-default" data-icon="search">查询</button>
            <a class="btn btn-orange" href="javascript:;" data-toggle="reloadsearch" data-clear-query="true" data-icon="undo">清空查询</a>可以搜索商户手机号</li>
        </ul>
    </form>
</div>
<div class="bjui-pageContent">
    <table data-toggle="tablefixed" data-width="100%" data-layout-h="0" data-nowrap="true">
        <thead>
			<tr>
             	<th>商户帐号</th>
                <th>微信账号</th>
                <th>当月交易额</th>
                <th>商户类别</th>
                <th>注册时间</th>
              	<th width="150">操作</th>
			</tr>
		</thead>
		<tbody>
        <#list userList as user>
            <tr>
                <td>${user.no}</td>
                <td>${user.nickname}</td>
                <td>${user.commission!""}</td>
                <td>${user.typeName}</td>
                <td>${user.createDate}</td>
                <td>
					<a href="toUpgrade.action?id=${user.id?string('#')}" class="btn btn-green" data-toggle="dialog" data-width="800" data-height="400" data-id="toUpgrade" data-mask="true">升级</a>
                </td>
            </tr>
		</#list>
		</tbody>
    </table>
    <#include "pageBar.ftl"/>
</div>