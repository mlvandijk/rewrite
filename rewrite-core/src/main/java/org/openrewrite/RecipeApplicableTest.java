package org.openrewrite;

@FunctionalInterface
public interface RecipeApplicableTest {
    TreeVisitor<?, ExecutionContext> getTest(Recipe recipe);

    static void addToExecutionContext(ExecutionContext ctx, RecipeApplicableTest test) {
        ctx.putMessageInSet(RecipeApplicableTest.class.getSimpleName(), test);
    }
}
