package com.ixxus

import java.io.File

import org.apache.commons.io.FileUtils
import org.scalatest.{Args, Status, Suite}
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.TestContextManager

/**
  * ScalaTest Suites that run only one spring application context for all the test classes
  *
  * Created by mirko on 11/01/2017.
  *
  * @param suitesToNest test classes or inner suite to run in the same spring context
  */
abstract class AbstractScalaIntegrationSuite[T <: AnyRef](testClass: Class[T])(suitesToNest: Suite*) extends Suite {
    thisSuite =>
    val logger: org.slf4j.Logger = LoggerFactory.getLogger(classOf[AbstractScalaIntegrationSuite[T]])
    // Create the context manager
    val contextManager: IxxusTestContextManager[T] = new IxxusTestContextManager(testClass)

    /**
      * Post process suite beans
      * Applying all the spring post processors
      */
    private def postProcessSuiteBeans(): Unit = {
        suitesToNest.foreach(bean => contextManager.prepareTestInstance(bean))
    }

    /**
      * Returns an immutable <code>IndexedSeq</code> containing the suites passed to the constructor in
      * the order they were passed.
      */
    override val nestedSuites: collection.immutable.IndexedSeq[Suite] = Vector.empty ++ suitesToNest

    override protected def runNestedSuites(args: Args): Status = {
        // Pre run setup
        preCleanUp()
        postProcessSuiteBeans()
        // Run the tests
        val status = super.runNestedSuites(args)
        // Post run setup
        contextManager.applicationContext().close()
        cleanUp()
        status
    }

    /**
      * Clean up operation before the running of the application context.
      * Will clean up any files remaining from previous test run that could
      * not be removed successfully.
      */
    protected def preCleanUp(): Unit = {
        if (new File("alf_data_dev").exists) {
            FileUtils.deleteDirectory(new File("alf_data_dev"))
        }

        if (new File("../alf_data_dev").exists) {
            FileUtils.deleteDirectory(new File("../alf_data_dev"))
        }
    }

    /**
      * Clean up operation after the closure of the application context
      */
    protected def cleanUp(): Unit = {
        try {
            FileUtils.deleteDirectory(new File("alf_data_dev"))
            FileUtils.deleteDirectory(new File("../alf_data_dev"))
        } catch {
            case e: Exception => logger.error("Error deleting directories: ", e)
        }
    }

}

/**
  * Extension of TestContextManager for exposing spring application context
  *
  * @param testClass test class
  * @tparam T type of test class
  */
class IxxusTestContextManager[T <: AnyRef](testClass: Class[T]) extends TestContextManager(testClass) {
    def applicationContext(): ConfigurableApplicationContext = getTestContext.getApplicationContext.asInstanceOf[ConfigurableApplicationContext]
}