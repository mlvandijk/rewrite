/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.search;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.util.Set;

/**
 * This recipe will find all fields that have a type matching the fully qualified type name and mark those fields with
 * {@link SearchResult} markers.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FindField extends Recipe {

    private final String fullyQualifiedTypeName;

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new FindFieldsVisitor();
    }

    public static Set<J.VariableDecls> find(J j, String clazz) {
        //noinspection ConstantConditions
        return ((FindFieldsVisitor) new FindField(clazz).getVisitor())
                .visit(j, ExecutionContext.builder().build())
                .findMarkedWith(SearchResult.class);
    }

    private final class FindFieldsVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.VariableDecls visitMultiVariable(J.VariableDecls multiVariable, ExecutionContext ctx) {
            if (multiVariable.getTypeExpr() instanceof J.MultiCatch) {
                return multiVariable;
            }
            if (multiVariable.getTypeExpr() != null && TypeUtils.hasElementType(multiVariable.getTypeExpr()
                    .getType(), fullyQualifiedTypeName)) {
                return multiVariable.mark(new SearchResult());
            }
            return multiVariable;
        }
    }
}
