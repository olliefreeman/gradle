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

package org.gradle.api.internal.artifacts.configurations;

import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.specs.Spec;

import java.util.Set;

//declares conflicts and provides means to resolve them. Is applicable to given ModuleIdentifier.
public interface DependencyConflictResolver extends Spec<ModuleIdentifier> {

    //provides a spec that can select from given candidates. Resulting spec must match at least one of the candidates.
    Spec<ModuleVersionIdentifier> getCandidateSelector(Set<ModuleVersionIdentifier> candidates);

    //provides a spec that can select conflicting modules for given module.
    Spec<ModuleIdentifier> getConflictingModulesSelector(ModuleIdentifier module);
}
