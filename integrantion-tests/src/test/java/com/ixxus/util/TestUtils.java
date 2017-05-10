/**
 * All rights reserved. Copyright (c) Ixxus Ltd 2015
 */

package com.ixxus.util;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.version.VersionBaseModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.PropertyMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.Status;
import javax.transaction.UserTransaction;
import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class TestUtils {

    public static final String TEST_SITE = "testSite";
    public static final String TEST_NAME = "testName";
    public static final String TEST_FOLDER_NAME = "testFolder";

    private static final Logger LOG = LoggerFactory.getLogger(TestUtils.class);

    private final NodeService nodeService;
    private final ContentService contentService;
    private final TransactionService transactionService;
    private final CheckOutCheckInService checkOutCheckInService;
    private final SiteService siteService;
    private final FileFolderService fileFolderService;
    private final PersonService personService;
    private final AuthorityService authorityService;
    private final PermissionService permissionService;
    private final Repository repositoryHelper;
    private final NamespaceService namespaceService;

    public TestUtils(final ServiceRegistry serviceRegistry, final Repository repositoryHelper) {
        this.nodeService = serviceRegistry.getNodeService();
        this.fileFolderService = serviceRegistry.getFileFolderService();
        this.contentService = serviceRegistry.getContentService();
        this.transactionService = serviceRegistry.getTransactionService();
        this.checkOutCheckInService = serviceRegistry.getCheckOutCheckInService();
        this.siteService = serviceRegistry.getSiteService();
        this.personService = serviceRegistry.getPersonService();
        this.authorityService = serviceRegistry.getAuthorityService();
        this.permissionService = serviceRegistry.getPermissionService();
        this.repositoryHelper = repositoryHelper;
        this.namespaceService = serviceRegistry.getNamespaceService();
    }

    public void createPublicSiteInsideTransaction(final String siteShortName) throws Exception {
        createPublicSiteInsideTransaction(siteShortName, "sitePresetTest");
    }

    public void createPublicSiteWithPresetInsideTransaction(final String siteShortName, final String sitePreset) throws Exception {
        createPublicSiteInsideTransaction(siteShortName, sitePreset);
    }

    public void createPublicSiteInsideTransaction(final String siteShortName, final String sitePreset) throws Exception {
        final UserTransaction txn = transactionService.getUserTransaction();
        txn.begin();
        siteService.createSite(sitePreset, siteShortName, "public test Site", "public test site",
                SiteVisibility.PUBLIC);
        siteService.createContainer(siteShortName, SiteService.DOCUMENT_LIBRARY, ContentModel.TYPE_FOLDER, null);
        txn.commit();
    }

    public void beforePurgeSite(final String shortName) {
        // Delete the associated groups
        // Delete the master site group
        final String siteGroup = siteService.getSiteGroup(shortName);
        if (authorityService.authorityExists(siteGroup)) {
            authorityService.deleteAuthority(siteGroup, false);
            SiteInfo siteInfo = siteService.getSite(shortName);
            if (siteInfo != null) {
                final QName siteType = nodeService.getType(siteInfo.getNodeRef());
                // Iterate over the role related groups and delete then
                Set<String> permissions = permissionService.getSettablePermissions(siteType);
                for (String permission : permissions) {
                    String siteRoleGroup = siteService.getSiteRoleGroup(shortName, permission);

                    // Delete the site role group
                    authorityService.deleteAuthority(siteRoleGroup);
                }
            }
        }
    }

    public void deleteSiteInsideTransaction(final String siteShortName) throws Exception {
        final UserTransaction txn = transactionService.getUserTransaction();
        txn.begin();
        beforePurgeSite(siteShortName);
        siteService.deleteSite(siteShortName);
        txn.commit();
    }

    public NodeRef createNodeOfType(final NodeRef container, final String nodeName, final QName type) {
        return createNodeOfType(container, nodeName, new PropertyMap(), type);
    }

    public NodeRef createNodeOfType(final NodeRef container, final String nodeName, final PropertyMap propertyMap,
            final QName type) {
        propertyMap.putIfAbsent(ContentModel.PROP_NAME, nodeName);
        return nodeService
                .createNode(container, ContentModel.ASSOC_CONTAINS,
                        QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, nodeName), type, propertyMap)
                .getChildRef();
    }

    public NodeRef createContentNode(final NodeRef container, final String assetFileName) {
        return createNodeOfType(container, assetFileName, new PropertyMap(), ContentModel.TYPE_CONTENT);
    }

    public NodeRef createContentNode(final NodeRef container, final String assetFileName, final String content, final String mimetype) {
        final NodeRef nodeRef = createNodeOfType(container, assetFileName, new PropertyMap(),
                ContentModel.TYPE_CONTENT);
        final ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
        writer.putContent(content);
        ContentData contentData = (ContentData)nodeService.getProperty(nodeRef, ContentModel.PROP_CONTENT);
        contentData = ContentData.setMimetype(contentData, mimetype);
        nodeService.setProperty(nodeRef, ContentModel.PROP_CONTENT, contentData);

        return nodeRef;
    }

    public void createNewVersionOfNode(final NodeRef nodeRef, final String newContent) {
        NodeRef workingCopy = checkOutCheckInService.checkout(nodeRef);
        ContentWriter writer = contentService.getWriter(workingCopy, ContentModel.PROP_CONTENT, true);
        writer.putContent(newContent);
        checkOutCheckInService.checkin(workingCopy, getNewVersionProperties());
    }

    public Map<String, Serializable> getNewVersionProperties() {
        final Map<String, Serializable> versionProperties = new HashMap<>();
        versionProperties.put(Version.PROP_DESCRIPTION, "New Test Version");
        versionProperties.put(VersionBaseModel.PROP_VERSION_TYPE, VersionType.MINOR);
        return versionProperties;
    }

    public NodeRef createContentNode(final NodeRef container, final String assetFileName, final String content) {
        return createContentNode(container, assetFileName, content, MimetypeMap.MIMETYPE_TEXT_PLAIN);
    }

    public NodeRef createFolderNode(final NodeRef container, final String folderName) {
        return createNodeOfType(container, folderName, new PropertyMap(), ContentModel.TYPE_FOLDER);
    }

    public String getOwner(final NodeRef nodeRef) {
        return (String)nodeService.getProperty(nodeRef, ContentModel.PROP_OWNER);
    }

    public String getNodeContent(final NodeRef nodeRef) {
        final ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
        return reader.getContentString();
    }

    public void setNodeContentFromFile(NodeRef nodeRef, String filePath, String mimetype) {
        URL fileUrl = getClass().getResource(filePath);
        File file = new File(fileUrl.getPath());
        ContentWriter contentWriter = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
        contentWriter.putContent(file);
        contentWriter.setMimetype(mimetype);
    }

    public void deleteNodesWithInNewTrancation(final NodeRef... nodes) throws Exception {
        final UserTransaction txn = transactionService.getUserTransaction();
        if (txn.getStatus() != Status.STATUS_MARKED_ROLLBACK) {
            txn.begin();
            for (final NodeRef nodeRef : nodes) {
                nodeService.deleteNode(nodeRef);
            }
            txn.commit();
        }
    }

    public void deleteEverythingInSiteDocumentLibrary(final String siteName) {
        final NodeRef siteDocLib = siteService.getContainer(siteName, SiteService.DOCUMENT_LIBRARY);
        deleteEverythingInNode(siteDocLib);
    }

    private void deleteEverythingInNode(final NodeRef node) {
        if (node != null) {
            final List<ChildAssociationRef> kids = nodeService.getChildAssocs(node);
            for (final ChildAssociationRef car : kids) {
                final NodeRef childRef = car.getChildRef();
                //we may have deleted some working copies in a previous iteration
                if (nodeService.exists(childRef)) {
                    cancelCheckedOut(childRef);
                    nodeService.deleteNode(childRef);
                }
            }
        }
    }

    public void cancelCheckedOut(final NodeRef nodeRef) {
        final NodeRef workingCopyNodeRef = checkOutCheckInService.getWorkingCopy(nodeRef);
        if (workingCopyNodeRef != null) {
            checkOutCheckInService.cancelCheckout(workingCopyNodeRef);
        }
    }

    public NodeRef getDocLib(final String siteShortName) {
        return siteService.getContainer(siteShortName, SiteService.DOCUMENT_LIBRARY);
    }

    public String getNodeName(final NodeRef nodeRef) {
        return (String)nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
    }

    public String getDayOfMonth(final Date aDate) {
        return new SimpleDateFormat("dd").format(aDate);
    }

    public String getMonth(final Date aDate) {
        return new SimpleDateFormat("MM").format(aDate);
    }

    public String getYear(final Date aDate) {
        return new SimpleDateFormat("yyyy").format(aDate);
    }

    public void assertNodeExists(final NodeRef nodeRef) {
        assertThat("the node should exist in the workspace spacesStore", nodeService.exists(nodeRef),
                equalTo(Boolean.TRUE));
    }

    public void assertNodeName(final NodeRef nodeRef, final String name) {
        final String actualNodeName = getNodeName(nodeRef);
        assertThat("the actual name for the node should be equal to the expected name", actualNodeName, equalTo(name));
    }

    public NodeRef assertPathPointsToAValidNode(final String path) {
        final NodeRef parentNode = getNodeFromPath(path);
        assertThat(String.format("A node should be found in path '%s'", path), parentNode, notNullValue());
        return parentNode;
    }

    public void assertNodeHasPrimaryParent(final NodeRef nodeRef, final NodeRef primaryParentNodeRef) {
        final NodeRef actualPrimaryParentNode = nodeService.getPrimaryParent(nodeRef).getParentRef();
        assertThat("The actual primary parent should be equal to the expected primary parent", actualPrimaryParentNode,
                equalTo(primaryParentNodeRef));
    }

    public void assertPathDoesntPointToAValidNode(final String path) {
        assertThat(String.format("A node shouldn't be found in path '%s'", path), getNodeFromPath(path), nullValue());
    }

    /**
     * Returns the {@link NodeRef} object for the node that is located at the given <code>path</code>. The search starts from the 'Company Home' node.
     *
     * @param path
     *        {@link String} that has the following format 'name_1/name_2/.../name_n'
     * @return {@link NodeRef} object if the node is found, else null
     */
    public NodeRef getNodeFromPath(final String path) {
        FileInfo fileInfo = null;
        try {
            fileInfo = fileFolderService.resolveNamePath(repositoryHelper.getCompanyHome(),
                    Arrays.asList(path.split("/")));
        } catch(final FileNotFoundException e) {
            LOG.debug("A node wasn't found for path {} ", path);
        }
        return fileInfo != null ? fileInfo.getNodeRef() : null;
    }

    public void createUserInGroup(final String userName, final String groupName) throws Exception {
        final UserTransaction txn = transactionService.getNonPropagatingUserTransaction();
        txn.begin();
        final HashMap<QName, Serializable> properties = new HashMap<>();
        properties.put(ContentModel.PROP_USERNAME, userName);
        personService.createPerson(properties);
        authorityService.addAuthority(groupName, userName);
        txn.commit();
    }

    public void deletePerson(final String testUser) throws Exception {
        final UserTransaction txn = transactionService.getNonPropagatingUserTransaction();
        txn.begin();
        if (!StringUtils.isBlank(testUser) && (personService.getPersonOrNull(testUser) != null)) {
            personService.deletePerson(testUser);
        }
        txn.commit();
    }

    public String getResourceAsString(final String resourcePath) throws IOException {
        final InputStream is = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
        final OutputStream os = new ByteArrayOutputStream();
        IOUtils.copy(is, os);
        return os.toString();
    }

    public void assertPropertyValue(final NodeRef nodeRef, final QName property, final Serializable expectedValue) {
        assertThat("The value of the " + property.getLocalName() + " property is incorrect.", nodeService.getProperty(nodeRef, property),
                equalTo(expectedValue));
    }

    public void addAspect(final NodeRef nodeRef, final QName aspectTypeQName, final Map<QName, Serializable> aspectProperties) {
        nodeService.addAspect(nodeRef, aspectTypeQName, aspectProperties);
    }

}