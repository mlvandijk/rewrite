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
package org.openrewrite.java.internal;

import org.openrewrite.TreePrinter;
import org.openrewrite.java.tree.J;

public class MethodDeclToString {
    public static String toString(J.MethodDecl method) {
        //noinspection ConstantConditions
        return METHOD_PRINTER.print(method, null);
    }

    private static final JavaPrinter<Void> METHOD_PRINTER = new JavaPrinter<Void>(TreePrinter.identity()) {
        @Override
        public J visitMethod(J.MethodDecl method, Void unused) {
            visitModifiers(method.getModifiers(), unused);
            StringBuilder acc = getPrinterAcc();
            if (method.getModifiers().isEmpty()) {
                acc.append(' ');
            }
            visit("<", method.getTypeParameters(), ",", ">", unused);
            if (method.getReturnTypeExpr() != null) {
                acc.append(method.getReturnTypeExpr().printTrimmed()).append(' ');
            }
            acc.append(method.getSimpleName());
            visit("(", method.getParams(), ",", ")", unused);
            visit("throws", method.getThrows(), ",", "", unused);
            return method;
        }
    };
}
