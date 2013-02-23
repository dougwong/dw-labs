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

import com.liferay.osb.model.ApplicationEntry;
import com.liferay.osb.model.ApplicationEntryRel;
import com.liferay.osb.model.ApplicationPackage;
import com.liferay.osb.model.ApplicationPackagePlugin;
import com.liferay.osb.model.ApplicationVersion;
import com.liferay.osb.model.CorpEntry;
import com.liferay.osb.model.CurrencyEntry;
import com.liferay.osb.service.ApplicationEntryLocalServiceUtil;
import com.liferay.osb.service.ApplicationEntryRelLocalServiceUtil;
import com.liferay.osb.service.ApplicationEntryRelServiceUtil;
import com.liferay.osb.service.ApplicationEntryServiceUtil;
import com.liferay.osb.service.ApplicationPackageLocalServiceUtil;
import com.liferay.osb.service.ApplicationPackagePluginServiceUtil;
import com.liferay.osb.service.ApplicationPackageServiceUtil;
import com.liferay.osb.service.ApplicationVersionLocalServiceUtil;
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
				ApplicationEntry applicationEntry = addApplicationEntry(
					actionRequest, title + " " + i);

				applicationEntry = updateApplicationVersion(
					applicationEntry.getApplicationEntryId());

				ApplicationVersion applicationVersion =
					ApplicationVersionLocalServiceUtil.
						getLatestApplicationVersion(
							applicationEntry.getApplicationEntryId());

				addApplicationPackage(applicationVersion);

				ApplicationEntryServiceUtil.updateStatus(
					applicationEntry.getApplicationEntryId(),
					WorkflowConstants.STATUS_APPROVED, StringPool.BLANK);
			}
		}
		catch (Exception e) {
			throw new PortletException(e);
		}
	}

	protected ApplicationEntry addApplicationEntry(
			ActionRequest actionRequest, String title)
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

		ApplicationEntry applicationEntry =
			ApplicationEntryServiceUtil.addApplicationEntry(
				ownerClassName, ownerClassPK, title, description, website,
				demoWebsite, documentationWebsite, sourceCodeWebsite,
				supportWebsite, labs, productType, null, null, iconFile,
				currencyEntryId, retailPrice, eulaContent, serviceContext);

		long[] assetAttachmentIds = StringUtil.split(
			ParamUtil.getString(actionRequest, "assetAttachmentIds"), 0L);

		ApplicationVersion applicationVersion =
			ApplicationVersionLocalServiceUtil.getLatestApplicationVersion(
				applicationEntry.getApplicationEntryId());

		for (long assetAttachmentId : assetAttachmentIds) {
			AssetAttachmentLocalServiceUtil.updateAssetAttachment(
				assetAttachmentId, ApplicationVersion.class.getName(),
				applicationVersion.getApplicationVersionId());
		}

		List<ApplicationEntryRel> applicationEntryRels =
			ApplicationEntryRelLocalServiceUtil.getApplicationEntryRels(
				applicationEntry.getApplicationEntryId(),
				1);

		for (ApplicationEntryRel applicationEntryRel : applicationEntryRels) {
			ApplicationEntryRelServiceUtil.deleteApplicationEntryRel(
				applicationEntryRel.getApplicationEntryRelId());
		}

		long[] supersedesApplicationEntryIds =
			StringUtil.split(
				ParamUtil.getString(
					actionRequest, "supersedesApplicationEntryIds"), 0L);

		for (long supersedesApplicationEntryId :
				supersedesApplicationEntryIds) {

			ApplicationEntryRelServiceUtil.addApplicationEntryRel(
				applicationEntry.getApplicationEntryId(),
				supersedesApplicationEntryId, 1);
		}

		return applicationEntry;
	}

	protected ApplicationEntry updateApplicationVersion(long applicationEntryId)
		throws PortalException, SystemException {

		return ApplicationEntryLocalServiceUtil.updateApplicationEntry(
			applicationEntryId, "1.0", StringPool.BLANK);
	}

	protected void addApplicationPackage(ApplicationVersion applicationVersion)
		throws Exception {

		ApplicationPackage applicationPackage =
			ApplicationPackageLocalServiceUtil.fetchApplicationPackage(
				applicationVersion.getApplicationVersionId(),
				PORTAL_6_1_20_BUILD_NUMBER);

		if (applicationPackage == null) {
			applicationPackage =
				ApplicationPackageServiceUtil.addApplicationPackage(
					applicationVersion.getApplicationEntryId(),
					applicationVersion.getApplicationVersionId(),
					PORTAL_6_1_20_BUILD_NUMBER, true);
		}

		ApplicationPackagePlugin applicationPackagePlugin = null;

		applicationPackagePlugin =
			ApplicationPackagePluginServiceUtil.
				addApplicationPackagePlugin(
					applicationPackage.getApplicationPackageId(),
					APP_FILE_NAME, getFile("/resources/files/", APP_FILE_NAME));
	}

	protected File generateIcon() throws Exception {
		Random random = new Random();

		return getFile(
			"/resources/images/icons/",
			APPLICATION_ICON_FILE_NAMES[
				random.nextInt(APPLICATION_ICON_FILE_NAMES.length)]);
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

	protected static final String[] APPLICATION_ICON_FILE_NAMES = {
		"7COGS-HOOK.png", "7COGS-MOBILE.png", "7COGS-THEME2.png", "alloy.png",
		"app.png", "chat.png", "GOOGLE ADSENSE.png", "GOOGLE-MAPS.png",
		"hook-templates.png", "icon-alloy.png", "icon-so.png",
		"icon-studio.png", "icon-wallet.png", "kaleo.png", "mail.png",
		"open-social.png", "pencil.png", "planner.png", "social-office.png",
		"solr.png", "talk.png", "theme.png", "twitter.png", "web forms.png"
		};

	protected static final int PORTAL_6_1_20_BUILD_NUMBER = 6120;

}