/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.integtests.tooling.r24

import org.gradle.integtests.fixtures.executer.GradleContextualExecuter
import org.gradle.integtests.tooling.fixture.ConfigurableOperation
import org.gradle.integtests.tooling.fixture.TargetGradleVersion
import org.gradle.integtests.tooling.fixture.ToolingApiSpecification
import org.gradle.integtests.tooling.fixture.ToolingApiVersion
import org.gradle.tooling.ProjectConnection
import org.gradle.util.TestPrecondition
import spock.lang.IgnoreIf

@IgnoreIf({ !GradleContextualExecuter.embedded || TestPrecondition.WINDOWS.fulfilled })
@ToolingApiVersion(">=2.4")
@TargetGradleVersion(">=2.4")
class DaemonUsageSuggestionCrossVersionTest extends ToolingApiSpecification {

    def "does not print suggestion to use the daemon when using tooling api in embedded mode"() {
        when:
        def operation = withConnection { ProjectConnection connection ->
            def build = connection.newBuild()
            def operation = new ConfigurableOperation(build)
            build.forTasks("help").run()
            operation
        }

        then:
        !operation.standardOutput.contains("This build could be faster, please consider using the daemon")
    }

}
