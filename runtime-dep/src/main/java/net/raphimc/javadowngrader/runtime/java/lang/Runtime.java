/*
 * MIT License
 *
 * This file is part of JavaDowngrader - https://github.com/RaphiMC/JavaDowngrader
 * Copyright (c) 2023 RK_01/RaphiMC and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.raphimc.javadowngrader.runtime.java.lang;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Runtime {
    private static Version version;

    public static Version version() {
        Version v = version;
        if (v == null) {
            final String strVersion = System.getProperty("java.runtime.version");
            try {
                v = Version.parse(strVersion);
            } catch (Exception e) {
                if (!strVersion.startsWith("1.8")) {
                    throw e;
                }
                final int underscoreIndex = strVersion.indexOf('_');
                final String versionNumber = strVersion.substring(0, underscoreIndex);
                final int dashIndex = strVersion.indexOf('-', underscoreIndex + 1);
                final String build;
                final Optional<String> opt;
                if (dashIndex >= 0) {
                    build = strVersion.substring(underscoreIndex + 1, dashIndex);
                    opt = Optional.of(strVersion.substring(dashIndex + 1));
                } else {
                    build = strVersion.substring(underscoreIndex + 1);
                    opt = Optional.empty();
                }
                final String[] split = versionNumber.split("\\.");
                final Integer[] versionI = new Integer[split.length];
                for (int i = 0; i < split.length; i++) {
                    versionI[i] = Integer.parseInt(split[i]);
                }
                int n = versionI.length;
                while (n > 1 && versionI[n - 1] == 0) n--;
                v = new Version(
                    Collections.unmodifiableList(Arrays.asList(Arrays.copyOf(versionI, n))),
                    Optional.empty(), Optional.of(Integer.parseInt(build)), opt
                );
            }
            version = v;
        }
        return v;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static class Version implements Comparable<Version> {
        private final List<Integer> version; // I wish these could be OptionalInt, but the getters require otherwise
        private final Optional<String> pre;
        private final Optional<Integer> build;
        private final Optional<String> optional;

        private Version(List<Integer> unmodifiableListOfVersions, Optional<String> pre, Optional<Integer> build, Optional<String> optional) {
            this.version = unmodifiableListOfVersions;
            this.pre = pre;
            this.build = build;
            this.optional = optional;
        }

        public static Version parse(String s) {
            if (s == null) {
                throw new NullPointerException();
            }

            if (isSimpleNumber(s)) {
                return new Version(Collections.singletonList(Integer.parseInt(s)), Optional.empty(), Optional.empty(), Optional.empty());
            }
            final Matcher m = VersionPattern.VSTR_PATTERN.matcher(s);
            if (!m.matches()) {
                throw new IllegalArgumentException("Invalid version string: '" + s + "'");
            }

            final String[] split = m.group(VersionPattern.VNUM_GROUP).split("\\.");
            final Integer[] version = new Integer[split.length];
            for (int i = 0; i < split.length; i++) {
                version[i] = Integer.parseInt(split[i]);
            }

            final Optional<String> pre = Optional.ofNullable(m.group(VersionPattern.PRE_GROUP));

            final String b = m.group(VersionPattern.BUILD_GROUP);
            final Optional<Integer> build = b == null ? Optional.empty() : Optional.of(Integer.parseInt(b));

            final Optional<String> optional = Optional.ofNullable(m.group(VersionPattern.OPT_GROUP));

            if (!build.isPresent()) {
                if (m.group(VersionPattern.PLUS_GROUP) != null) {
                    if (optional.isPresent()) {
                        if (pre.isPresent()) {
                            throw new IllegalArgumentException("'+' found with pre-release and optional components:'" + s + "'");
                        }
                    } else {
                        throw new IllegalArgumentException("'+' found with neither build or optional components: '" + s + "'");
                    }
                } else {
                    if (optional.isPresent() && !pre.isPresent()) {
                        throw new IllegalArgumentException("optional component must be preceded by a pre-release component or '+': '" + s + "'");
                    }
                }
            }
            return new Version(Collections.unmodifiableList(Arrays.asList(version)), pre, build, optional);
        }

        private static boolean isSimpleNumber(String s) {
            for (int i = 0; i < s.length(); i++) {
                final char c = s.charAt(i);
                final char lowerBound = i > 0 ? '0' : '1';
                if (c < lowerBound || c > '9') {
                    return false;
                }
            }
            return true;
        }

        public int feature() {
            return version.get(0);
        }

        public int interim() {
            return version.size() > 1 ? version.get(1) : 0;
        }

        public int update() {
            return version.size() > 2 ? version.get(2) : 0;
        }

        public int patch() {
            return version.size() > 3 ? version.get(3) : 0;
        }

        public int major() {
            return feature();
        }

        public int minor() {
            return interim();
        }

        public int security() {
            return update();
        }

        public List<Integer> version() {
            return version;
        }

        public Optional<String> pre() {
            return pre;
        }

        public Optional<Integer> build() {
            return build;
        }

        public Optional<String> optional() {
            return optional;
        }

        @Override
        public int compareTo(@SuppressWarnings("NullableProblems") Version obj) {
            return compare(obj, false);
        }

        public int compareToIgnoreOptional(Version obj) {
            return compare(obj, true);
        }

        private int compare(Version obj, boolean ignoreOpt) {
            if (obj == null) {
                throw new NullPointerException();
            }

            int ret = compareVersion(obj);
            if (ret != 0) {
                return ret;
            }

            ret = comparePre(obj);
            if (ret != 0) {
                return ret;
            }

            ret = compareBuild(obj);
            if (ret != 0) {
                return ret;
            }

            return !ignoreOpt ? compareOptional(obj) : 0;
        }

        private int compareVersion(Version obj) {
            final int size = version.size();
            final int oSize = obj.version().size();
            final int min = Math.min(size, oSize);
            for (int i = 0; i < min; i++) {
                final int val = version.get(i);
                final int oVal = obj.version().get(i);
                if (val != oVal) {
                    return val - oVal;
                }
            }
            return size - oSize;
        }

        private int comparePre(Version obj) {
            final Optional<String> oPre = obj.pre();
            if (!pre.isPresent()) {
                if (oPre.isPresent()) {
                    return 1;
                }
            } else {
                if (!oPre.isPresent()) {
                    return -1;
                }
                final String val = pre.get();
                final String oVal = oPre.get();
                if (val.matches("\\d+")) {
                    return oVal.matches("\\d+") ? new BigInteger(val).compareTo(new BigInteger(oVal)) : -1;
                } else {
                    return oVal.matches("\\d+") ? 1 : val.compareTo(oVal);
                }
            }
            return 0;
        }

        private int compareBuild(Version obj) {
            final Optional<Integer> oBuild = obj.build();
            if (oBuild.isPresent()) {
                //noinspection OptionalIsPresent
                return build.isPresent() ? build.get().compareTo(oBuild.get()) : -1;
            } else if (build.isPresent()) {
                return 1;
            }
            return 0;
        }

        private int compareOptional(Version obj) {
            final Optional<String> oOpt = obj.optional();
            if (!optional.isPresent()) {
                if (oOpt.isPresent()) {
                    return -1;
                }
            } else {
                //noinspection OptionalIsPresent
                if (!oOpt.isPresent()) {
                    return 1;
                }
                return optional.get().compareTo(oOpt.get());
            }
            return 0;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(version.stream().map(Object::toString).collect(Collectors.joining(".")));

            //noinspection OptionalIsPresent
            if (pre.isPresent()) {
                sb.append("-").append(pre.get());
            }

            if (build.isPresent()) {
                sb.append("+").append(build.get());
                //noinspection OptionalIsPresent
                if (optional.isPresent()) {
                    sb.append("-").append(optional.get());
                }
            } else if (optional.isPresent()) {
                sb.append(pre.isPresent() ? "-" : "+-");
                sb.append(optional.get());
            }

            return sb.toString();
        }

        @Override
        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        public boolean equals(Object obj) {
            return equalsIgnoreOptional(obj) && optional().equals(((Version)obj).optional());
        }

        public boolean equalsIgnoreOptional(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Version)) {
                return false;
            }
            final Version o = (Version)obj;
            return version().equals(o.version) && pre().equals(o.pre()) && build().equals(o.build());
        }

        @Override
        public int hashCode() {
            int h = 1;
            h = 17 * h + version.hashCode();
            h = 17 * h + pre.hashCode();
            h = 17 * h + build.hashCode();
            h = 17 * h + optional.hashCode();
            return h;
        }
    }

    private static class VersionPattern {
        private static final String VNUM = "(?<VNUM>[1-9][0-9]*(?:(?:\\.0)*\\.[1-9][0-9]*)*)";
        private static final String PRE = "(?:-(?<PRE>[a-zA-Z0-9]+))?";
        private static final String BUILD = "(?:(?<PLUS>\\+)(?<BUILD>0|[1-9][0-9]*)?)?";
        private static final String OPT = "(?:-(?<OPT>[-a-zA-Z0-9.]+))?";
        private static final String VSTR_FORMAT = VNUM + PRE + BUILD + OPT;

        static final Pattern VSTR_PATTERN = Pattern.compile(VSTR_FORMAT);

        static final String VNUM_GROUP = "VNUM";
        static final String PRE_GROUP = "PRE";
        static final String PLUS_GROUP = "PLUS";
        static final String BUILD_GROUP = "BUILD";
        static final String OPT_GROUP = "OPT";
    }
}
