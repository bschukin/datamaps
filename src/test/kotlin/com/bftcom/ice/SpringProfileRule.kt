package com.bftcom.ice

import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.springframework.beans.DirectFieldAccessor
import org.springframework.core.env.Environment
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestContextManager
import org.springframework.test.context.junit4.statements.RunAfterTestMethodCallbacks


@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
/**
 * https://jira.spring.io/browse/SPR-11677
 */
annotation class IfSpringProfileActive(vararg val value:String)

/**
 * TestRule verifying Tests marked with IfSpringProfileActive are verified against currently active spring profile
 */
class SpringProfileRule private constructor(private var env: Environment?) : TestRule {

    override fun apply(base: Statement, description: Description): Statement {

        val profileValue = description.getAnnotation(IfSpringProfileActive::class.java)
        val requiredProfiles: Set<String> = profileValue?.value?.toSet() ?: setOf()

        return object : Statement() {

            @Throws(Throwable::class)
            override fun evaluate() {

                if (!requiredProfiles.isEmpty()) {
                    initEnvironmentWhenNotSet(base)
                    verify(requiredProfiles)
                }

                base.evaluate()
            }
        }

    }

    protected fun initEnvironmentWhenNotSet(base: Statement) {

        if (env == null && base is RunAfterTestMethodCallbacks) {

            // there should be a better way of doing this...
            val contextManager = DirectFieldAccessor(base)
                    .getPropertyValue("testContextManager") as TestContextManager
            val testContext = DirectFieldAccessor(contextManager).getPropertyValue("testContext") as TestContext
            env = testContext.applicationContext.environment
        }
    }

    @Throws(Throwable::class)
    protected fun verify(requiredProfile: Set<String>) {

        requiredProfile.forEach {
            if (env!!.acceptsProfiles(it)) {
                return
            }
        }
        throw AssumptionViolatedException(String.format("Profile %s is currently not active", requiredProfile))
    }

    companion object {

        fun forSpringJunitClassRunner(): SpringProfileRule {
            return SpringProfileRule(null)
        }

        fun forEnvironment(env: Environment): SpringProfileRule {
            return SpringProfileRule(env)
        }
    }

}