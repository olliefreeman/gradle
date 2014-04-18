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

package org.gradle.api.internal.artifacts.ivyservice.resolutionstrategy;

import groovy.lang.Closure;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier;
import org.gradle.api.internal.artifacts.configurations.DependencyConflictDetails;
import org.gradle.api.internal.artifacts.configurations.DependencyConflictResolver;
import org.gradle.api.specs.OrSpec;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.Specs;
import org.gradle.api.specs.internal.ClosureSpec;

import java.util.HashSet;
import java.util.Set;

public class DefaultDependencyConflictDetails implements DependencyConflictDetails, DependencyConflictResolver {

    private Set<Spec<ModuleIdentifier>> conflicts = new HashSet<Spec<ModuleIdentifier>>();
    private Spec<ModuleVersionIdentifier> resolutionSpec;

    public Spec<ModuleVersionIdentifier> getCandidateSelector(Set<ModuleVersionIdentifier> candidates) {
        return resolutionSpec;
    }

    public boolean isSatisfiedBy(ModuleIdentifier element) {
        return new OrSpec<ModuleIdentifier>(conflicts).isSatisfiedBy(element);
    }

    public Spec<ModuleIdentifier> getConflictingModulesSelector(ModuleIdentifier module) {
        Spec<ModuleIdentifier> match = null;
        for (Spec<ModuleIdentifier> conflict : conflicts) {
            if (conflict.isSatisfiedBy(module)) {
                match = conflict;
                break;
            }
        }
        if (match == null) {
            return Specs.satisfyNone();
        }
        Set<ModuleIdentifier> out = new HashSet<ModuleIdentifier>((Set) conflicts);
        out.remove(match);
        return new OrSpec(out);
    }

    public DependencyConflictDetails modules(Object... module) {
        for (Object m : module) {
            if (m instanceof CharSequence) {
                final String[] split = m.toString().split(":");
                conflicts.add(new Spec<ModuleIdentifier>() {
                    public boolean isSatisfiedBy(ModuleIdentifier element) {
                        return element.getGroup().equals(split[0]) && element.getName().equals(split[1]);
                    }
                });
            } else if (m instanceof Closure) {
                conflicts.add(new ClosureSpec<ModuleIdentifier>((Closure<?>) m));
            } else {
                throw new IllegalArgumentException("I don't know this module: " + m);
            }
        }
        return this;
    }

    public DependencyConflictDetails resolution(Spec<ModuleVersionIdentifier> resolutionSpec) {
        this.resolutionSpec = resolutionSpec;
        return this;
    }

    public DependencyConflictDetails resolution(Closure resolutionSpec) {
        return this.resolution(new ClosureSpec(resolutionSpec));
    }
}
