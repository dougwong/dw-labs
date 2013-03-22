<%--
/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
--%>

<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>

<%@ taglib uri="http://liferay.com/tld/aui" prefix="aui" %>
<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>

<%@ page import="com.liferay.osb.model.CorpEntry" %><%@
page import="com.liferay.osb.service.CorpEntryLocalServiceUtil" %><%@
page import="com.liferay.portal.kernel.dao.orm.QueryUtil" %><%@
page import="com.liferay.portal.kernel.util.HtmlUtil" %><%@
page import="com.liferay.portal.kernel.util.ParamUtil" %><%@
page import="com.liferay.portal.kernel.util.StringPool" %><%@
page import="com.liferay.portal.model.Group" %><%@
page import="com.liferay.portal.service.GroupLocalServiceUtil" %><%@
page import="com.liferay.portlet.asset.model.AssetCategory" %><%@
page import="com.liferay.portlet.asset.model.AssetVocabulary" %><%@
page import="com.liferay.portlet.asset.service.AssetCategoryLocalServiceUtil" %><%@
page import="com.liferay.portlet.asset.service.AssetVocabularyLocalServiceUtil" %>

<%@ page import="java.util.List" %>

<portlet:defineObjects />

<liferay-theme:defineObjects />

<%
	Group guestGroup = GroupLocalServiceUtil.getGroup(themeDisplay.getCompanyId(), "Guest");
	long GROUP_GUEST_ID = guestGroup.getGroupId();
	long ASSET_CATEGORY_EE_PLUGINS_ID = 0;

	String[] stringArray = new String[0];

	List<AssetCategory> eeAssetCategories = AssetCategoryLocalServiceUtil.search(GROUP_GUEST_ID, "EE Apps & Themes", stringArray, 0, 1);

	if (eeAssetCategories.size() > 0) {
		ASSET_CATEGORY_EE_PLUGINS_ID = eeAssetCategories.get(0).getCategoryId();
	}
%>