/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rq.multipart;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.BiFunction;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

/**
 * Multipart processor state.
 * <p>
 * While parsing multipart request we may enter different states:
 * <ol>
 * <li>Initial state, didn't start processing yet</li>
 * <li>Parsing preamble before first part started (should be ignored)</li>
 * <li>Parsing request part, between boundaries. Also, we need to know if we are
 *   <ul>
 *   <li>Just started a new part parsing to create new part with part headers accumulator</li>
 *   <li>Finished part, to notify downstream on complete</li>
 *   </ul>
 * </li>
 * <li>Parsing epilogue, after the last part and double dash (should be ignored)</li>
 * </ol>
 * This class defines all state and its transition and provide method to patch and check the state.
 * </p>
 * @since 1.0
 * @checkstyle MagicNumberCheck (50 lines)
 */
final class State {

    /**
     * Initial state.
     */
    private static final int INIT = 1;

    /**
     * Processing preamble.
     */
    private static final int PREAMBLE = 1 << 1;

    /**
     * Processing part.
     */
    private static final int PART = 1 << 2;

    /**
     * Finished part.
     */
    private static final int END = 1 << 3;

    /**
     * Starting part.
     */
    private static final int START = 1 << 4;

    /**
     * Processing epilogue.
     */
    private static final int EPILOGUE = 1 << 5;

    /**
     * Patch factories for multipart chunks.
     */
    private static final Collection<BiFunction<ByteBuffer, Boolean, Patch>> PATCHERS =
        Collections.unmodifiableList(
            Arrays.asList(
                (buf, end) -> Patch.equal(State.INIT).addFlags(State.PREAMBLE),
                (buf, end) -> Patch.hasFlag(State.INIT).removeFlags(State.INIT),
                (buf, end) -> Patch.hasFlag(State.START).removeFlags(State.START),
                (buf, end) -> Patch.equal(State.INIT).addFlags(State.START),
                (buf, end) -> Patch.hasFlag(State.END).addFlags(State.START),
                (buf, end) -> Patch.hasFlag(State.PREAMBLE | State.END).removeFlags(State.PREAMBLE),
                (buf, end) -> new Patch(
                    state -> (state & (State.END | State.EPILOGUE)) == State.END,
                    state -> {
                        final ByteBuffer dup = buf.duplicate();
                        // epilogue starts with double minus `--` seq after end of previous part
                        int patch = state;
                        if (dup.remaining() >= 2 && dup.get() == '-' && dup.get() == '-') {
                            patch |= State.EPILOGUE;
                        }
                        return patch;
                    }
                ),
                (buf, end) -> new Patch(
                    state -> ((state & State.END) == State.END) != end,
                    state -> {
                        final int patch;
                        if (end) {
                            patch = state | State.END;
                        } else {
                            patch = state & ~State.END;
                        }
                        return patch;
                    }
                ),
                (buf, end) -> new Patch(state -> (state & (State.PREAMBLE | State.EPILOGUE)) == 0)
                    .addFlags(State.PART),
                (buf, end) -> new Patch(state -> (state & (State.PREAMBLE | State.EPILOGUE)) != 0)
                    .removeFlags(State.PART)
            )
        );

    /**
     * Current state flags.
     */
    private volatile int flags;

    /**
     * New init state.
     */
    State() {
        this.flags = State.INIT;
    }

    @Override
    public String toString() {
        final StringBuilder res = new StringBuilder(38);
        if (this.hasFlag(State.INIT)) {
            res.append("INIT,");
        }
        if (this.hasFlag(State.PREAMBLE)) {
            res.append("PREAMBLE,");
        }
        if (this.hasFlag(State.PART)) {
            res.append("PART,");
        }
        if (this.hasFlag(State.END)) {
            res.append("END,");
        }
        if (this.hasFlag(State.START)) {
            res.append("START,");
        }
        if (this.hasFlag(State.EPILOGUE)) {
            res.append("EPILOGUE,");
        }
        return res.toString();
    }

    /**
     * Patch current state with new chunk.
     * @param buf Next chunk
     * @param end End of part
     */
    void patch(final ByteBuffer buf, final boolean end) {
        for (final BiFunction<ByteBuffer, Boolean, Patch> patcher : State.PATCHERS) {
            final ByteBuffer copy = buf.duplicate();
            final Patch patch = patcher.apply(copy, end);
            if (patch.test(this.flags)) {
                this.flags = patch.applyAsInt(this.flags);
            }
        }
    }

    /**
     * Current state should be ignored, since it's either preamble or
     * epilogue.
     * @return True if ignore
     */
    boolean shouldIgnore() {
        return (this.flags & (State.PREAMBLE | State.EPILOGUE)) != 0;
    }

    /**
     * Is in initial state.
     * @return True if current state is initial
     */
    boolean isInit() {
        return this.hasFlag(State.INIT);
    }

    /**
     * Check if state is in start of the part.
     * @return True if in start
     */
    boolean started() {
        return this.hasFlag(State.START);
    }

    /**
     * Check if state in end of the part.
     * @return True if in the end
     */
    boolean ended() {
        return this.hasFlag(State.END);
    }

    /**
     * Check state has flag.
     * @param flag Flag to check
     * @return True if has
     */
    private boolean hasFlag(final int flag) {
        return (this.flags & flag) == flag;
    }

    /**
     * Patch for state, it matches state to apply and update state with new flags.
     * @since 1.0
     */
    private static final class Patch implements IntPredicate, IntUnaryOperator {

        /**
         * Empty patch which match any state.
         */
        static final Patch ANY = new Patch(state -> true);

        /**
         * Predicate to match current state.
         */
        private final IntPredicate predicate;

        /**
         * Operator to update current state.
         */
        private final IntUnaryOperator operator;

        /**
         * New patch with predicate and operator.
         * @param predicate To match current state
         */
        Patch(final IntPredicate predicate) {
            this(predicate, state -> state);
        }

        /**
         * New patch with predicate and operator.
         * @param predicate To match current state
         * @param operator To update current state
         */
        Patch(final IntPredicate predicate, final IntUnaryOperator operator) {
            this.operator = operator;
            this.predicate = predicate;
        }

        @Override
        public boolean test(final int state) {
            return this.predicate.test(state);
        }

        @Override
        public int applyAsInt(final int state) {
            return this.operator.applyAsInt(state);
        }

        /**
         * Create new patch copy, that apply flags to state.
         * @param flags To apply
         * @return State copy with new operator
         */
        Patch addFlags(final int flags) {
            return new Patch(this.predicate, this.operator.andThen(state -> state | flags));
        }

        /**
         * Create new patch copy, that removes flags from state.
         * @param flags To apply
         * @return State copy with new operator
         */
        Patch removeFlags(final int flags) {
            return new Patch(this.predicate, this.operator.andThen(state -> state & ~flags));
        }

        /**
         * Creates new patch matches states equal to provided state.
         * @param val Value to test against current state
         * @return New patch instance
         */
        static Patch equal(final int val) {
            return new Patch(test -> test == val, state -> state);
        }

        /**
         * Creates new patch matches state by having flags.
         * @param flag Flag that state should have to match
         * @return New patch instance
         */
        static Patch hasFlag(final int flag) {
            return new Patch(test -> (test & flag) == flag, state -> state);
        }

        /**
         * Creates new patch matches state by having no flags.
         * @param flag Flag that state should not have to match
         * @return New patch instance
         */
        static Patch hasNoFlag(final int flag) {
            return new Patch(test -> (test & flag) == 0, state -> state);
        }
    }
}
