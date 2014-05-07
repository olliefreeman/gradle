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

import groovy.lang.Closure;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.internal.artifacts.configurations.DependencyConflictResolver;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.internal.ClosureSpec;

import java.util.Collection;
import java.util.Set;

public class ComponentReplacementTarget {

    private final Spec<ModuleIdentifier> from;
    private Collection<DependencyConflictResolver> replacementConflictResolvers;

    public ComponentReplacementTarget(String from, Collection<DependencyConflictResolver> replacementConflictResolvers) {
        final String[] split = from.split(":");
        this.from = new Spec<ModuleIdentifier>() {
            public boolean isSatisfiedBy(ModuleIdentifier element) {
                return element.getGroup().equals(split[0]) && element.getName().equals(split[1]);
            }
        };
        this.replacementConflictResolvers = replacementConflictResolvers;
    }

    public void into(final Object intoObject) {
        final Spec<ModuleIdentifier> into = convertInto(intoObject);

        replacementConflictResolvers.add(new DependencyConflictResolver() {
            public Spec<ModuleVersionIdentifier> getCandidateSelector(Set<ModuleVersionIdentifier> candidates) {
                boolean fromSatisfied = false;
                boolean intoSatisfied = false;
                for (ModuleVersionIdentifier candidate : candidates) {
                    if (from.isSatisfiedBy(candidate.getModule())) {
                        fromSatisfied = true;
                        continue;
                    }
                    if (into.isSatisfiedBy(candidate.getModule())) {
                        intoSatisfied = true;
                    }
                }
                if (fromSatisfied && intoSatisfied) {
                    //both 'from' and 'into' needs to present in the candidates
                    //we have a conflict and this resolver can handle it
                    return new Spec<ModuleVersionIdentifier>() {
                        public boolean isSatisfiedBy(ModuleVersionIdentifier element) {
                            return into.isSatisfiedBy(element.getModule());
                        }
                    };
                }
                //otherwise this resolver is not designed to handle this conflict or that there is no conflict.
                return null;
            }

            public Spec<ModuleIdentifier> getConflictingModulesSelector(ModuleIdentifier module) {
                if (from.isSatisfiedBy(module)) {
                    return into;
                }
                if (into.isSatisfiedBy(module)) {
                    return from;
                }
                return null;
            }
        });
    }

    private Spec<ModuleIdentifier> convertInto(Object intoObject) {
        Spec<ModuleIdentifier> into;
        if (intoObject instanceof String) {
            final String[] split = ((String) intoObject).split(":");
            into = new Spec<ModuleIdentifier>() {
                public boolean isSatisfiedBy(ModuleIdentifier element) {
                    return element.getGroup().equals(split[0]) && element.getName().equals(split[1]);
                }
            };
        } else if (intoObject instanceof Closure) {
            into = new ClosureSpec((Closure) intoObject);
        } else {
            throw new InvalidUserDataException("Don't know how to use provided component replacement 'into' value: " + intoObject);
        }
        return into;
    }
}
