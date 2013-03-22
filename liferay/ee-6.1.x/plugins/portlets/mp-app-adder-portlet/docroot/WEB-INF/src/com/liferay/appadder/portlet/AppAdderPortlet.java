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

package com.liferay.appadder.portlet;

import com.liferay.osb.model.AppEntry;
import com.liferay.osb.model.AppEntryRel;
import com.liferay.osb.model.AppPackage;
import com.liferay.osb.model.AppPackagePlugin;
import com.liferay.osb.model.AppVersion;
import com.liferay.osb.model.CorpEntry;
import com.liferay.osb.model.CurrencyEntry;
import com.liferay.osb.service.AppEntryLocalServiceUtil;
import com.liferay.osb.service.AppEntryRelLocalServiceUtil;
import com.liferay.osb.service.AppEntryRelServiceUtil;
import com.liferay.osb.service.AppEntryServiceUtil;
import com.liferay.osb.service.AppPackageLocalServiceUtil;
import com.liferay.osb.service.AppPackagePluginServiceUtil;
import com.liferay.osb.service.AppPackageServiceUtil;
import com.liferay.osb.service.AppVersionLocalServiceUtil;
import com.liferay.osb.service.AssetAttachmentLocalServiceUtil;
import com.liferay.osb.service.CurrencyEntryLocalServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.model.User;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.ServiceContextFactory;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.SystemProperties;
import com.liferay.util.bridges.mvc.MVCPortlet;

import java.io.File;
import java.io.IOException;

import java.util.List;
import java.util.Random;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;

/**
 * @author Douglas Wong
 */
public class AppAdderPortlet extends MVCPortlet {

	public void processAction(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws IOException, PortletException {

		try {
			int amount = ParamUtil.getInteger(actionRequest, "amount");
			String title = ParamUtil.getString(actionRequest, "title");

			for (int i = 0; i < amount; i++) {
				AppEntry appEntry = addAppEntry(actionRequest, title + " " + i);

				appEntry = updateAppVersion(appEntry.getAppEntryId());

				AppVersion appVersion =
					AppVersionLocalServiceUtil.getLatestAppVersion(
						appEntry.getAppEntryId());

				addAppPackage(appVersion);

				AppEntryServiceUtil.updateStatus(
					appEntry.getAppEntryId(), WorkflowConstants.STATUS_APPROVED,
					StringPool.BLANK);
			}
		}
		catch (Exception e) {
			throw new PortletException(e);
		}
	}

	protected AppEntry addAppEntry(ActionRequest actionRequest, String title)
		throws Exception {

		ThemeDisplay themeDisplay =
			(ThemeDisplay)actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

		boolean customEula = false;
		String description = "Lorem ipsum dolor sit amet, consectetur";
		String website = "http://www.liferay.com";
		String demoWebsite = "http://www.liferay.com";
		String documentationWebsite = "http://www.liferay.com";
		String sourceCodeWebsite = "http://www.liferay.com";
		String supportWebsite = "http://www.liferay.com";
		boolean labs = false;
		int productType = 0;
		double retailPrice = 0;
		String eulaContent = StringPool.BLANK;

		String defaultLanguageId = "en_US";
		String languageId = "en_US";

		String ownerClassName = CorpEntry.class.getName();
		long ownerClassPK = ParamUtil.getLong(actionRequest, "ownerClassPK");

		boolean descriptionLocalized = false;
		File iconFile = generateIcon();

		CurrencyEntry currencyEntry =
			CurrencyEntryLocalServiceUtil.getCurrencyEntry("USD");

		long currencyEntryId = currencyEntry.getCurrencyEntryId();

		ServiceContext serviceContext = ServiceContextFactory.getInstance(
			actionRequest);

		if (Validator.isNull(defaultLanguageId)) {
			defaultLanguageId = themeDisplay.getLanguageId();
		}

		if (ownerClassName.equals(User.class.getName())) {
			ownerClassPK = themeDisplay.getUserId();
		}

		description = LocalizationUtil.updateLocalization(
			StringPool.BLANK, "static-content", description, languageId,
			languageId, true, descriptionLocalized);

		AppEntry appEntry =
			AppEntryServiceUtil.addAppEntry(
				ownerClassName, ownerClassPK, title, description, website,
				demoWebsite, documentationWebsite, sourceCodeWebsite,
				supportWebsite, labs, productType, null, null, iconFile,
				currencyEntryId, retailPrice, eulaContent, serviceContext);

		long[] assetAttachmentIds = StringUtil.split(
			ParamUtil.getString(actionRequest, "assetAttachmentIds"), 0L);

		AppVersion appVersion = AppVersionLocalServiceUtil.getLatestAppVersion(
			appEntry.getAppEntryId());

		for (long assetAttachmentId : assetAttachmentIds) {
			AssetAttachmentLocalServiceUtil.updateAssetAttachment(
				assetAttachmentId, AppVersion.class.getName(),
				appVersion.getAppVersionId());
		}

		List<AppEntryRel> appEntryRels =
			AppEntryRelLocalServiceUtil.getAppEntryRels(
				appEntry.getAppEntryId(), 1);

		for (AppEntryRel appEntryRel : appEntryRels) {
			AppEntryRelServiceUtil.deleteAppEntryRel(
				appEntryRel.getAppEntryRelId());
		}

		long[] supersedesAppEntryIds =
			StringUtil.split(
				ParamUtil.getString(actionRequest, "supersedesAppEntryIds"),
				0L);

		for (long supersedesAppEntryId : supersedesAppEntryIds) {
			AppEntryRelServiceUtil.addAppEntryRel(
				appEntry.getAppEntryId(), supersedesAppEntryId, 1);
		}

		return appEntry;
	}

	protected AppEntry updateAppVersion(long appEntryId)
		throws PortalException, SystemException {

		return AppEntryLocalServiceUtil.updateAppEntry(
			appEntryId, "1.0", StringPool.BLANK);
	}

	protected void addAppPackage(AppVersion appVersion)
		throws Exception {

		AppPackage appPackage = AppPackageLocalServiceUtil.fetchAppPackage(
			appVersion.getAppVersionId(), PORTAL_6_1_20_BUILD_NUMBER);

		if (appPackage == null) {
			appPackage = AppPackageServiceUtil.addAppPackage(
				appVersion.getAppEntryId(), appVersion.getAppVersionId(),
				PORTAL_6_1_20_BUILD_NUMBER, true);
		}

		AppPackagePlugin appPackagePlugin = null;

		appPackagePlugin = AppPackagePluginServiceUtil.addAppPackagePlugin(
			appPackage.getAppPackageId(), APP_FILE_NAME,
			getFile("/resources/files/", APP_FILE_NAME));
	}

	protected File generateIcon() throws Exception {
		Random random = new Random();

		return getFile(
			"/resources/images/icons/",
			APP_ICON_FILE_NAMES[random.nextInt(APP_ICON_FILE_NAMES.length)]);
	}

	protected File getFile(String path, String fileName) throws Exception {
		String tempDir = SystemProperties.get(SystemProperties.TMP_DIR);

		File file = new File(tempDir + File.separator + fileName);

		ClassLoader classLoader = getClass().getClassLoader();

		FileUtil.write(file, classLoader.getResourceAsStream(path + fileName));

		return file;
	}

	protected static final String APP_FILE_NAME =
		"hello-pacl-world-portlet-6.1.20.1.war";

	protected static final String[] APP_ICON_FILE_NAMES = {
		"7COGS-HOOK.png", "7COGS-MOBILE.png", "7COGS-THEME2.png", "alloy.png",
		"app.png", "chat.png", "GOOGLE ADSENSE.png", "GOOGLE-MAPS.png",
		"hook-templates.png", "icon-alloy.png", "icon-so.png",
		"icon-studio.png", "icon-wallet.png", "kaleo.png", "mail.png",
		"open-social.png", "pencil.png", "planner.png", "social-office.png",
		"solr.png", "talk.png", "theme.png", "twitter.png", "web forms.png"
		};

	protected static final int PORTAL_6_1_20_BUILD_NUMBER = 6120;

}