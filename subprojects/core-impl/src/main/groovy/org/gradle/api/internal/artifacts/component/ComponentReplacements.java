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
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier;
import org.gradle.api.internal.artifacts.configurations.DependencyConflictResolver;
import org.gradle.api.internal.artifacts.ivyservice.resolutionstrategy.DefaultDependencyConflictDetails;
import org.gradle.api.specs.Spec;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ComponentReplacements {

    private final List<ComponentReplacement> replacements = new LinkedList<ComponentReplacement>();

    public ComponentReplacementConfigurer from(String moduleIdentifier) {
        return new ComponentReplacementConfigurer(moduleIdentifier, replacements);
    }

    public Collection<DependencyConflictResolver> toConflictResolvers() {
        Collection<DependencyConflictResolver> out = new LinkedList<DependencyConflictResolver>();
        for (ComponentReplacement replacement : replacements) {
            final String[] split = replacement.into.toString().split(":");
            Spec<ModuleVersionIdentifier> resolution = new Spec<ModuleVersionIdentifier>() {
                public boolean isSatisfiedBy(ModuleVersionIdentifier element) {
                    return element.getGroup().equals(split[0]) && element.getName().equals(split[1]);
                }
            };
            DefaultDependencyConflictDetails d = new DefaultDependencyConflictDetails();
            d.modules(replacement.from, replacement.into).resolution(resolution);
            out.add(d);
        }

        return out;
    }
}
