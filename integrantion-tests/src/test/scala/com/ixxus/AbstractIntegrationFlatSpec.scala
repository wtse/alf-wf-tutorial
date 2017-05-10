package com.ixxus

import java.util.UUID
import javax.transaction.UserTransaction

import com.ixxus.util.TestUtils
import org.alfresco.model.ContentModel
import org.alfresco.repo.model.Repository
import org.alfresco.repo.security.authentication.AuthenticationUtil
import org.alfresco.service.ServiceRegistry
import org.alfresco.service.cmr.dictionary.DictionaryService
import org.alfresco.service.cmr.repository.{ContentService, NodeRef, NodeService}
import org.alfresco.service.namespace.{NamespaceService, QName}
import org.alfresco.service.transaction.TransactionService
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}
import org.springframework.beans.factory.annotation.{Autowired, Qualifier}

import scala.reflect.ClassTag

/**
  * Alfresco integration test base class.
  * It extends FlatSpec, BeforeAndAfterEach, BeforeAndAfterAll and Matchers.
  * It implements before/after all and each
  *
  * Created by mirko on 13/01/2017.
  */
trait AbstractIntegrationFlatSpec extends FlatSpec with BeforeAndAfterEach with BeforeAndAfterAll with Matchers {
    val TEST_SITE: String = randomName(TestUtils.TEST_SITE)
    val TEST_FOLDER: String = randomName(TestUtils.TEST_FOLDER_NAME)
    val TEST_NAME: String = randomName(TestUtils.TEST_NAME)

    type QNameMap = java.util.HashMap[QName, java.io.Serializable]

    // Alfresco services
    @Autowired val serviceRegistry: ServiceRegistry = null
    @Autowired val nodeService: NodeService = null
    @Autowired val contentService: ContentService = null
    @Autowired val dictionaryService: DictionaryService = null
    @Autowired val transactionService: TransactionService = null

    @Autowired
    @Qualifier("repositoryHelper")
    val repository: Repository = null

    // Custom services
    @Autowired val testUtils: TestUtils = null

    override def beforeAll(): Unit = {
        initFullyAdminAuthentication()
        testUtils.createPublicSiteInsideTransaction(TEST_SITE)
    }

    override def afterAll(): Unit = {
        initFullyAdminAuthentication()
        testUtils.deleteSiteInsideTransaction(TEST_SITE)
        AuthenticationUtil.clearCurrentSecurityContext()
    }

    /**
      * Initialize security context with admin user as fully authenticated
      */
    protected def initFullyAdminAuthentication(): Unit = {
        AuthenticationUtil.clearCurrentSecurityContext()
        AuthenticationUtil.setAdminUserAsFullyAuthenticatedUser()
        AuthenticationUtil.pushAuthentication()
    }

    def randomName(name: String): String = name + "-" + UUID.randomUUID

    /** Matcher for comparing with null. E.g. xxx should nullable */
    def nullable[T: ClassTag] =
        Matcher { (nullObj: T) =>
            val clazz = implicitly[ClassTag[T]].runtimeClass
            val toCompare = nullObj match {
                case anyShould: AnyShouldWrapper[clazz] => anyShould.leftSideValue
                case _ => nullObj
            }
            MatchResult(
                toCompare == null,
                toCompare + " was not null",
                toCompare + " was null"
            )
        }


    /**
      * Create folder in Alfresco CompanyHome
      *
      * @param folderName folder name
      * @return nodeRef for created folder
      */
    def createFolderInCompanyHome(folderName: String): NodeRef = {
        val companyHomeNodeRef: NodeRef = repository.getCompanyHome
        createFolder(companyHomeNodeRef, randomName(folderName), new QNameMap())
    }

    /**
      * Create a folder in a specific location.
      *
      * @param parentNodeRef parent nodeRef
      * @param folderName    folder name that you want to create
      * @param properties    properties map
      * @return nodeRef for created folder
      */
    def createFolder(parentNodeRef: NodeRef, folderName: String, properties: QNameMap): NodeRef = {
        properties.put(ContentModel.PROP_NAME, folderName)
        transacted {
            val nodeRef: NodeRef = nodeService.createNode(parentNodeRef, ContentModel.ASSOC_CONTAINS,
                QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, folderName), ContentModel.TYPE_FOLDER,
                properties).getChildRef
            nodeRef
        }
    }

    /**
      * Create a document in a specific folder
      *
      * @param parentNodeRef parent nodeRef
      * @param name          document name
      * @param docType       document type
      * @param properties    properties map
      * @param content       content to be set in the new document
      * @return nodeRef for created document
      */
    def createDoc(parentNodeRef: NodeRef, name: String, docType: QName, properties: QNameMap, content: String): NodeRef = {
        properties.put(ContentModel.PROP_NAME, name)
        transacted {
            val nodeRef: NodeRef = nodeService.createNode(parentNodeRef, ContentModel.ASSOC_CONTAINS,
                QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, name),
                docType, properties).getChildRef
            if (content != null) {
                val contentWriter = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true)
                contentWriter.putContent(content)
            }
            nodeRef
        }
    }

    /**
      * Manage manually an Alfresco transaction without rollback
      *
      * @param transaction function with the statements to execute in transaction
      * @return value of type T
      */

    def transacted[T](transaction: => T): T = {
        val trx: UserTransaction = transactionService.getNonPropagatingUserTransaction(false)
        trx.begin()
        val res = transaction
        trx.commit()
        res
    }
}
