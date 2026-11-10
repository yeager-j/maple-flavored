package mapleflavored.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runs the GameTest in a batch of its own, with the environment its {@code @GameTest} names. Put it on a test that
 * changes or reads state the whole server shares, such as its players, game rules or Subspaces, so no other test runs
 * beside it.
 *
 * <pre>{@code
 * @RunsAlone
 * @GameTest(environment = "maple_flavored_gametest:normal")
 * public void aWardenOpensASubspace(GameTestHelper helper) {
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RunsAlone {
}
