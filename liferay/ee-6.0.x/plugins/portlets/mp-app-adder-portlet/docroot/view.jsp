<%--
/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
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

<%@ include file="/init.jsp" %>

<%
String redirect = ParamUtil.getString(request, "redirect");
%>

<p>This is the <b>Marketplace App Adder</b> portlet.</p>

<aui:form action="<%= renderResponse.createActionURL() %>" method="post" name="fm">
	<aui:fieldset>
		<div class="title">
			<aui:input name="title" />
		</div>

		<div class="amount">
			<aui:input name="amount" />
		</div>

		<div class="category">
			<aui:select label="category" name="assetCategoryIds">
				<aui:option />

				<%
				AssetVocabulary assetVocabulary = AssetVocabularyLocalServiceUtil.getGroupVocabulary(GROUP_GUEST_ID, "Marketplace");

				List<AssetCategory> assetCategories = AssetCategoryLocalServiceUtil.getVocabularyRootCategories(assetVocabulary.getVocabularyId(), QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);

				for (AssetCategory assetCategory : assetCategories) {
					if (assetCategory.getCategoryId() == ASSET_CATEGORY_EE_PLUGINS_ID) {
						continue;
					}
				%>

					<optgroup label="<%= assetCategory.getTitle(themeDisplay.getLanguageId(), true) %>">

						<%
						List<AssetCategory> subassetCategories = AssetCategoryLocalServiceUtil.getChildCategories(assetCategory.getCategoryId());

						for (AssetCategory subassetCategory : subassetCategories) {
							String value = assetCategory.getCategoryId() + StringPool.COMMA + subassetCategory.getCategoryId();
						%>

							<aui:option label="<%= subassetCategory.getTitle(themeDisplay.getLanguageId(), true) %>" value="<%= value %>" />

						<%
						}
						%>

					</optgroup>

				<%
				}
				%>

			</aui:select>
		</div>

		<div class="corp-entry">
			<aui:select label="company" name="ownerClassPK">
				<aui:option />

				<%
				List<CorpEntry> corpEntries = CorpEntryLocalServiceUtil.getUserCorpEntries(user.getUserId());

				for (CorpEntry corpEntry : corpEntries) {
				%>

					<aui:option label="<%= HtmlUtil.escape(corpEntry.getName()) %>" value="<%= corpEntry.getCorpEntryId() %>" />

				<%
				}
				%>

			</aui:select>
		</div>
	</aui:fieldset>

	<aui:button-row>
		<aui:button type="submit" />

		<aui:button href="<%= redirect %>" type="cancel" />
	</aui:button-row>
</aui:form>