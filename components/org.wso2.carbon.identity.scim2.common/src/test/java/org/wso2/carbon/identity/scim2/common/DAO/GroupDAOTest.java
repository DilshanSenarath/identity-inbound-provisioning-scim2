/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.scim2.common.DAO;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.common.testng.WithH2Database;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.scim2.common.exceptions.IdentitySCIMException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
import static org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_ID;

/**
 * Test class for GroupDAO.
 */
@WithH2Database(jndiName = "jdbc/WSO2IdentityDB", files = {"dbscripts/identity.sql"})
public class GroupDAOTest {

    private static final String DEFAULT_USER_STORE_DOMAIN = "PRIMARY";
    private static final String SECONDARY_USER_STORE_DOMAIN = "SECONDARY";
    private static final String INTERNAL_DOMAIN = "Internal";
    private static final String GROUP_NAME_PREFIX = "Group";
    private static final int GROUP_COUNT = 3;
    private static final String ID_ATTRIBUTE = "urn:ietf:params:scim:schemas:core:2.0:id";
    private static final String ID_ATTRIBUTE_VALUE = "34181606-4848-43b8-b550-7c07ebe8cc24";
    private static final String CREATED_DATE_ATTRIBUTE = "urn:ietf:params:scim:schemas:core:2.0:meta.created";
    private static final String CREATED_DATE_ATTRIBUTE_VALUE = "2021-08-25T10:00:00Z";

    private MockedStatic<IdentityTenantUtil> mockIdentityTenantUtil;
    private MockedStatic<IdentityUtil> mockIdentityUtil;
    private GroupDAO groupDAO;

    /**
     * Setup the test environment for GroupDAOTest.
     */
    @BeforeClass
    public void setup() throws IdentitySCIMException {

        mockIdentityTenantUtil = mockStatic(IdentityTenantUtil.class);
        mockIdentityUtil = mockStatic(IdentityUtil.class);
        groupDAO = new GroupDAO();
        setupInitConfigurations();
        createTestGroups();
    }

    @DataProvider(name = "GroupListDataProviderWithFilter")
    public Object[][] groupListDataProviderWithFilter() {

        return new Object[][] {
                {SUPER_TENANT_ID, INTERNAL_DOMAIN + CarbonConstants.DOMAIN_SEPARATOR + "Group%", GROUP_COUNT},
                {SUPER_TENANT_ID, INTERNAL_DOMAIN + CarbonConstants.DOMAIN_SEPARATOR + "Group1%", 1},
                {SUPER_TENANT_ID, INTERNAL_DOMAIN + CarbonConstants.DOMAIN_SEPARATOR + "%ou%", GROUP_COUNT},
                {SUPER_TENANT_ID, INTERNAL_DOMAIN + CarbonConstants.DOMAIN_SEPARATOR + "%oup1%", 1},
                {SUPER_TENANT_ID, INTERNAL_DOMAIN + CarbonConstants.DOMAIN_SEPARATOR + "%oup1", 1},
                {SUPER_TENANT_ID, DEFAULT_USER_STORE_DOMAIN + CarbonConstants.DOMAIN_SEPARATOR + "Group%",
                        GROUP_COUNT},
                {SUPER_TENANT_ID, DEFAULT_USER_STORE_DOMAIN + CarbonConstants.DOMAIN_SEPARATOR + "Group1%", 1},
                {SUPER_TENANT_ID, DEFAULT_USER_STORE_DOMAIN + CarbonConstants.DOMAIN_SEPARATOR + "%ou%", GROUP_COUNT},
                {SUPER_TENANT_ID, DEFAULT_USER_STORE_DOMAIN + CarbonConstants.DOMAIN_SEPARATOR + "%oup1%", 1},
                {SUPER_TENANT_ID, DEFAULT_USER_STORE_DOMAIN + CarbonConstants.DOMAIN_SEPARATOR + "%oup1", 1},
                {SUPER_TENANT_ID, SECONDARY_USER_STORE_DOMAIN + CarbonConstants.DOMAIN_SEPARATOR + "Group%",
                        GROUP_COUNT},
                {SUPER_TENANT_ID, SECONDARY_USER_STORE_DOMAIN + CarbonConstants.DOMAIN_SEPARATOR + "Group1%", 1},
                {SUPER_TENANT_ID, SECONDARY_USER_STORE_DOMAIN + CarbonConstants.DOMAIN_SEPARATOR + "%ou%",
                        GROUP_COUNT},
                {SUPER_TENANT_ID, SECONDARY_USER_STORE_DOMAIN + CarbonConstants.DOMAIN_SEPARATOR + "%oup1%", 1},
                {SUPER_TENANT_ID, SECONDARY_USER_STORE_DOMAIN + CarbonConstants.DOMAIN_SEPARATOR + "%oup1", 1},
                {SUPER_TENANT_ID, "%ou%", GROUP_COUNT * 3},
                {SUPER_TENANT_ID, "Group%", 0},
                {SUPER_TENANT_ID, "%oup1", GROUP_COUNT},
                {SUPER_TENANT_ID, "oup1", 0},
                {SUPER_TENANT_ID, "", 0}
        };
    }


    @Test(description = "Test list group names with display name filter.",
            dataProvider = "GroupListDataProviderWithFilter")
    public void testListGroupNameWithDisplayNameFilter(int tenantId, String filter, int expectedGroupCount)
            throws IdentitySCIMException {

        Assert.assertEquals(groupDAO.getGroupNameList(tenantId, filter).length, expectedGroupCount);
    }

    /**
     * Create test groups.
     */
    private void createTestGroups() throws IdentitySCIMException {

        String[] domainList = new String[] {DEFAULT_USER_STORE_DOMAIN, SECONDARY_USER_STORE_DOMAIN, INTERNAL_DOMAIN};
        Map<String, String> attributes = new HashMap<>();
        attributes.put(CREATED_DATE_ATTRIBUTE, CREATED_DATE_ATTRIBUTE_VALUE);
        for (String domain : domainList) {
            for (int i = 0; i < GROUP_COUNT; i++) {
                attributes.put(ID_ATTRIBUTE, ID_ATTRIBUTE_VALUE + domain.hashCode() + i);
                groupDAO.addSCIMGroupAttributes(SUPER_TENANT_ID,
                        domain + CarbonConstants.DOMAIN_SEPARATOR + GROUP_NAME_PREFIX + (i + 1), attributes);
            }
        }
    }

    /**
     * Setup the configurations for the test.
     */
    private void setupInitConfigurations() {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes", "repository").
                toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH, Paths.get(carbonHome, "conf").toString());

        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(SUPER_TENANT_DOMAIN_NAME);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(SUPER_TENANT_ID);

        CarbonConstants.ENABLE_LEGACY_AUTHZ_RUNTIME = false;

        mockIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(eq(SUPER_TENANT_DOMAIN_NAME)))
                .thenReturn(SUPER_TENANT_ID);
        mockIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantDomain(eq(SUPER_TENANT_ID)))
                .thenReturn(SUPER_TENANT_DOMAIN_NAME);

        mockIdentityUtil.when(IdentityUtil::getIdentityConfigDirPath)
                .thenReturn(Paths.get(carbonHome, "conf", "identity").toString());
        mockIdentityUtil.when(() -> IdentityUtil.extractDomainFromName(anyString()))
                .thenReturn(DEFAULT_USER_STORE_DOMAIN);
        mockIdentityUtil.when(IdentityUtil::getPrimaryDomainName).thenReturn(SUPER_TENANT_DOMAIN_NAME);
    }
}
