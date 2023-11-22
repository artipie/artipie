/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package  com.artipie.conan;

import com.google.common.base.Strings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ini file basic support.
 * @since 0.1
 */
public final class IniFile {

    /**
     * Ini section pattern.
     */
    private static final Pattern SECTION = Pattern.compile("^\\s*\\[([^]]*)]\\s*$");

    /**
     * Ini key=value pattern.
     */
    private static final Pattern KEYVAL = Pattern.compile("^\\s*([^=]*)?=?(.*)$");

    /**
     * Parsed .ini file entries.
     */
    private final Map<String, Map<String, String>> entries;

    /**
     * Initializes object instance with contents of the .ini file in the data array.
     * @param data Data of the .ini file.
     */
    public IniFile(final byte[] data) {
        this(IniFile.loadEntries(new String(data)));
    }

    /**
     * Initializes object instance with Map of .ini entries.
     * @param entries Map of .ini file entries.
     */
    public IniFile(final Map<String, Map<String, String>> entries) {
        this.entries = entries;
    }

    /**
     * Loads IniFile from contents of the .ini file at the given path.
     * @param path Path to the .ini file.
     * @return IniFile object with contents of the file.
     * @throws IOException In case some I/O error occurs.
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    public static IniFile loadIniFile(final Path path) throws IOException {
        return new IniFile(IniFile.loadEntries(new String(Files.readAllBytes(path))));
    }

    /**
     * Save all entries to the file at the given path.
     * @param path Path to the file as String.
     * @throws IOException In case some I/O related error occurs.
     */
    public void save(final String path) throws IOException {
        final byte[] data = IniFile.saveEntries(this.entries);
        Files.write(Paths.get(path), data);
    }

    /**
     * Saves all entries to the byte array.
     * @return Bytes array with .ini data.
     * @throws IOException In case some I/O related error occurs.
     */
    public byte[] save() throws IOException {
        return IniFile.saveEntries(this.entries);
    }

    @Override
    public boolean equals(final Object other) {
        final boolean result;
        if (this == other) {
            result = true;
        } else if (other == null || getClass() != other.getClass()) {
            result = false;
        } else {
            final IniFile ini = (IniFile) other;
            result = this.entries.equals(ini.entries);
        }
        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.entries);
    }

    /**
     * Returns .ini file entries.
     * @return Map of .ini file entries, section->key->value.
     */
    public Map<String, Map<String, String>> getEntries() {
        return Collections.unmodifiableMap(this.entries);
    }

    /**
     * Sets .ini file value as String.
     * @param section Ini file section name.
     * @param key Ini file key name.
     * @param value Ini file default return value, if no value found.
     */
    public void setString(final String section, final String key, final String value) {
        final Map<String, String> values = this.entries
            .computeIfAbsent(section, k -> new HashMap<>());
        values.put(key, value);
    }

    /**
     * Returns .ini file value as String.
     * @param section Ini file section name.
     * @param key Ini file key name.
     * @param defaultvalue Ini file default return value, if no value found.
     * @return Corresponding value from Ini file, of default value, if not found.
     */
    public String getString(final String section, final String key, final String defaultvalue) {
        final Map<String, String> values = this.entries.get(section);
        final String result;
        if (values == null) {
            result = defaultvalue;
        } else {
            result = values.get(key);
        }
        return result;
    }

    /**
     * Returns .ini file value as T, using internally new T(String) ctor. This is a convenience
     * method, to facilitate the readability of the end-user code.
     * @param section Ini file section name.
     * @param key Ini file key name.
     * @param defaultvalue Ini file default return value, if no value found.
     * @param <T> Get this type.
     * @return Corresponding value from Ini file as type T, of default value, if not found.
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(final String section, final String key, final T defaultvalue) {
        return this.getValue(
            section, key, defaultvalue, str -> {
                T result;
                try {
                    result = (T) defaultvalue.getClass().getDeclaredConstructor(String.class)
                        .newInstance(str);
                } catch (final ReflectiveOperationException | ClassCastException ex) {
                    result = defaultvalue;
                }
                return result;
            });
    }

    /**
     * Returns .ini file value as T, using provided factory for T objects.
     * @param section Ini file section name.
     * @param key Ini file key name.
     * @param defaultvalue Ini file default return value, if no value found.
     * @param factory Factory that would provide new instances of T, initialized with String value.
     * @param <T> Get this type.
     * @return Corresponding value from Ini file as type T, of default value, if not found.
     * @checkstyle ParameterNumberCheck (30 lines)
     */
    public <T> T getValue(final String section, final String key, final T defaultvalue,
        final Function<String, T> factory) {
        final Map<String, String> values = this.entries.get(section);
        final T result;
        if (values == null) {
            result = defaultvalue;
        } else {
            final String value = values.get(key);
            if (value == null) {
                result = defaultvalue;
            } else {
                result = factory.apply(value);
            }
        }
        return result;
    }

    /**
     * Loads ini file.
     * @param data Contents of the ini file.
     * @return Loaded map with ini file data: section->(key->value).
     * @throws IOException In case some I/O error occurs.
     */
    @SuppressWarnings("PMD.AssignmentInOperand")
    private static Map<String, Map<String, String>> loadEntries(final String data) {
        try (BufferedReader reader = new BufferedReader(new StringReader(data))) {
            final Map<String, Map<String, String>> entries = new HashMap<>();
            String line;
            String section = null;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = IniFile.SECTION.matcher(line);
                if (matcher.matches()) {
                    section = matcher.group(1).trim();
                    entries.computeIfAbsent(section, key -> new HashMap<>());
                } else if (section != null) {
                    matcher = IniFile.KEYVAL.matcher(line);
                    extractKeyval(matcher, section, entries);
                }
            }
            return entries;
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Extracts key=value pair from specified section to entries.
     * @param matcher Matcher for key=value string.
     * @param section Ini file section name.
     * @param entries Parsed .ini file entities map.
     */
    private static void extractKeyval(final Matcher matcher, final String section,
        final Map<String, Map<String, String>> entries) {
        if (matcher.matches()) {
            final String key = matcher.group(1);
            final String value = matcher.group(2);
            if (key.isEmpty() && value.isEmpty()) {
                return;
            }
            final Map<String, String> values = entries.get(section);
            values.put(key.trim(), value.trim());
        }
    }

    /**
     * Saves provided .ini entries to the byte array.
     * @param entries Map of .ini file entries.
     * @return Bytes array with .ini data.
     * @throws IOException In case some I/O related error occurs.
     */
    private static byte[] saveEntries(final Map<String, Map<String, String>> entries)
        throws IOException {
        final int inisize = 1024;
        try (StringWriter writer = new StringWriter(inisize)) {
            for (final String section : entries.keySet()) {
                writer.write(String.join("", "[", section, "]\n"));
                final Map<String, String> values = entries.get(section);
                for (final String key : values.keySet()) {
                    final String value = values.get(key);
                    final String head = String.join("", "  ", key);
                    final String tail;
                    if (Strings.isNullOrEmpty(value)) {
                        tail = "";
                    } else {
                        tail = String.join("", "=", value);
                    }
                    writer.write(String.join("", head, tail, "\n"));
                }
            }
            return writer.toString().getBytes();
        }
    }
}
