/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.api.internal.artifacts.component;

import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.internal.artifacts.configurations.DependencyConflictResolver;
import org.gradle.api.specs.Spec;

import java.util.Set;

public class ComponentReplacement implements DependencyConflictResolver {
    final String from;
    final String into;

    public ComponentReplacement(String from, String into) {
        this.from = from;
        this.into = into;
    }

    public Spec<ModuleVersionIdentifier> getCandidateSelector(Set<ModuleVersionIdentifier> candidates) {
        return null;
    }

    public Spec<ModuleIdentifier> getConflictingModulesSelector(ModuleIdentifier module) {
        return null;
    }
}
