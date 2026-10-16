package me.Padej_.soupapi.gui.option;

import java.util.ArrayList;
import java.util.List;

/** One entry in the settings sidebar: a title and the rows shown when it is selected. */
public final class OptionPage {

    private final String id;
    private final String title;
    private final List<Option> rows = new ArrayList<Option>();

    public OptionPage(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public List<Option> rows() {
        return rows;
    }

    public void add(Option option) {
        rows.add(option);
    }

    /** Rows whose label contains {@code query}, with their group headers kept for context. */
    public List<Option> search(String query) {
        if (query == null || query.trim().isEmpty()) return rows;
        String needle = query.toLowerCase(java.util.Locale.ROOT);
        List<Option> out = new ArrayList<Option>();
        Option pendingHeader = null;
        for (Option row : rows) {
            if (!row.interactive()) {
                pendingHeader = row;
                continue;
            }
            if (row.label().toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                if (pendingHeader != null) {
                    out.add(pendingHeader);
                    pendingHeader = null;
                }
                out.add(row);
            }
        }
        return out;
    }

    public int interactiveCount() {
        int n = 0;
        for (Option row : rows) {
            if (row.interactive()) n++;
        }
        return n;
    }
}
