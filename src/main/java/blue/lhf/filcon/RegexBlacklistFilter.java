package blue.lhf.filcon;

import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.*;

import java.util.*;
import java.util.regex.*;

/**
 * A filter that blocks log messages that match any of the given regex patterns.
 * */
public final class RegexBlacklistFilter extends AbstractFilter {

    private final List<Pattern> patterns;

    public RegexBlacklistFilter(final Pattern... patterns) {
        super(Result.DENY, Result.NEUTRAL);
        this.patterns = new ArrayList<>(List.of(patterns));
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object... params) {
        if (params == null || params.length == 0) return filter(msg);
        return filter(ParameterizedMessage.format(msg, params));
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Object msg, final Throwable t) {
        if (msg == null) return onMismatch;
        return filter(msg.toString());
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Message msg, final Throwable t) {
        if (msg == null) return onMismatch;
        return filter(msg.getFormattedMessage());
    }

    @Override
    public Result filter(final LogEvent event) {
        final String text = event.getMessage().getFormattedMessage();
        return filter(text);
    }

    private Result filter(final String msg) {
        if (msg == null || patterns.isEmpty()) return onMismatch;

        for (final Pattern pattern : patterns) {
            final Matcher m = pattern.matcher(msg);
            if (m.find()) return onMatch;
        }

        return onMismatch;
    }

    /**
     * @return A modifiable view of the patterns used by this filter.
     * */
    public List<Pattern> getPatterns() {
        return this.patterns;
    }
}
