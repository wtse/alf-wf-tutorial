package com.ixxus.helloWorld

import com.ixxus.AbstractIntegrationFlatSpec

/**
  * Sample test.
  *
  * @author Giuseppe Iacono <giuseppe.iacono@ixxus.com>
  */
class HelloWorldIT extends AbstractIntegrationFlatSpec {

    private val STRING_1 = "hello world!"
    private val STRING_2 = "hello world!"

    "A Stack" should "pop values in last-in-first-out order" in {
        STRING_1 should equal(STRING_2)
    }

}
