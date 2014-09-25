package org.checkerframework.qualframework.poly;

import java.util.*;

import org.checkerframework.javacutil.Pair;

import org.checkerframework.qualframework.base.QualifierHierarchy;

/** This class provides a <code>QualifierHierarchy</code> implementation for
 * sets of qualifier parameters.  Under this hierarchy, A is a subtype of B iff
 * the value of each qualifier parameter in A is contained within the value of
 * the corresponding parameter in B.
 */
public class QualifierParameterHierarchy<Q> implements QualifierHierarchy<QualParams<Q>> {
    private QualifierHierarchy<Wildcard<Q>> containmentHierarchy;
    private List<Pair<Wildcard<Q>, Wildcard<Q>>> constraintTarget = null;

    public final QualParams<Q> PARAMS_BOTTOM = QualParams.<Q>getBottom();
    public final QualParams<Q> PARAMS_TOP = QualParams.<Q>getTop();


    public QualifierParameterHierarchy(QualifierHierarchy<Wildcard<Q>> containmentHierarchy) {
        this.containmentHierarchy = containmentHierarchy;
    }

    // We can't use constructor overloads for the following variants because
    // they all have the same erasure.

    /** Construct an instance from a {@link ContainmentHierarchy} or
     * equivalent.
     */
    public static <Q> QualifierParameterHierarchy<Q> fromContainment(QualifierHierarchy<Wildcard<Q>> containmentHierarchy) {
        return new QualifierParameterHierarchy<>(containmentHierarchy);
    }

    /** Construct an instance from a {@link PolyQualHierarchy} or equivalent,
     * using the default {@link ContainmentHierarchy} implementation.
     */
    public static <Q> QualifierParameterHierarchy<Q> fromPolyQual(QualifierHierarchy<PolyQual<Q>> polyQualHierarchy) {
        return fromContainment(new ContainmentHierarchy<Q>(polyQualHierarchy));
    }

    /** Construct an instance from a {@code QualifierHierarchy<Q>}, using the
     * default {@link PolyQualHierarchy} and {@link ContainmentHierarchy}
     * implementations.
     */
    public static <Q> QualifierParameterHierarchy<Q> fromGround(QualifierHierarchy<Q> groundHierarchy) {
        return fromPolyQual(new PolyQualHierarchy<Q>(groundHierarchy));
    }


    /** Get the containment hierarchy that is used to compare wildcards. */
    protected QualifierHierarchy<Wildcard<Q>> getContaintmentHierarchy() {
        return containmentHierarchy;
    }

    /** Set a target for constraint generation.  When the current constraint
     * target is not {@code null}, all subtyping checks return {@code true} and
     * populate the target with a (subtype, supertype) pair for each
     * containment check that would normally be made.
     */
    public void setConstraintTarget(List<Pair<Wildcard<Q>, Wildcard<Q>>> constraintTarget) {
        this.constraintTarget = constraintTarget;
    }


    @Override
    public boolean isSubtype(QualParams<Q> subtype, QualParams<Q> supertype) {
        if (subtype.equals(supertype))
            return true;

        if (subtype == PARAMS_BOTTOM || supertype == PARAMS_TOP)
            return true;

        if (subtype == PARAMS_TOP || supertype == PARAMS_BOTTOM ||
                !subtype.keySet().equals(supertype.keySet())) {
            if (constraintTarget == null) {
                return false;
            } else {
                constraintTarget.add(null);
                return true;
            }
        }

        for (String k : subtype.keySet()) {
            if (constraintTarget == null) {
                if (!containmentHierarchy.isSubtype(subtype.get(k), supertype.get(k)))
                    return false;
            } else {
                constraintTarget.add(Pair.of(subtype.get(k), supertype.get(k)));
            }
        }

        return true;
    }

    @Override
    public QualParams<Q> leastUpperBound(QualParams<Q> a, QualParams<Q> b) {
        if (this.constraintTarget != null) {
            throw new UnsupportedOperationException("unexpected leastUpperBound when generating constraints");
        }

        if (a == PARAMS_BOTTOM)
            return b;

        if (b == PARAMS_BOTTOM)
            return a;

        if (a == PARAMS_TOP || b == PARAMS_TOP)
            return PARAMS_TOP;

        Map<String, Wildcard<Q>> result = new HashMap<>();

        for (String k : a.keySet()) {
            if (b.containsKey(k)) {
                result.put(k, containmentHierarchy.leastUpperBound(a.get(k), b.get(k)));
            } else {
                result.put(k, a.get(k));
            }
        }

        for (String k : b.keySet()) {
            if (!a.containsKey(k)) {
                result.put(k, b.get(k));
            }
        }

        return new QualParams<Q>(result);
    }

    @Override
    public QualParams<Q> greatestLowerBound(QualParams<Q> a, QualParams<Q> b) {
        if (this.constraintTarget != null) {
            throw new UnsupportedOperationException("unexpected leastUpperBound when generating constraints");
        }

        if (a == PARAMS_TOP)
            return b;

        if (b == PARAMS_TOP)
            return a;

        if (a == PARAMS_BOTTOM || b == PARAMS_BOTTOM)
            return PARAMS_BOTTOM;

        Map<String, Wildcard<Q>> result = new HashMap<>();

        for (String k : a.keySet()) {
            if (b.containsKey(k)) {
                result.put(k, containmentHierarchy.greatestLowerBound(a.get(k), b.get(k)));
            } else {
                result.put(k, a.get(k));
            }
        }

        for (String k : b.keySet()) {
            if (!a.containsKey(k)) {
                result.put(k, b.get(k));
            }
        }

        return new QualParams<Q>(result);
    }

    @Override
    public QualParams<Q> getBottom() {
        return PARAMS_BOTTOM;
    }

    @Override
    public QualParams<Q> getTop() {
        return PARAMS_TOP;
    }
}

