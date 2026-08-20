package kg.tunduk.cvscan.candidate.controller;

import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

/**
 * Parses the contract's single {@code sort=field,direction} query parameter
 * (e.g. {@code name,asc}) into a Spring {@link Sort}, whitelisting sortable
 * fields to prevent arbitrary property injection.
 */
final class SortParser {

    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "name", "totalExp", "createdAt", "status", "verdict", "position"
    );
    private static final String DEFAULT_FIELD = "createdAt";
    private static final Sort.Direction DEFAULT_DIRECTION = Sort.Direction.DESC;

    private SortParser() {
    }

    static Sort parse(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(DEFAULT_DIRECTION, DEFAULT_FIELD);
        }
        String[] parts = sort.split(",", 2);
        String field = parts[0].trim();
        if (!ALLOWED_FIELDS.contains(field)) {
            field = DEFAULT_FIELD;
        }
        Sort.Direction direction = DEFAULT_DIRECTION;
        if (parts.length > 1) {
            try {
                direction = Sort.Direction.fromString(parts[1].trim());
            } catch (IllegalArgumentException ignored) {
                direction = DEFAULT_DIRECTION;
            }
        }
        return Sort.by(direction, field);
    }
}
