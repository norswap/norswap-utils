package norswap.utils.visitors;

import norswap.utils.NArrays;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static norswap.utils.Util.cast;
import static norswap.utils.visitors.WalkVisitType.*;

/**
 * An analog to {@link Visitor} that implements a tree walker that calls an operation
 * on every node in the tree hiearchy. The operation that has different behaviours
 * (<i>specializations</i>) for different subclasses of {@code T} (the node type), as well as
 * for different {@link WalkVisitType visit types}.
 *
 * <p>The walker can call operations on each node before visiting its children ({@link
 * WalkVisitType#PRE_VISIT}), after visiting its children ({@link WalkVisitType#POST_VISIT}), or
 * in-between every pair of successive children ({@link WalkVisitType#IN_VISIT}). It's possible to
 * combine multiple visit modalities, and all those that apply should be passed to {@link
 * #Walker(WalkVisitType...) the constructor}.
 *
 * <p>Each specialization is represented by an instance of {@link Consumer}. They are registered by
 * calling {@link #register(Class, WalkVisitType, Consumer)}. Specializations for a class are only
 * operational for values that have that specific class — inheritance does not enter into account
 * when dispatching the operation. Each specialization is also specific to one of the visit type
 * mentionned above.
 *
 * <p>It's also possible to specify a specialization applicable for all visit types. In this case,
 * the {@link WalkVisitType} will be passed as a parameter to the specialization, which has type
 * {@link BiConsumer}{@code <WalkVisitType, T>}. Use method {@link #register_fallback( BiConsumer)}
 * for this. If you choose to use this modality for a given class, you must not also use per-visit
 * specialization for that class!
 *
 * <p>If a specialization for a (class, visit type) combination does not exist, a fallback
 * specialization can be called. Fallback specializations are registered by calling {@link
 * #register_fallback(WalkVisitType, Consumer)} (per-visit-type fallbacks) and {@link
 * #register_fallback(BiConsumer)} (generic fallback). If a fallback can't be found, an {@link
 * IllegalArgumentException} is thrown.
 *
 * <p>The class offers the {@link #walk} method, which calls the visitor operation on the node
 * (possibly multiple times, see below) and recursively calls itself on the children of the
 * node.
 *
 * <p>To use this class, you must subclass it and override the {@link #children} method to
 * instruct the walker how to find the children of a node.
 *
 * <p>Specialization must not call the {@link #walk} method of their own walker!
 */
public abstract class Walker<T>
{
    // ---------------------------------------------------------------------------------------------

    /** Does this walker perform pre-visits? ({@link WalkVisitType#PRE_VISIT}) */
    public final boolean pre_visit;
    /** Does this walker perform post-visits? ({@link WalkVisitType#POST_VISIT}) */
    public final boolean post_visit;
    /** Does this walker perform in-visits? ({@link WalkVisitType#IN_VISIT}) */
    public final boolean in_visit;

    // ---------------------------------------------------------------------------------------------

    protected Walker (WalkVisitType... visit_types)
    {
        if (visit_types.length == 0)
            throw new IllegalArgumentException("no visit types specified");

        this.pre_visit  = NArrays.contains(visit_types, PRE_VISIT);
        this.post_visit = NArrays.contains(visit_types, POST_VISIT);
        this.in_visit   = NArrays.contains(visit_types, IN_VISIT);
    }

    // ---------------------------------------------------------------------------------------------

    private final static class Specializations<T>
    {
        Consumer<? super T> pre;
        Consumer<? super T> post;
        Consumer<? super T> in;
        BiConsumer<WalkVisitType, ? super T> all;

        void call (WalkVisitType visit_type, T node)
        {
            switch (visit_type) {
                case PRE_VISIT:
                    if (pre != null)  { pre.accept(node);  return; }
                    break;
                case POST_VISIT:
                    if (post != null) { post.accept(node); return; }
                    break;
                case IN_VISIT:
                    if (in != null)   { in.accept(node);   return; }
                    break;
                }

            if (all != null) all.accept(visit_type, node);
            else throw new IllegalArgumentException(String.format(
                    "no valid specialization found for node: %s (visit type: %s)",
                    node, visit_type));
        }

        void set (WalkVisitType visit_type, Consumer<? super T> specialization)
        {
            switch (visit_type) {
                case PRE_VISIT:
                    pre = specialization;  break;
                case POST_VISIT:
                    post = specialization; break;
                case IN_VISIT:
                    in = specialization;   break;
            }
        }
    }

    // ---------------------------------------------------------------------------------------------

    private final HashMap<Class<? extends T>, Specializations<T>> dispatch = new HashMap<>();

    private final Specializations<T> fallback_specializations = new Specializations<>();

    private WalkVisitType visit_type;

    // ---------------------------------------------------------------------------------------------

    /**
     * Specifies how to retrieve the children of a node.
     */
    public abstract Iterable<T> children (T node);

    // ---------------------------------------------------------------------------------------------

    private void visit (T node)
    {
        Specializations<T> specializations = dispatch.get(node.getClass());
        if (specializations == null) specializations = fallback_specializations;
        specializations.call(visit_type, node);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Walk the tree-like hierarchy rooted at the node, see {@link Walker}.
     */
    public void walk (T node)
    {
        if (pre_visit) {
            visit_type = PRE_VISIT;
            visit(node);
        }

        boolean first = true;
        for (T child: children(node)) {
            if (in_visit && !first) {
                visit_type = IN_VISIT;
                visit(node);
            } else {
                first = false;
            }
            walk(child);
        }
        if (post_visit) {
            visit_type = POST_VISIT;
            visit(node);
        }
    }
    // ---------------------------------------------------------------------------------------------

    /**
     * Register a specialization for the given visit type and the given class.
     */
    public <S extends T> Walker<T> register
            (Class<S> klass, WalkVisitType visit_type, Consumer<? super S> specialization)
    {
        Specializations<T> specializations =
                dispatch.computeIfAbsent(klass, k -> new Specializations<>());
        if (specializations.all != null)
            throw new IllegalStateException("Trying to mix per-visit-type and generic specializations.");
        specializations.set(visit_type, cast(specialization));
        return this;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Register a specialization for the given class, that will be called for all visit types
     * (the visit type will be supplied to the specialization as first parameter).
     */
    public <S extends T> Walker<T> register
            (Class<S> klass, BiConsumer<WalkVisitType, ? super S> specialization)
    {
        Specializations<T> s = dispatch.computeIfAbsent(klass, k -> new Specializations<>());
        if (s.pre != null || s.post != null || s.in != null)
            throw new IllegalStateException("Trying to mix per-visit-type and generic specializations.");
        s.all = cast(specialization);
        return this;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Register the fallback specialization for all visit types (the visit type will be supplied to
     * the specialization as first parameter).
     */
    public Walker<T> register_fallback (WalkVisitType visit_type, Consumer<? super T> fallback)
    {
        if (fallback_specializations.all != null)
            throw new IllegalStateException("Trying to mix per-visit-type and generic fallbacks.");
        fallback_specializations.set(visit_type, fallback);
        return this;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Register the fallback specialization for the given visit type and the given class.
     */
    public Walker<T> register_fallback (BiConsumer<WalkVisitType, ? super T> fallback)
    {
        Specializations<T> s = fallback_specializations;
        if (s.pre != null || s.post != null || s.in != null)
            throw new IllegalStateException("Trying to mix per-visit-type and generic fallbacks.");
        s.all = fallback;
        return this;
    }

    // ---------------------------------------------------------------------------------------------
}
