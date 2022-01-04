package io.snice.testing.core.check;

import io.snice.testing.core.Session;
import io.snice.testing.core.common.Pair;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CheckTest {

    /**
     * All checks succeed and all has "saveAs"
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 10})
    public void testCheckAllHappy(final int count) {
        final var checks = generateChecks(count);
        final var session = Check.check("hello world", new Session("my session"), checks);
        final var counts = countSuccessVsFailures(session);
        assertThat(counts.left(), is(0));
        assertThat(counts.right(), is(count));
        ensureAttributesCorrect(session, count);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 10})
    public void testCheckAllHappyButNotSavedAs(final int count) {
        final var checks = generateChecks(false, count);
        final var session = Check.check("hello world", new Session("my session"), checks);
        final var counts = countSuccessVsFailures(session);
        assertThat(counts.left(), is(0));
        assertThat(counts.right(), is(count));
        assertThat(session.attributes().isEmpty(), is(true));
    }

    /**
     * All checks fail
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 10})
    public void testCheckAllFailures(final int count) {
        final var failedIndeces = range(0, count);
        final var checks = generateChecks(count, failedIndeces);
        final var session = Check.check("hello world", new Session("my session"), checks);
        final var counts = countSuccessVsFailures(session);
        assertThat(counts.left(), is(count));
        assertThat(counts.right(), is(0));
        ensureAttributesCorrect(session, 0);
    }

    /**
     * All odd numbered checks fail, all even numbered succeeds.
     *
     * @param count
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 9, 10, 11})
    public void testCheckEveryOtherSucceedsFail(final int count) {
        final int succeeds = count - (count / 2);
        final int fails = count - succeeds;
        final var failedIndexes = oddOrEven(true, 0, count);

        final var checks = generateChecks(count, failedIndexes);
        final var session = Check.check("hello world", new Session("my session"), checks);
        final var counts = countSuccessVsFailures(session);
        assertThat(counts.left(), is(fails));
        assertThat(counts.right(), is(succeeds));

        // these are all the indexes that should have been successful and as such,
        // should exist and saved in the session.
        final var succeededIndexes = oddOrEven(false, 0, count);
        for (int i = 0; i < succeededIndexes.length; ++i) {
            final int index = succeededIndexes[i];
            assertThat(session.attributes("name-" + index).get(), is("value-" + index));
        }
    }

    private Pair<Integer, Integer> countSuccessVsFailures(final Session session) {
        final long failures = session.checkResults().stream().filter(CheckResult::isFailure).count();
        final long success = session.checkResults().size() - failures;
        return new Pair<>((int) failures, (int) success);
    }

    private int[] oddOrEven(final boolean isOdd, final int start, final int stop) {
        final var odd = IntStream.range(start, stop).boxed().filter(i -> i % 2 == (isOdd ? 1 : 0)).collect(Collectors.toList());
        final int[] array = new int[odd.size()];
        for (int i = 0; i < odd.size(); ++i) {
            array[i] = odd.get(i);
        }
        return array;
    }

    private int[] range(final int start, final int stop) {
        final int[] array = new int[stop - start];
        for (int i = start; i < stop; ++i) {
            array[i - start] = i;
        }
        return array;
    }

    private static void ensureAttributesCorrect(final Session session, final int countHappySavedSessions) {
        for (int i = 0; i < countHappySavedSessions; ++i) {
            assertThat(session.attributes("name-" + i).get(), is("value-" + i));
        }
    }

    private List<Check<String>> generateChecks(final int count, final int... failureIndex) {
        return generateChecks(true, count, failureIndex);
    }

    /**
     * Generate X number of checks that all report a happy result with an extracted value and a "saveAs" name, both
     * based on the index of the given check.
     *
     * @param saveAs       whether the "extracted" value should be saved
     * @param count        how checks many to generate
     * @param failureIndex indicate which one of the generated checks should fail.
     * @return
     */
    private List<Check<String>> generateChecks(final boolean saveAs, final int count, final int... failureIndex) {
        final var checks = new ArrayList<Check<String>>();
        for (int i = 0; i < count; ++i) {
            final Check<String> check = mock(Check.class);
            final Optional<String> maybeSaveAs = saveAs ? Optional.of("name-" + i) : empty();
            final var validation = isIn(i, failureIndex) ?
                    new CheckResult(check, empty(), maybeSaveAs, Optional.of("failed-" + i)) :
                    new CheckResult(check, Optional.of("value-" + i), maybeSaveAs, empty());
            when(check.check(any(), any())).thenReturn(validation);
            checks.add(check);
        }
        return checks;
    }

    private static boolean isIn(final int x, final int... array) {
        if (array == null) {
            return false;
        }

        return Arrays.stream(array).filter(i -> i == x).findAny().isPresent();
    }

}