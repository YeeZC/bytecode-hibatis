package me.zyee.hibatis.dao.scaner;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/16
 */
public class NamePredicate implements Predicate<String> {
    private static final Character ASTERISK = '*';
    private static final Character QUESTION_MARK = '?';
    private static final Character ESCAPE = '\\';

    public final String pattern;

    private NamePredicate(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean test(String s) {
        return match(s, 0, 0);
    }

    private boolean match(String target, int stringStartNdx, int patternStartNdx) {
        //#135
        if (target == null || pattern == null) {
            return false;
        }
        int pNdx = patternStartNdx;
        int sNdx = stringStartNdx;
        int pLen = pattern.length();
        if (pLen == 1) {
            // speed-up
            if (pattern.charAt(0) == ASTERISK) {
                return true;
            }
        }
        int sLen = target.length();
        boolean nextIsNotWildcard = false;

        while (true) {

            // check if end of string and/or pattern occurred
            if ((sNdx >= sLen)) {
                // end of string still may have pending '*' callback pattern
                while ((pNdx < pLen) && (pattern.charAt(pNdx) == ASTERISK)) {
                    pNdx++;
                }
                return pNdx >= pLen;
            }
            // end of pattern, but not end of the string
            if (pNdx >= pLen) {
                return false;
            }
            // pattern char
            char p = pattern.charAt(pNdx);

            // perform logic
            if (!nextIsNotWildcard) {

                if (p == ESCAPE) {
                    pNdx++;
                    nextIsNotWildcard = true;
                    continue;
                }
                if (p == QUESTION_MARK) {
                    sNdx++;
                    pNdx++;
                    continue;
                }
                if (p == ASTERISK) {
                    // next pattern char
                    char pnext = 0;
                    if (pNdx + 1 < pLen) {
                        pnext = pattern.charAt(pNdx + 1);
                    }
                    // double '*' have the same effect as one '*'
                    if (pnext == ASTERISK) {
                        pNdx++;
                        continue;
                    }
                    int i;
                    pNdx++;

                    // find recursively if there is any substring from the end of the
                    // line that matches the rest of the pattern !!!
                    for (i = target.length(); i >= sNdx; i--) {
                        if (match(target, i, pNdx)) {
                            return true;
                        }
                    }
                    return false;
                }
            } else {
                nextIsNotWildcard = false;
            }

            // check if pattern char and string char are equals
            if (p != target.charAt(sNdx)) {
                return false;
            }

            // everything matches for now, continue
            sNdx++;
            pNdx++;
        }
    }

    public static Predicate<String> of(String pattern) {
        return Optional.ofNullable(pattern)
                .<Predicate<String>>map(p -> {
                    if (StringUtils.isEmpty(p)) {
                        return s -> true;
                    }
                    return new NamePredicate(p);
                })
                .orElse(s -> true);
    }
}
