package com.quadient.wfdxml.internal;

import java.util.List;

/**
 * Marks a layout node that owns a private list of child nodes (e.g. condition variables)
 * which are logically scoped to that node rather than to the global Data root.
 * <p>
 * Implementors supply the list via {@link #getLocalNodes()}; the
 * {@code ForwardReferencesExporter} discovers and registers these nodes with a
 * {@code ParentId} pointing to the owning node, matching the structure that
 * Inspire Designer produces interactively.
 */
public interface HasLocalNodes {
    List<NodeImpl> getLocalNodes();
}
