package io.split.android.engine.matchers.collections;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.split.android.engine.matchers.collections.EqualToSetMatcher;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class EqualToSetMatcherTest {
    @Test
    public void works_for_sets() {
        Set<String> set = new HashSet<>();
        set.add("first");
        set.add("second");

        EqualToSetMatcher matcher = new EqualToSetMatcher(set);

        assertThat(matcher.match(null, null, null, null), is(false));

        Set<String> argument = new HashSet<>();
        assertThat(matcher.match(argument, null, null, null), is(false));

        argument.add("second");
        assertThat(matcher.match(argument, null, null, null), is(false));

        argument.add("first");
        assertThat(matcher.match(argument, null, null, null), is(true));

        argument.add("third");
        assertThat(matcher.match(argument, null, null, null), is(false));
    }

    @Test
    public void works_for_sets_same_order() {
        Set<String> set = new HashSet<>();
        set.add("first");
        set.add("second");

        EqualToSetMatcher matcher = new EqualToSetMatcher(set);

        assertThat(matcher.match(null, null, null, null), is(false));

        Set<String> argument = new HashSet<>();
        assertThat(matcher.match(argument, null, null, null), is(false));

        argument.add("first");
        assertThat(matcher.match(argument, null, null, null), is(false));

        argument.add("second");
        assertThat(matcher.match(argument, null, null, null), is(true));

        argument.add("third");
        assertThat(matcher.match(argument, null, null, null), is(false));
    }

    @Test
    public void works_for_lists() {
        List<String> list = new ArrayList<>();
        list.add("first");
        list.add("second");

        EqualToSetMatcher matcher = new EqualToSetMatcher(list);

        assertThat(matcher.match(null, null, null, null), is(false));

        List<String> argument = new ArrayList<>();
        assertThat(matcher.match(argument, null, null, null), is(false));

        argument.add("second");
        assertThat(matcher.match(argument, null, null, null), is(false));

        argument.add("first");
        assertThat(matcher.match(argument, null, null, null), is(true));

        argument.add("third");
        assertThat(matcher.match(argument, null, null, null), is(false));
    }

    @Test
    public void works_for_empty_paramter() {
        List<String> list = new ArrayList<>();

        EqualToSetMatcher matcher = new EqualToSetMatcher(list);

        assertThat(matcher.match(null, null, null, null), is(false));

        List<String> argument = new ArrayList<>();
        assertThat(matcher.match(argument, null, null, null), is(true));

        argument.add("second");
        assertThat(matcher.match(argument, null, null, null), is(false));

        argument.add("first");
        assertThat(matcher.match(argument, null, null, null), is(false));

        argument.add("third");
        assertThat(matcher.match(argument, null, null, null), is(false));
    }
}
