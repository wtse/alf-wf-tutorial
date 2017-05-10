package com.ixxus

import com.ixxus.helloWorld.HelloWorldIT
import org.springframework.test.context.ContextConfiguration

/**
  * <p>ScalaIntegrationSuite class</p>
  * <p>Contains all the suites to be executed as part of the integration tests</p>
  *
  * @author Antonio David Perez Morales <antonio.perez@ixxus.com>
  */
@ContextConfiguration(Array("classpath:alfresco/application-context.xml"))
class ScalaIntegrationSuite
        extends AbstractScalaIntegrationSuite[ScalaIntegrationSuite](classOf[ScalaIntegrationSuite])(
                new HelloWorldIT
        )
