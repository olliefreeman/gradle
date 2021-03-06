/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.launcher.exec

import org.gradle.BuildResult
import org.gradle.StartParameter
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.GradleInternal
import org.gradle.api.logging.LogLevel
import org.gradle.initialization.BuildRequestContext
import org.gradle.initialization.BuildRequestMetaData
import org.gradle.initialization.DefaultGradleLauncher
import org.gradle.initialization.GradleLauncherFactory
import org.gradle.internal.environment.GradleBuildEnvironment
import org.gradle.internal.invocation.BuildAction
import org.gradle.internal.invocation.BuildActionRunner
import org.gradle.internal.invocation.BuildController
import org.gradle.logging.StyledTextOutput
import org.gradle.logging.StyledTextOutputFactory
import org.gradle.util.Requires
import spock.lang.Specification
import spock.lang.Unroll

import static org.gradle.util.TestPrecondition.NOT_WINDOWS
import static org.gradle.util.TestPrecondition.WINDOWS

class InProcessBuildActionExecuterTest extends Specification {
    static final String DAEMON_DOCS_URL = "gradle-daemon-docs-url"

    final GradleLauncherFactory factory = Mock()
    final DefaultGradleLauncher launcher = Mock()
    final BuildRequestContext buildRequestContext = Mock()
    final BuildActionParameters param = Mock()
    final BuildRequestMetaData metaData = Mock()
    final BuildResult buildResult = Mock()
    final GradleInternal gradle = Mock()
    final BuildActionRunner actionRunner = Mock()
    final StartParameter startParameter = Mock()
    final StyledTextOutputFactory textOutputFactory = Mock()
    final GradleBuildEnvironment buildEnvironment = Mock()
    final DocumentationRegistry documentationRegistry = Mock()
    final StyledTextOutput textOutput = Mock()
    BuildAction action = Mock() {
        getStartParameter() >> startParameter
    }
    final InProcessBuildActionExecuter executer = new InProcessBuildActionExecuter(factory, actionRunner, textOutputFactory, buildEnvironment, documentationRegistry)

    def setup() {
        _ * param.buildRequestMetaData >> metaData
        _ * textOutputFactory.create(InProcessBuildActionExecuter, LogLevel.LIFECYCLE) >> textOutput
        _ * documentationRegistry.getDocumentationFor("gradle_daemon") >> DAEMON_DOCS_URL
    }

    def "creates launcher and forwards action to action runner"() {
        given:
        param.envVariables >> [:]

        when:
        def result = executer.execute(action, buildRequestContext, param)

        then:
        result == '<result>'

        and:
        1 * factory.newInstance(startParameter, buildRequestContext) >> launcher
        1 * actionRunner.run(action, !null) >> { BuildAction a, BuildController controller ->
            controller.result = '<result>'
        }
        1 * launcher.stop()
    }

    def "can have null result"() {
        given:
        param.envVariables >> [:]

        when:
        def result = executer.execute(action, buildRequestContext, param)

        then:
        result == null

        and:
        1 * factory.newInstance(startParameter, buildRequestContext) >> launcher
        1 * actionRunner.run(action, !null) >> { BuildAction a, BuildController controller ->
            assert !controller.hasResult()
            controller.result = null
            assert controller.hasResult()
        }
        1 * launcher.stop()
    }

    def "runs build when requested by action"() {
        given:
        param.envVariables >> [:]

        when:
        def result = executer.execute(action, buildRequestContext, param)

        then:
        result == '<result>'

        and:
        1 * factory.newInstance(startParameter, buildRequestContext) >> launcher
        1 * launcher.run() >> buildResult
        _ * buildResult.failure >> null
        _ * buildResult.gradle >> gradle
        _ * actionRunner.run(action, !null) >> { BuildAction a, BuildController controller ->
            assert controller.run() == gradle
            controller.result = '<result>'
        }
        1 * launcher.stop()
    }

    def "configures build when requested by action"() {
        given:
        param.envVariables >> [:]

        when:
        def result = executer.execute(action, buildRequestContext, param)

        then:
        result == '<result>'

        and:
        1 * factory.newInstance(startParameter, buildRequestContext) >> launcher
        1 * launcher.getBuildAnalysis() >> buildResult
        _ * buildResult.failure >> null
        _ * buildResult.gradle >> gradle
        _ * actionRunner.run(action, !null) >> { BuildAction a, BuildController controller ->
            assert controller.configure() == gradle
            controller.result = '<result>'
        }
        1 * launcher.stop()
    }

    def "cannot request configuration after build has been run"() {
        given:
        actionRunner.run(action, !null) >> { BuildAction a, BuildController controller ->
            controller.run()
            controller.configure()
        }

        when:
        executer.execute(action, buildRequestContext, param)

        then:
        IllegalStateException e = thrown()
        e.message == 'Cannot use launcher after build has completed.'

        and:
        1 * factory.newInstance(startParameter, buildRequestContext) >> launcher
        1 * launcher.run() >> buildResult
        1 * launcher.stop()
    }

    def "wraps build failure and cleans up"() {
        def failure = new RuntimeException()

        given:
        buildResult.failure >> failure

        when:
        executer.execute(action, buildRequestContext, param)

        then:
        ReportedException e = thrown()
        e.cause == failure

        and:
        1 * factory.newInstance(startParameter, buildRequestContext) >> launcher
        1 * launcher.run() >> buildResult
        _ * actionRunner.run(action, !null) >> { BuildAction a, BuildController controller ->
            controller.run()
        }
        1 * launcher.stop()
    }

    @Requires(NOT_WINDOWS)
    def "suggests using daemon when not on windows, daemon usage is not explicitly specified, CI env var is not specified and not running in a long running process"() {
        given:
        factory.newInstance(startParameter, buildRequestContext) >> launcher

        and:
        buildEnvironment.longLivingProcess >> false
        param.daemonUsageConfiguredExplicitly >> false
        param.envVariables >> [CI: null]

        when:
        executer.execute(action, buildRequestContext, param)

        then:
        1 * textOutput.println()

        and:
        1 * textOutput.println("This build could be faster, please consider using the daemon: $DAEMON_DOCS_URL")
    }

    @Requires(WINDOWS)
    def "does not suggests using daemon when on windows"() {
        given:
        factory.newInstance(startParameter, buildRequestContext) >> launcher

        and:
        buildEnvironment.longLivingProcess >> false
        param.daemonUsageConfiguredExplicitly >> false
        param.envVariables >> [CI: null]

        when:
        executer.execute(action, buildRequestContext, param)

        then:
        0 * textOutput.println()
    }

    @Unroll
    def "does not suggest using daemon [#longLivingProcess, #deamonUsageConfiguredExplicitly, #ciEnvValue]"() {
        given:
        factory.newInstance(startParameter, buildRequestContext) >> launcher

        and:
        buildEnvironment.longLivingProcess >> longLivingProcess
        param.daemonUsageConfiguredExplicitly >> deamonUsageConfiguredExplicitly
        param.getEnvVariables() >> [CI: ciEnvValue]

        when:
        executer.execute(action, buildRequestContext, param)

        then:
        0 * textOutput._

        where:
        [longLivingProcess, deamonUsageConfiguredExplicitly, ciEnvValue] << ([[true, false], [true, false], [null, "true"]].combinations() - [[false, false, null]])
    }
}
