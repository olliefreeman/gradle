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

package org.gradle.api.internal.artifacts.ivyservice.resolveengine;

import org.apache.ivy.core.module.id.ModuleId;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.internal.artifacts.configurations.DependencyConflictResolver;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.Specs;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DefaultModuleConflictHandler implements ModuleConflictHandler {

    private ModuleConflictResolver defaultResolver;
    private final Collection<DependencyConflictResolver> resolvers;

    public DefaultModuleConflictHandler(ModuleConflictResolver defaultResolver, Collection<DependencyConflictResolver> resolvers) {
        this.defaultResolver = defaultResolver;
        this.resolvers = resolvers;
    }

    public <T extends ModuleRevisionResolveState> T select(Collection<? extends T> candidates) {
        Collection<T> filteredCandidates = new HashSet<T>(candidates);
        for (DependencyConflictResolver resolver : resolvers) {
            //checks if given resolver can be applied to *all* incoming *candidates* (not filtered)
            if (areHandledByGivenResolver(candidates, resolver)) {
                //get selector for given filtered candidates
                Spec<ModuleVersionIdentifier> selector = resolver.getCandidateSelector(toModuleVersionsSet(filteredCandidates));
                //filter candidates
                filteredCandidates = applyFilter(selector, filteredCandidates);
            }
        }
        if (filteredCandidates.isEmpty()) {
            throw new IllegalStateException("Conflict resolvers filtered out all candidates: " + toModuleVersionsSet(candidates));
        }
        return defaultResolver.select(filteredCandidates);
    }

    public Spec<ModuleIdentifier> getModuleConflicts(ModuleIdentifier module) {
        for (DependencyConflictResolver resolver : resolvers) {
            if (resolver.isSatisfiedBy(module)) {
                return resolver.getConflictingModulesSelector(module);
            }
        }
        return Specs.satisfyNone();
    }

    private static <T extends ModuleRevisionResolveState> Collection<T> applyFilter(Spec<ModuleVersionIdentifier> selector, Collection<T> filteredCandidates) {
        Set<T> out = new HashSet<T>();
        for (T c : filteredCandidates) {
            if(selector.isSatisfiedBy(c.getSelectedId())) {
                out.add(c);
            }
        }
        return out;
    }

    private static <T extends ModuleRevisionResolveState> Set<ModuleVersionIdentifier> toModuleVersionsSet(Collection<? extends T> candidates) {
        Set<ModuleVersionIdentifier> out = new HashSet<ModuleVersionIdentifier>();
        for (T c : candidates) {
            out.add(c.getSelectedId());
        }
        return out;
    }

    //checks if every candidate is handled by given resolver
    private static <T extends ModuleRevisionResolveState> boolean areHandledByGivenResolver(Collection<? extends T> candidates, DependencyConflictResolver resolver) {
        for (T candidate : candidates) {
            if (!resolver.isSatisfiedBy(candidate.getSelectedId().getModule())) {
                return false;
            }
        }
        return true;
    }
}
