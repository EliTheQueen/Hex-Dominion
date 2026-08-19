package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Serialization-only shell for saves written before the SingleCommandSlot
 * became the sole production mechanism. No gameplay code may use this type.
 */
@Deprecated
final class ProductionQueue implements java.io.Serializable {
    private static final long serialVersionUID = 8109985418134989609L;
    @SuppressWarnings("unused")
    private final List<ProductionTask> tasks = new ArrayList<>();
}
