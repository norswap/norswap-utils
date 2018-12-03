package norswap.utils;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A parent class for test classes that implements a few handy assertion methods.
 *
 * The key point of this compared to the usual frameworks is the ability to trim the stack traces of
 * the thrown assertion errors, as well as other exceptions (see below and {@link
 * #trim_stack_trace}). This is fully compatible with TestNG (maybe with JUnit, haven't checked).
 *
 * <p>The assertion methods should be pretty obvious and repetitive and so they are not documented
 * individually. Here are a few precisions that apply to them.
 *
 * <p>Whenever an integer {@code peel} parameter is present, it indicates that this many items
 * should be removed from the bottom of the stack trace (most recently called methods) of the thrown
 * assertion error.
 *
 * <p>All assertion methods take care of peeling themselves off (as only the assertion call site
 * is really interesting), so you do not need to account for them in {@code peel}.
 *
 * <p>Whenever the assertion message is supplied by a {@link Supplier}, the supplier is only
 * called if the assertion is violated, and only once.
 *
 * <p>In assertion methods names, equals refers to the {@link Object#equals} while "same" refers
 * to identity comparison (I reused the JUnit/TestNG terminology).
 *
 * <p>For equality comparisons, the values being compared are added on a new line after the supplied
 * error message, in the same format that TestNG uses - making it compatible with its plugin
 * (although for some reason the plugin only offer values comparisons when equality constraints are
 * violated, not different constraints). Might be compatible with JUnit as well, but I haven't
 * checked. Pull requests welcome.
 *
 * <p>Also see the documentation of {@link #trace_separator} and {@link #peel_test_runner}
 * for additional usage notices.
 */
public abstract class TestFixture
{
    // ---------------------------------------------------------------------------------------------

    /**
     * A separator to be added at the end of assertion error messages, to separate them from the
     * stack trace of the assertion error itself. Especially handy if the error message
     * ends with indented items itself. Defaults to the empty string.
     */
    public String trace_separator = "";

    // ---------------------------------------------------------------------------------------------

    /**
     * Whether to remove stack trace elements pertaining to the test runner (basically anything
     * under the test class in stack trace) from the assertion errors' stack traces. Defaults to
     * true.
     *
     * <p>If you want to apply the same treatment to other exceptions, use {@link
     * #trim_stack_trace}.
     */
    public boolean peel_test_runner = true;

    // ---------------------------------------------------------------------------------------------

    /**
     * Trims the stack trace of the given throwable, removing {@code peel} stack trace
     * elements at the top of the stack trace (the most recently called methods),
     * and removes all stack trace elements under the last occurence of the class whose full
     * name (the dot-separated "binary name") is equal to {@code bottom_class}, if it isn't null.
     */
    public static void trim_stack_trace (Throwable t, int peel, String bottom_class)
    {
        StackTraceElement[] trace = t.getStackTrace();
        int new_end = trace.length;

        for (int i = trace.length - 1; i >= 0; --i)
            if (trace[i].getClassName().equals(bottom_class)) {
                new_end = i + 1;
                break;
            }

        t.setStackTrace(Arrays.copyOfRange(trace, peel, new_end));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Throws an {@link AssertionError} with the given message. Removes itself and {@code peel}
     * additional stack trace elements at the top of the stack trace, and honors the {@link
     * #peel_test_runner} setting.
     */
    public void throw_assertion (int peel, String msg)
    {
        AssertionError error = new AssertionError(msg + trace_separator);
        trim_stack_trace(error, peel + 1, peel_test_runner ? this.getClass().getName() : null);
        throw error;
    }

    // ---------------------------------------------------------------------------------------------

    public void assert_true (boolean condition, int peel, Supplier<String> msg)
    {
        if (!condition) throw_assertion(peel + 1, msg.get());
    }

    // ---------------------------------------------------------------------------------------------

    public void assert_true (boolean condition, Supplier<String> msg)
    {
        if (!condition) throw_assertion(1, msg.get());
    }

    // ---------------------------------------------------------------------------------------------

    public void assert_true (boolean condition, String msg)
    {
        if (!condition) throw_assertion(1, msg);
    }

    // ---------------------------------------------------------------------------------------------

    public void assert_true (boolean condition)
    {
        if (!condition) throw_assertion(1, "");
    }

    // ---------------------------------------------------------------------------------------------

    public void assert_equals (Object actual, Object expected, int peel, Supplier<String> msg)
    {
        if (!Objects.deepEquals(actual, expected))
            throw_assertion(peel + 1,
                msg.get() + "\nexpected [" + expected + "] but found [" + actual + "]");
    }

    // ---------------------------------------------------------------------------------------------

    public void assert_equals (Object actual, Object expected, Supplier<String> msg)
    {
        assert_equals(actual, expected, 1, msg);
    }

    // ---------------------------------------------------------------------------------------------

    public void assert_equals (Object actual, Object expected, String msg)
    {
        assert_equals(actual, expected, 1, () -> msg);
    }

    // ---------------------------------------------------------------------------------------------

    public void assert_equals (Object actual, Object expected)
    {
        assert_equals(actual, expected, 1, () -> "");
    }

    // ---------------------------------------------------------------------------------------------

    public void assert_not_equals (Object actual, Object expected, int peel, Supplier<String> msg)
    {
        if (Objects.deepEquals(actual, expected))
            throw_assertion(peel + 1,
                msg.get() + "\nexpected not same [" + expected + "] but found [" + actual + "]");
    }

    // ---------------------------------------------------------------------------------------------

    public void assert_not_equals (Object actual, Object expected, Supplier<String> msg)
    {
        assert_not_equals(actual, expected, 1, msg);
    }

    // ---------------------------------------------------------------------------------------------

    public void assert_not_equals (Object actual, Object expected, String msg)
    {
        assert_not_equals(actual, expected, 1, () -> msg);
    }

    // ---------------------------------------------------------------------------------------------

    public void assert_not_equals (Object actual, Object expected)
    {
        assert_not_equals(actual, expected, 1, () -> "");
    }

    // ---------------------------------------------------------------------------------------------

    public void assert_same (Object actual, Object expected, int peel, Supplier<String> msg)
    {
        if (actual != expected)
            throw_assertion(peel + 1,
                msg.get() + "\nexpected [" + expected + "] but found [" + actual + "]");
    }

    // ---------------------------------------------------------------------------------------------

    public void assert_same (Object actual, Object expected, Supplier<String> msg)
    {
        assert_same(actual, expected, 1, msg);
    }

    // ---------------------------------------------------------------------------------------------

    public void assert_same (Object actual, Object expected, String msg)
    {
        assert_same(actual, expected, 1, () -> msg);
    }

    // ---------------------------------------------------------------------------------------------

    public void assert_same (Object actual, Object expected)
    {
        assert_same(actual, expected, 1, () -> "");
    }

    // ---------------------------------------------------------------------------------------------

    public void assert_not_same (Object actual, Object expected, int peel, Supplier<String> msg)
    {
        if (actual == expected)
            throw_assertion(peel + 1,
                    msg.get() + "\nexpected not same [" + expected + "] but found [" + actual + "]");
    }

    // ---------------------------------------------------------------------------------------------

    public void assert_not_same (Object actual, Object expected, Supplier<String> msg)
    {
        assert_not_same(actual, expected, 1, msg);
    }

    // ---------------------------------------------------------------------------------------------

    public void assert_not_same (Object actual, Object expected, String msg)
    {
        assert_not_same(actual, expected, 1, () -> msg);
    }

    // ---------------------------------------------------------------------------------------------

    public void assert_not_same (Object actual, Object expected)
    {
        assert_not_same(actual, expected, 1, () -> "");
    }

    // ---------------------------------------------------------------------------------------------
}