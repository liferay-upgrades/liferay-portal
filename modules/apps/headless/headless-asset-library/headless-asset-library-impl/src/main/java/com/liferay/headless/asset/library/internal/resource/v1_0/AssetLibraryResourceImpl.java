/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.headless.asset.library.internal.resource.v1_0;

import com.liferay.depot.constants.DepotActionKeys;
import com.liferay.depot.constants.DepotRolesConstants;
import com.liferay.depot.model.DepotAppCustomization;
import com.liferay.depot.model.DepotEntry;
import com.liferay.depot.model.DepotEntryGroupRel;
import com.liferay.depot.model.DepotEntryPin;
import com.liferay.depot.service.DepotAppCustomizationLocalService;
import com.liferay.depot.service.DepotEntryGroupRelService;
import com.liferay.depot.service.DepotEntryPinLocalService;
import com.liferay.depot.service.DepotEntryPinService;
import com.liferay.depot.service.DepotEntryService;
import com.liferay.document.library.configuration.DLSizeLimitConfigurationProvider;
import com.liferay.expando.kernel.model.ExpandoBridge;
import com.liferay.expando.kernel.model.ExpandoColumnConstants;
import com.liferay.exportimport.kernel.configuration.ExportImportConfigurationParameterMapFactory;
import com.liferay.exportimport.kernel.configuration.ExportImportConfigurationSettingsMapFactory;
import com.liferay.exportimport.kernel.configuration.constants.ExportImportConfigurationConstants;
import com.liferay.exportimport.kernel.lar.PortletDataHandlerKeys;
import com.liferay.exportimport.kernel.service.ExportImportConfigurationLocalService;
import com.liferay.exportimport.kernel.service.ExportImportLocalService;
import com.liferay.headless.asset.library.dto.v1_0.AssetLibrary;
import com.liferay.headless.asset.library.dto.v1_0.MimeTypeLimit;
import com.liferay.headless.asset.library.dto.v1_0.Settings;
import com.liferay.headless.asset.library.internal.odata.entity.v1_0.AssetLibraryEntityModel;
import com.liferay.headless.asset.library.internal.util.AssetLibraryUtil;
import com.liferay.headless.asset.library.resource.v1_0.AssetLibraryResource;
import com.liferay.petra.function.UnsafeSupplier;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.DuplicateGroupExternalReferenceCodeException;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.UserGroup;
import com.liferay.portal.kernel.model.UserGroupGroupRole;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.search.filter.Filter;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.security.permission.resource.ModelResourcePermission;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.service.UserGroupGroupRoleLocalService;
import com.liferay.portal.kernel.service.UserGroupGroupRoleService;
import com.liferay.portal.kernel.service.UserGroupLocalService;
import com.liferay.portal.kernel.service.UserGroupRoleService;
import com.liferay.portal.kernel.service.UserGroupService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.service.UserService;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.UnicodePropertiesBuilder;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.odata.entity.EntityModel;
import com.liferay.portal.vulcan.dto.converter.DTOConverter;
import com.liferay.portal.vulcan.dto.converter.DTOConverterRegistry;
import com.liferay.portal.vulcan.dto.converter.DefaultDTOConverterContext;
import com.liferay.portal.vulcan.fields.NestedFieldsSupplier;
import com.liferay.portal.vulcan.pagination.Page;
import com.liferay.portal.vulcan.pagination.Pagination;
import com.liferay.portal.vulcan.permission.Permission;
import com.liferay.portal.vulcan.util.LocalizedMapUtil;
import com.liferay.portal.vulcan.util.SearchUtil;
import com.liferay.sharing.constants.SharingConfigurationConstants;

import jakarta.ws.rs.core.MultivaluedMap;

import java.io.File;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * @author Roberto Díaz
 */
@Component(
	properties = "OSGI-INF/liferay/rest/v1_0/asset-library.properties",
	scope = ServiceScope.PROTOTYPE, service = AssetLibraryResource.class
)
public class AssetLibraryResourceImpl extends BaseAssetLibraryResourceImpl {

	@Override
	public void deleteAssetLibrary(String assetLibraryExternalReferenceCode)
		throws Exception {

		DepotEntry depotEntry = _getGroupDepotEntry(
			_getGroupIdByExternalReferenceCode(
				assetLibraryExternalReferenceCode));

		_depotEntryService.deleteDepotEntry(depotEntry.getDepotEntryId());
	}

	@Override
	public void deleteAssetLibraryPin(String assetLibraryExternalReferenceCode)
		throws Exception {

		DepotEntry depotEntry = _getGroupDepotEntry(
			_getGroupIdByExternalReferenceCode(
				assetLibraryExternalReferenceCode));

		_depotEntryPinService.deleteDepotEntryPin(
			contextUser.getUserId(), depotEntry.getDepotEntryId());
	}

	@Override
	public Page<AssetLibrary> getAssetLibrariesPinnedByMePage(
			Pagination pagination)
		throws Exception {

		List<AssetLibrary> assetLibraries = new ArrayList<>(
			pagination.getPageSize());

		List<DepotEntryPin> depotEntryPins =
			_depotEntryPinLocalService.getUserDepotEntryPins(
				contextUser.getUserId(), pagination.getStartPosition(),
				pagination.getEndPosition());

		for (DepotEntryPin depotEntryPin : depotEntryPins) {
			assetLibraries.add(
				_toAssetLibrary(
					_depotEntryService.getDepotEntry(
						depotEntryPin.getDepotEntryId())));
		}

		return Page.of(
			assetLibraries, pagination,
			_depotEntryPinLocalService.getUserDepotEntryPinsCount(
				contextUser.getUserId()));
	}

	@Override
	public EntityModel getEntityModel(MultivaluedMap multivaluedMap) {
		return _assetLibraryEntityModel;
	}

	@Override
	public AssetLibrary patchAssetLibrary(
			String assetLibraryExternalReferenceCode, AssetLibrary assetLibrary)
		throws Exception {

		DepotEntry depotEntry = _getGroupDepotEntry(
			_getGroupIdByExternalReferenceCode(
				assetLibraryExternalReferenceCode));

		Group group = depotEntry.getGroup();

		String name = _getValue(
			() -> group.getName(contextAcceptLanguage.getPreferredLocale()),
			assetLibrary::getName);
		String description = _getValue(
			() -> group.getDescription(
				contextAcceptLanguage.getPreferredLocale()),
			assetLibrary::getDescription);

		if (assetLibrary.getSettings() == null) {
			assetLibrary.setSettings(Settings::new);
		}

		UnicodeProperties unicodeProperties = _patchUnicodeProperties(
			assetLibrary.getSettings(),
			_getUnicodeProperties(
				contextCompany.getCompanyId(),
				group.getExternalReferenceCode()));

		AssetLibrary updatedAssetLibrary = _toAssetLibrary(
			_addOrUpdateDepotEntry(
				assetLibrary,
				LocalizedMapUtil.patchLocalizedMap(
					group.getDescriptionMap(),
					contextAcceptLanguage.getPreferredLocale(), description,
					assetLibrary.getDescription_i18n()),
				group.getExternalReferenceCode(),
				LocalizedMapUtil.patchLocalizedMap(
					group.getNameMap(),
					contextAcceptLanguage.getPreferredLocale(), name,
					assetLibrary.getName_i18n()),
				_getServiceContext(), unicodeProperties,
				_dlSizeLimitConfigurationProvider.getGroupMimeTypeSizeLimit(
					group.getGroupId())));

		Permission[] permissions = assetLibrary.getPermissions();

		if (permissions != null) {
			Page<Permission> permissionsPage = putAssetLibraryPermissionsPage(
				updatedAssetLibrary.getExternalReferenceCode(), permissions);

			updatedAssetLibrary.setPermissions(
				() -> NestedFieldsSupplier.supply(
					"permissions",
					nestedField -> {
						Collection<Permission> collection =
							permissionsPage.getItems();

						return collection.toArray(new Permission[0]);
					}));
		}

		return updatedAssetLibrary;
	}

	@Override
	public AssetLibrary postAssetLibraryCopy(
			String assetLibraryExternalReferenceCode, AssetLibrary assetLibrary)
		throws Exception {

		DepotEntry sourceDepotEntry = _depotEntryService.getGroupDepotEntry(
			_getGroupIdByExternalReferenceCode(
				assetLibraryExternalReferenceCode));

		_checkDuplicateExternalReferenceCode(
			assetLibrary.getExternalReferenceCode());

		if (assetLibrary.getSettings() == null) {
			assetLibrary.setSettings(Settings::new);
		}

		if (assetLibrary.getType() == null) {
			assetLibrary.setType(
				() -> AssetLibraryUtil.getAssetLibraryType(
					sourceDepotEntry.getType()));
		}

		ServiceContext serviceContext = _getServiceContext();

		Group sourceGroup = sourceDepotEntry.getGroup();

		DepotEntry depotEntry = _addOrUpdateDepotEntry(
			assetLibrary,
			_getLocalizedMap(
				assetLibrary.getDescription(),
				assetLibrary.getDescription_i18n()),
			assetLibrary.getExternalReferenceCode(),
			_getLocalizedMap(
				assetLibrary.getName(), assetLibrary.getName_i18n()),
			serviceContext,
			_patchUnicodeProperties(
				assetLibrary.getSettings(),
				sourceGroup.getTypeSettingsProperties()),
			_dlSizeLimitConfigurationProvider.getGroupMimeTypeSizeLimit(
				sourceGroup.getGroupId()));

		_copyAssets(depotEntry.getGroupId(), sourceGroup.getGroupId());

		_copyConnectedSites(depotEntry, sourceDepotEntry);

		_copyDepotAppCustomizations(depotEntry, sourceDepotEntry);

		_copyMembers(
			depotEntry.getGroupId(), serviceContext, sourceGroup.getGroupId());

		return _toAssetLibrary(depotEntry);
	}

	@Override
	public AssetLibrary putAssetLibraryPin(
			String assetLibraryExternalReferenceCode)
		throws Exception {

		DepotEntry depotEntry = _getGroupDepotEntry(
			_getGroupIdByExternalReferenceCode(
				assetLibraryExternalReferenceCode));

		_depotEntryPinService.addDepotEntryPin(
			contextUser.getUserId(), depotEntry.getDepotEntryId());

		return _toAssetLibrary(depotEntry);
	}

	@Override
	protected Page<AssetLibrary> doGetAssetLibrariesPage(
			String keywords, String search, Filter filter,
			Pagination pagination, Sort[] sorts)
		throws Exception {

		return SearchUtil.search(
			Collections.emptyMap(),
			booleanQuery -> {
			},
			filter, DepotEntry.class.getName(), keywords, pagination,
			queryConfig -> {
			},
			searchContext -> {
				searchContext.setCompanyId(contextCompany.getCompanyId());

				if (Validator.isNotNull(search)) {
					searchContext.setKeywords(search);
				}
			},
			sorts,
			document -> _toAssetLibrary(
				_depotEntryService.getDepotEntry(
					GetterUtil.getLong(document.get(Field.ENTRY_CLASS_PK)))));
	}

	@Override
	protected AssetLibrary doGetAssetLibrary(
			String assetLibraryExternalReferenceCode)
		throws Exception {

		return _toAssetLibrary(
			_depotEntryService.getGroupDepotEntry(
				_getGroupIdByExternalReferenceCode(
					assetLibraryExternalReferenceCode)));
	}

	@Override
	protected AssetLibrary doPostAssetLibrary(AssetLibrary assetLibrary)
		throws Exception {

		String externalReferenceCode = assetLibrary.getExternalReferenceCode();

		_checkDuplicateExternalReferenceCode(externalReferenceCode);

		return _toAssetLibrary(
			_addOrUpdateDepotEntry(
				assetLibrary,
				_getLocalizedMap(
					assetLibrary.getDescription(),
					assetLibrary.getDescription_i18n()),
				externalReferenceCode,
				_getLocalizedMap(
					assetLibrary.getName(), assetLibrary.getName_i18n()),
				_getServiceContext(),
				_putUnicodeProperties(assetLibrary.getSettings()),
				new LinkedHashMap<>()));
	}

	@Override
	protected AssetLibrary doPutAssetLibrary(
			String assetLibraryExternalReferenceCode, AssetLibrary assetLibrary)
		throws Exception {

		return _toAssetLibrary(
			_addOrUpdateDepotEntry(
				assetLibrary,
				_getLocalizedMap(
					assetLibrary.getDescription(),
					assetLibrary.getDescription_i18n()),
				assetLibraryExternalReferenceCode,
				_getLocalizedMap(
					assetLibrary.getName(), assetLibrary.getName_i18n()),
				_getServiceContext(),
				_putUnicodeProperties(assetLibrary.getSettings()),
				new LinkedHashMap<>()));
	}

	@Override
	protected Long getPermissionCheckerGroupId(
			String groupExternalReferenceCode)
		throws Exception {

		Group group = _groupLocalService.fetchGroupByExternalReferenceCode(
			groupExternalReferenceCode, contextCompany.getCompanyId());

		return group.getGroupId();
	}

	@Override
	protected Long getPermissionCheckerResourceId(String externalReferenceCode)
		throws Exception {

		DepotEntry depotEntry = _depotEntryService.getGroupDepotEntry(
			getPermissionCheckerGroupId(externalReferenceCode));

		return depotEntry.getDepotEntryId();
	}

	@Override
	protected String getPermissionCheckerResourceName(
			String externalReferenceCode)
		throws Exception {

		return DepotEntry.class.getName();
	}

	private DepotEntry _addOrUpdateDepotEntry(
			AssetLibrary assetLibrary, Map<Locale, String> descriptionMap,
			String externalReferenceCode, Map<Locale, String> nameMap,
			ServiceContext serviceContext, UnicodeProperties unicodeProperties,
			Map<String, Long> mimeTypeSizeLimits)
		throws Exception {

		if (assetLibrary.getSettings() == null) {
			assetLibrary.setSettings(Settings::new);
		}

		Group group = null;

		if (Validator.isNotNull(externalReferenceCode)) {
			group = _groupLocalService.fetchGroupByExternalReferenceCode(
				externalReferenceCode, serviceContext.getCompanyId());
		}

		if (group != null) {
			DepotEntry depotEntry = _depotEntryService.getGroupDepotEntry(
				group.getGroupId());

			if (Validator.isNotNull(assetLibrary.getExternalReferenceCode()) &&
				!externalReferenceCode.equals(
					assetLibrary.getExternalReferenceCode())) {

				group = depotEntry.getGroup();

				group.setExternalReferenceCode(
					assetLibrary.getExternalReferenceCode());

				group = _groupLocalService.updateGroup(group);
			}

			DepotEntry updatedDepotEntry = _depotEntryService.updateDepotEntry(
				depotEntry.getDepotEntryId(), nameMap, descriptionMap,
				_getDepotAppCustomizationMap(
					depotEntry.getCompanyId(), externalReferenceCode),
				assetLibrary.getFriendlyURL(),
				UnicodePropertiesBuilder.create(
					group.getTypeSettingsProperties(), true
				).putAll(
					unicodeProperties
				).build(),
				serviceContext);

			_updateDLSizeLimitConfiguration(
				assetLibrary, group.getGroupId(), mimeTypeSizeLimits);

			return updatedDepotEntry;
		}

		if (Validator.isNotNull(externalReferenceCode)) {
			serviceContext.setAttribute(
				"groupExternalReferenceCode", externalReferenceCode);
		}

		DepotEntry depotEntry = _depotEntryService.addDepotEntry(
			nameMap, descriptionMap,
			AssetLibraryUtil.getDepotEntryType(
				GetterUtil.getObject(
					assetLibrary.getType(),
					() -> AssetLibrary.Type.ASSET_LIBRARY)),
			serviceContext);

		group = depotEntry.getGroup();

		if ((unicodeProperties != null) && !unicodeProperties.isEmpty()) {
			_groupLocalService.updateGroup(
				group.getGroupId(),
				UnicodePropertiesBuilder.create(
					group.getTypeSettingsProperties(), true
				).putAll(
					unicodeProperties
				).buildString());
		}

		_updateFriendlyURL(assetLibrary, group.getGroupId());

		_updateDLSizeLimitConfiguration(
			assetLibrary, group.getGroupId(), mimeTypeSizeLimits);

		if (assetLibrary.getType() == AssetLibrary.Type.SPACE) {
			Company company = _companyLocalService.getCompanyById(
				serviceContext.getCompanyId());

			ExpandoBridge expandoBridge = company.getExpandoBridge();

			if (!expandoBridge.hasAttribute("cmsFirstTimeAccess")) {
				expandoBridge.addAttribute(
					"cmsFirstTimeAccess", ExpandoColumnConstants.BOOLEAN, false,
					false);

				expandoBridge.setAttribute(
					"cmsFirstTimeAccess", Boolean.FALSE, false);
			}
		}

		return depotEntry;
	}

	private void _checkDuplicateExternalReferenceCode(
		String externalReferenceCode) {

		if (Validator.isNull(externalReferenceCode)) {
			return;
		}

		Group group = _groupLocalService.fetchGroupByExternalReferenceCode(
			externalReferenceCode, contextCompany.getCompanyId());

		if (group != null) {
			throw new DuplicateGroupExternalReferenceCodeException(
				externalReferenceCode);
		}
	}

	private void _copyAssets(long groupId, long sourceGroupId)
		throws Exception {

		Map<String, String[]> parameterMap =
			_exportImportConfigurationParameterMapFactory.
				buildFullPublishParameterMap();

		parameterMap.put(
			PortletDataHandlerKeys.DATA_STRATEGY,
			new String[] {PortletDataHandlerKeys.DATA_STRATEGY_COPY_AS_NEW});

		long userId = contextUser.getUserId();
		Locale locale = contextAcceptLanguage.getPreferredLocale();
		TimeZone timeZone = contextUser.getTimeZone();

		File file = _exportImportLocalService.exportLayoutsAsFile(
			_exportImportConfigurationLocalService.
				addDraftExportImportConfiguration(
					userId,
					ExportImportConfigurationConstants.TYPE_EXPORT_LAYOUT,
					_exportImportConfigurationSettingsMapFactory.
						buildExportLayoutSettingsMap(
							userId, sourceGroupId, false, new long[0],
							parameterMap, locale, timeZone)));

		try {
			_exportImportLocalService.importLayouts(
				_exportImportConfigurationLocalService.
					addDraftExportImportConfiguration(
						userId,
						ExportImportConfigurationConstants.TYPE_IMPORT_LAYOUT,
						_exportImportConfigurationSettingsMapFactory.
							buildImportLayoutSettingsMap(
								userId, groupId, false, new long[0],
								parameterMap, locale, timeZone)),
				file);
		}
		finally {
			FileUtil.delete(file);
		}
	}

	private void _copyConnectedSites(
			DepotEntry depotEntry, DepotEntry sourceDepotEntry)
		throws Exception {

		for (DepotEntryGroupRel sourceDepotEntryGroupRel :
				_depotEntryGroupRelService.getDepotEntryGroupRels(
					sourceDepotEntry, QueryUtil.ALL_POS, QueryUtil.ALL_POS)) {

			DepotEntryGroupRel depotEntryGroupRel =
				_depotEntryGroupRelService.addDepotEntryGroupRel(
					depotEntry.getDepotEntryId(),
					sourceDepotEntryGroupRel.getToGroupId());

			_depotEntryGroupRelService.updateDDMStructuresAvailable(
				depotEntryGroupRel.getDepotEntryGroupRelId(),
				sourceDepotEntryGroupRel.isDdmStructuresAvailable());

			_depotEntryGroupRelService.updateSearchable(
				depotEntryGroupRel.getDepotEntryGroupRelId(),
				sourceDepotEntryGroupRel.isSearchable());
		}
	}

	private void _copyDepotAppCustomizations(
			DepotEntry depotEntry, DepotEntry sourceDepotEntry)
		throws Exception {

		for (DepotAppCustomization depotAppCustomization :
				_depotAppCustomizationLocalService.getDepotAppCustomizations(
					sourceDepotEntry.getDepotEntryId())) {

			_depotAppCustomizationLocalService.updateDepotAppCustomization(
				depotEntry.getDepotEntryId(), depotAppCustomization.isEnabled(),
				depotAppCustomization.getPortletId());
		}
	}

	private void _copyMembers(
			long groupId, ServiceContext serviceContext, long sourceGroupId)
		throws Exception {

		List<User> users = ListUtil.filter(
			_userLocalService.getGroupUsers(sourceGroupId),
			user -> user.getUserId() != contextUser.getUserId());

		_userService.addGroupUsers(
			groupId, ListUtil.toLongArray(users, User.USER_ID_ACCESSOR),
			serviceContext);

		for (User user : users) {
			long[] roleIds = ListUtil.toLongArray(
				ListUtil.filter(
					_roleLocalService.getUserGroupRoles(
						user.getUserId(), sourceGroupId),
					role -> !Objects.equals(
						role.getName(),
						DepotRolesConstants.ASSET_LIBRARY_OWNER)),
				Role.ROLE_ID_ACCESSOR);

			_userGroupRoleService.addUserGroupRoles(
				user.getUserId(), groupId, roleIds);
		}

		List<UserGroup> userGroups = _userGroupLocalService.getGroupUserGroups(
			sourceGroupId);

		_userGroupService.addGroupUserGroups(
			groupId,
			ListUtil.toLongArray(userGroups, UserGroup.USER_GROUP_ID_ACCESSOR));

		for (UserGroup userGroup : userGroups) {
			_userGroupGroupRoleService.addUserGroupGroupRoles(
				userGroup.getUserGroupId(), groupId,
				ListUtil.toLongArray(
					_userGroupGroupRoleLocalService.getUserGroupGroupRoles(
						userGroup.getUserGroupId(), sourceGroupId),
					UserGroupGroupRole::getRoleId));
		}
	}

	private Boolean _getBooleanValue(Object defaultValue, Boolean value) {
		if (value == null) {
			return GetterUtil.getBoolean(defaultValue);
		}

		return value;
	}

	private Map<String, Boolean> _getDepotAppCustomizationMap(
			long companyId, String externalReferenceCode)
		throws Exception {

		Map<String, Boolean> depotAppCustomizationMap = new HashMap<>();

		Group group = _groupLocalService.fetchGroupByExternalReferenceCode(
			externalReferenceCode, companyId);

		if (group == null) {
			return depotAppCustomizationMap;
		}

		DepotEntry depotEntry = _depotEntryService.getGroupDepotEntry(
			group.getGroupId());

		for (DepotAppCustomization depotAppCustomization :
				_depotAppCustomizationLocalService.getDepotAppCustomizations(
					depotEntry.getDepotEntryId())) {

			depotAppCustomizationMap.put(
				depotAppCustomization.getPortletId(),
				depotAppCustomization.isEnabled());
		}

		return depotAppCustomizationMap;
	}

	private DepotEntry _getGroupDepotEntry(Long assetLibraryId)
		throws Exception {

		DepotEntry depotEntry = _depotEntryService.fetchGroupDepotEntry(
			assetLibraryId);

		if (depotEntry != null) {
			return depotEntry;
		}

		return _depotEntryService.getDepotEntry(assetLibraryId);
	}

	private long _getGroupIdByExternalReferenceCode(
			String externalReferenceCode)
		throws Exception {

		Group group = _groupLocalService.getGroupByExternalReferenceCode(
			externalReferenceCode, contextCompany.getCompanyId());

		return group.getGroupId();
	}

	private Map<Locale, String> _getLocalizedMap(
		String defaultValue, Map<String, String> i18nMap) {

		Map<Locale, String> localizedMap = LocalizedMapUtil.getLocalizedMap(
			contextAcceptLanguage.getPreferredLocale(), defaultValue, i18nMap);

		if (!localizedMap.containsKey(LocaleUtil.getDefault())) {
			localizedMap = LocalizedMapUtil.patchLocalizedMap(
				localizedMap, LocaleUtil.getDefault(), defaultValue, i18nMap);
		}

		return localizedMap;
	}

	private ServiceContext _getServiceContext() throws Exception {
		ServiceContext serviceContext = ServiceContextFactory.getInstance(
			DepotEntry.class.getName(), contextHttpServletRequest);

		serviceContext.setCompanyId(contextCompany.getCompanyId());
		serviceContext.setModifiedDate(new Date());
		serviceContext.setUserId(contextUser.getUserId());

		return serviceContext;
	}

	private UnicodeProperties _getUnicodeProperties(
		long companyId, String externalReferenceCode) {

		Group group = _groupLocalService.fetchGroupByExternalReferenceCode(
			externalReferenceCode, companyId);

		if (group != null) {
			return group.getTypeSettingsProperties();
		}

		return new UnicodeProperties(true);
	}

	private <T, E extends Exception> T _getValue(
			UnsafeSupplier<T, E> defaultValueUnsafeSupplier,
			UnsafeSupplier<T, E> valueUnsafeSupplier)
		throws Exception {

		T value = valueUnsafeSupplier.get();

		if (value == null) {
			return defaultValueUnsafeSupplier.get();
		}

		return value;
	}

	private UnicodeProperties _patchUnicodeProperties(
		Settings settings, UnicodeProperties unicodeProperties) {

		return UnicodePropertiesBuilder.create(
			true
		).put(
			"autoTaggingEnabled",
			_getBooleanValue(
				unicodeProperties.getProperty("autoTaggingEnabled"),
				settings.getAutoTaggingEnabled())
		).put(
			"inheritLocales",
			() -> {
				Boolean inheritLocales = _getBooleanValue(
					unicodeProperties.getProperty("inheritLocales"),
					settings.getUseCustomLanguages());

				if (settings.getUseCustomLanguages() == null) {
					return inheritLocales.toString();
				}

				return String.valueOf(!inheritLocales.booleanValue());
			}
		).put(
			"languageId",
			GetterUtil.getString(
				settings.getDefaultLanguageId(),
				unicodeProperties.getProperty("languageId"))
		).put(
			"locales",
			GetterUtil.getString(
				StringUtil.merge(settings.getAvailableLanguageIds()),
				unicodeProperties.getProperty("locales"))
		).put(
			"logoColor",
			GetterUtil.getString(
				settings.getLogoColor(),
				unicodeProperties.getProperty("logoColor"))
		).put(
			"sharingEnabled",
			_getBooleanValue(
				unicodeProperties.getProperty("sharingEnabled"),
				settings.getSharingEnabled())
		).put(
			"trashEnabled",
			_getBooleanValue(
				unicodeProperties.getProperty("trashEnabled"),
				settings.getTrashEnabled())
		).put(
			"trashEntriesMaxAge",
			GetterUtil.getInteger(
				settings.getTrashEntriesMaxAge(),
				GetterUtil.getInteger(
					unicodeProperties.getProperty("trashEntriesMaxAge")))
		).build();
	}

	private UnicodeProperties _putUnicodeProperties(Settings settings) {
		if (settings == null) {
			return null;
		}

		return UnicodePropertiesBuilder.create(
			true
		).put(
			"autoTaggingEnabled",
			GetterUtil.getBoolean(settings.getAutoTaggingEnabled())
		).put(
			"inheritLocales",
			!GetterUtil.getBoolean(settings.getUseCustomLanguages())
		).put(
			"languageId", settings.getDefaultLanguageId()
		).put(
			"locales", StringUtil.merge(settings.getAvailableLanguageIds())
		).put(
			"logoColor",
			GetterUtil.getString(settings.getLogoColor(), "outline-0")
		).put(
			"sharingEnabled",
			GetterUtil.getBoolean(
				settings.getSharingEnabled(),
				SharingConfigurationConstants.SHARING_ENABLED_DEFAULT)
		).put(
			"trashEnabled",
			GetterUtil.getBoolean(settings.getTrashEnabled(), true)
		).put(
			"trashEntriesMaxAge",
			GetterUtil.getInteger(settings.getTrashEntriesMaxAge())
		).build();
	}

	private AssetLibrary _toAssetLibrary(DepotEntry depotEntry)
		throws Exception {

		return _assetLibraryDTOConverter.toDTO(
			new DefaultDTOConverterContext(
				contextAcceptLanguage.isAcceptAllLanguages(),
				HashMapBuilder.put(
					"assign-members",
					() -> {
						if (!_groupModelResourcePermission.contains(
								PermissionThreadLocal.getPermissionChecker(),
								depotEntry.getGroupId(),
								ActionKeys.ASSIGN_MEMBERS)) {

							return null;
						}

						return addAction(
							ActionKeys.VIEW, depotEntry, "getAssetLibrary");
					}
				).put(
					"connect-sites",
					() -> {
						if (!_depotEntryModelResourcePermission.contains(
								PermissionThreadLocal.getPermissionChecker(),
								depotEntry.getDepotEntryId(),
								ActionKeys.UPDATE)) {

							return null;
						}

						return addAction(
							ActionKeys.VIEW, depotEntry, "getAssetLibrary");
					}
				).put(
					"create",
					addAction(
						DepotActionKeys.ADD_DEPOT_ENTRY, depotEntry,
						"postAssetLibrary")
				).put(
					"delete",
					addAction(
						ActionKeys.DELETE, depotEntry, "deleteAssetLibrary")
				).put(
					"get",
					addAction(ActionKeys.VIEW, depotEntry, "getAssetLibrary")
				).put(
					"permissions",
					addAction(
						ActionKeys.PERMISSIONS, depotEntry, "patchAssetLibrary")
				).put(
					"pin",
					addAction(
						ActionKeys.UPDATE, depotEntry, "putAssetLibraryPin")
				).put(
					"unpin",
					addAction(
						ActionKeys.UPDATE, depotEntry, "deleteAssetLibraryPin")
				).put(
					"update",
					addAction(
						ActionKeys.UPDATE, depotEntry, "patchAssetLibrary")
				).put(
					"view-connected-sites",
					() -> {
						if (_depotEntryModelResourcePermission.contains(
								PermissionThreadLocal.getPermissionChecker(),
								depotEntry.getDepotEntryId(),
								ActionKeys.UPDATE)) {

							return null;
						}

						return addAction(
							ActionKeys.VIEW, depotEntry, "getAssetLibrary");
					}
				).put(
					"view-members",
					() -> {
						if (_groupModelResourcePermission.contains(
								PermissionThreadLocal.getPermissionChecker(),
								depotEntry.getGroupId(),
								ActionKeys.ASSIGN_MEMBERS)) {

							return null;
						}

						return addAction(
							ActionKeys.VIEW, depotEntry, "getAssetLibrary");
					}
				).build(),
				_dtoConverterRegistry, depotEntry.getDepotEntryId(),
				contextAcceptLanguage.getPreferredLocale(), contextUriInfo,
				contextUser));
	}

	private void _updateDLSizeLimitConfiguration(
			AssetLibrary assetLibrary, long groupId,
			Map<String, Long> mimeTypeSizeLimits)
		throws Exception {

		Settings settings = assetLibrary.getSettings();

		MimeTypeLimit[] mimeTypeLimits = settings.getMimeTypeLimits();

		if (mimeTypeLimits == null) {
			_dlSizeLimitConfigurationProvider.updateGroupSizeLimit(
				groupId, 0L, 0L, mimeTypeSizeLimits);

			return;
		}

		mimeTypeSizeLimits = new LinkedHashMap<>();

		for (MimeTypeLimit mimeTypeLimit : mimeTypeLimits) {
			String mimeType = mimeTypeLimit.getMimeType();

			if (Validator.isNotNull(mimeType)) {
				mimeTypeSizeLimits.put(
					mimeType,
					GetterUtil.getLong(mimeTypeLimit.getMaximumSize()));
			}
		}

		_dlSizeLimitConfigurationProvider.updateGroupSizeLimit(
			groupId, 0L, 0L, mimeTypeSizeLimits);
	}

	private void _updateFriendlyURL(AssetLibrary assetLibrary, long groupId)
		throws Exception {

		String friendlyURL = assetLibrary.getFriendlyURL();

		if (Validator.isNotNull(friendlyURL)) {
			_groupLocalService.updateFriendlyURL(groupId, friendlyURL);
		}
	}

	private static final AssetLibraryEntityModel _assetLibraryEntityModel =
		new AssetLibraryEntityModel();

	@Reference(
		policy = ReferencePolicy.DYNAMIC,
		policyOption = ReferencePolicyOption.GREEDY,
		target = "(dto.class.name=com.liferay.depot.model.DepotEntry)"
	)
	private volatile DTOConverter<DepotEntry, AssetLibrary>
		_assetLibraryDTOConverter;

	@Reference
	private CompanyLocalService _companyLocalService;

	@Reference
	private DepotAppCustomizationLocalService
		_depotAppCustomizationLocalService;

	@Reference
	private DepotEntryGroupRelService _depotEntryGroupRelService;

	@Reference(target = "(model.class.name=com.liferay.depot.model.DepotEntry)")
	private ModelResourcePermission<DepotEntry>
		_depotEntryModelResourcePermission;

	@Reference
	private DepotEntryPinLocalService _depotEntryPinLocalService;

	@Reference
	private DepotEntryPinService _depotEntryPinService;

	@Reference
	private DepotEntryService _depotEntryService;

	@Reference
	private DLSizeLimitConfigurationProvider _dlSizeLimitConfigurationProvider;

	@Reference
	private DTOConverterRegistry _dtoConverterRegistry;

	@Reference
	private ExportImportConfigurationLocalService
		_exportImportConfigurationLocalService;

	@Reference
	private ExportImportConfigurationParameterMapFactory
		_exportImportConfigurationParameterMapFactory;

	@Reference
	private ExportImportConfigurationSettingsMapFactory
		_exportImportConfigurationSettingsMapFactory;

	@Reference
	private ExportImportLocalService _exportImportLocalService;

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference(
		target = "(model.class.name=com.liferay.portal.kernel.model.Group)"
	)
	private ModelResourcePermission<Group> _groupModelResourcePermission;

	@Reference
	private RoleLocalService _roleLocalService;

	@Reference
	private UserGroupGroupRoleLocalService _userGroupGroupRoleLocalService;

	@Reference
	private UserGroupGroupRoleService _userGroupGroupRoleService;

	@Reference
	private UserGroupLocalService _userGroupLocalService;

	@Reference
	private UserGroupRoleService _userGroupRoleService;

	@Reference
	private UserGroupService _userGroupService;

	@Reference
	private UserLocalService _userLocalService;

	@Reference
	private UserService _userService;

}