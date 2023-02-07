package org.openrewrite.java.filter;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class SourceApplicabilityFilter extends Recipe {

    @AllArgsConstructor
    enum Target {
        AllSource(Target.ALL_SOURCE),
        AllSourceWhenNonTestDetected(Target.ALL_SOURCE_IF_DETECTED_IN_NON_TEST),
        NonTestSource(Target.NON_TEST_SOURCE);

        static final String ALL_SOURCE = "All Source";
        static final String ALL_SOURCE_IF_DETECTED_IN_NON_TEST = "All Source if detected in Non Test Source";
        static final String NON_TEST_SOURCE = "Non-Test Source";

        private static Target fromString(@Nullable String target) {
            if (target == null) {
                return NonTestSource;
            }
            switch (target) {
                case ALL_SOURCE:
                    return AllSource;
                case ALL_SOURCE_IF_DETECTED_IN_NON_TEST:
                    return AllSourceWhenNonTestDetected;
                default:
                    return NonTestSource;
            }
        }

        private final String description;
    }

    @Option(
            displayName = "Target",
            description = "Specify whether all recipes scheduled in this run should apply to all sources or only non-test sources. Defaults to non-test sources.",
            required = false,
            valid = {
                    Target.ALL_SOURCE,
                    Target.ALL_SOURCE_IF_DETECTED_IN_NON_TEST,
                    Target.NON_TEST_SOURCE
            },
            example = Target.ALL_SOURCE
    )
    String target;

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        Target target = Target.fromString(getTarget());
        RecipeApplicableTest.addToExecutionContext(ctx, recipe -> new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                // If the target is Non Test Source, and this is a test source file, skip it.
                if ((Target.NonTestSource.equals(target) || Target.AllSourceWhenNonTestDetected.equals(target)) && isTestSource(cu.getSourcePath())) {
                    return cu;
                }
                boolean allMatch = true;
                for (TreeVisitor<?, ExecutionContext> applicableTest : recipe.getSingleSourceApplicableTests()) {
                    if (applicableTest.visit(cu, ctx, getCursor().getParentOrThrow()) == cu) {
                        allMatch = false;
                        break;
                    }
                }
                if (allMatch) {
                    // Unfortunately, this couples the Applicable Test to the Recipe visitor, which could be expensive.
                    // However, it guarantees that at least one non-test source file will be modified before attempting to apply it to the entire tree.
                    return (J.CompilationUnit) getVisitor(recipe).visitNonNull(cu, executionContext, getCursor().getParentOrThrow());
                }
                return cu;
            }
        });
        return super.visit(before, ctx);
    }

    private static TreeVisitor<?, ExecutionContext> getVisitor(Recipe recipe) {
        try {
            Method getVisitor = recipe.getClass().getDeclaredMethod("getVisitor");
            getVisitor.setAccessible(true);
            //noinspection unchecked
            return (TreeVisitor<?, ExecutionContext>) getVisitor.invoke(recipe);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    static boolean isTestSource(Path path) {
        return path.getFileSystem().getPathMatcher("glob:**/test/**").matches(path);
    }
}
