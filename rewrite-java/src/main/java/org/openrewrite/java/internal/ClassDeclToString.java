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

public class ClassDeclToString {
    public static String toString(J.ClassDecl clazz) {
        //noinspection ConstantConditions
        return CLASS_DECL_PRINTER.print(clazz, null);
    }

    private static final JavaPrinter<Void> CLASS_DECL_PRINTER = new JavaPrinter<Void>(TreePrinter.identity()) {
        @Override
        public J visitClassDecl(J.ClassDecl classDecl, Void unused) {
            visitModifiers(classDecl.getModifiers(), unused);
            StringBuilder acc = getPrinterAcc();
            if (!classDecl.getModifiers().isEmpty()) {
                acc.append(' ');
            }
            switch (classDecl.getKind().getElem()) {
                case Class:
                    acc.append("class ");
                    break;
                case Enum:
                    acc.append("enum ");
                    break;
                case Interface:
                    acc.append("interface ");
                    break;
                case Annotation:
                    acc.append("@interface ");
                    break;
            }
            acc.append(classDecl.getName().printTrimmed());
            if (classDecl.getTypeParameters() != null) {
                visit("<", classDecl.getTypeParameters(), ",", ">", unused);
                acc.append(' ');
            }
            visit("extends", classDecl.getExtends(), unused);
            if (classDecl.getImplements() != null) {
                if (J.ClassDecl.Kind.Interface.equals(classDecl.getKind().getElem())) {
                    acc.append("extends");
                } else {
                    acc.append("implements");
                }
            }
            visit("", classDecl.getImplements(), ",", "", unused);
            return classDecl;
        }
    };
}
