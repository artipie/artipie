/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.helm.metadata;

import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.asto.rx.RxStorage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.helm.ChartYaml;
import com.artipie.helm.TgzArchive;
import com.artipie.helm.misc.DateTimeNow;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.yaml.snakeyaml.Yaml;

/**
 * Index.yaml file. The main file in a chart repo.
 *
 * @since 0.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class IndexYaml {
    /**
     * The `index.yaml` string.
     */
    public static final Key INDEX_YAML = new Key.From("index.yaml");

    /**
     * The RxStorage.
     */
    private final RxStorage storage;

    /**
     * Ctor.
     * @param storage The storage.
     */
    public IndexYaml(final Storage storage) {
        this.storage = new RxStorageWrapper(storage);
    }

    /**
     * Update the index file.
     * @param arch New archive in a repo for which metadata is missing.
     * @return The operation result
     */
    public Completable update(final TgzArchive arch) {
        return this.indexFromStrg(
            Single.just(IndexYaml.empty())
        ).map(
            idx -> IndexYaml.update(idx, arch)
        ).flatMapCompletable(this::indexToStorage);
    }

    /**
     * Delete from `index.yaml` file specified chart.
     * If the file `index.yaml` is missing an exception is thrown.
     * @param name Chart name
     * @return The operation result.
     */
    public Completable deleteByName(final String name) {
        return this.indexFromStrg(IndexYaml.notFoundException())
            .map(
                idx -> {
                    new IndexYamlMapping(idx).entries().remove(name);
                    return idx;
                }
            ).flatMapCompletable(this::indexToStorage);
    }

    /**
     * Delete from `index.yaml` file specified chart with given version.
     * If the file `index.yaml` is missing an exception is thrown.
     * @param name Chart name
     * @param version Version of the chart which should be deleted
     * @return The operation result.
     */
    public Completable deleteByNameAndVersion(final String name, final String version) {
        return this.indexFromStrg(IndexYaml.notFoundException())
            .map(
                idx -> {
                    final IndexYamlMapping mapping = new IndexYamlMapping(idx);
                    final List<Map<String, Object>> newvers;
                    newvers = mapping.byChart(name).stream()
                        .filter(entry -> !entry.get("version").equals(version))
                        .collect(Collectors.toList());
                    mapping.entries().remove(name);
                    if (!newvers.isEmpty()) {
                        mapping.addChartVersions(name, newvers);
                    }
                    return idx;
                }
        ).flatMapCompletable(this::indexToStorage);
    }

    /**
     * Return an empty Index mappings.
     * @return The empty yaml mappings.
     */
    private static Map<String, Object> empty() {
        final Map<String, Object> res = new HashMap<>(3);
        res.put("apiVersion", "v1");
        res.put("entries", new HashMap<String, Object>(0));
        res.put("generated", new DateTimeNow().asString());
        return res;
    }

    /**
     * Generate exception.
     * @param <T> Ignore
     * @return Not found exception.
     */
    private static <T> Single<T> notFoundException() {
        return Single.error(
            new FileNotFoundException(
                String.format("File '%s' is not found", IndexYaml.INDEX_YAML)
            )
        );
    }

    /**
     * Perform an update.
     * @param index The index yaml mappings.
     * @param tgz The archive.
     * @return Updated map.
     */
    private static Map<String, Object> update(
        final Map<String, Object> index,
        final TgzArchive tgz
    ) {
        final Map<String, Object> copy = new HashMap<>(index);
        final IndexYamlMapping yaml = new IndexYamlMapping(copy);
        final ChartYaml chart = tgz.chartYaml();
        if (
            !yaml
                .byChartAndVersion(
                    chart.name(),
                    chart.version()
                )
                .isPresent()
        ) {
            yaml.addChartVersions(
                chart.name(),
                Collections.singletonList(tgz.metadata(Optional.empty()))
            );
        }
        return copy;
    }

    /**
     * Obtain index.yaml file from storage.
     * @param notexist Value if index.yaml does not exist in the storage.
     * @return Mapping for index.yaml if exists, otherwise value specified in the parameter.
     */
    private Single<Map<String, Object>> indexFromStrg(final Single<Map<String, Object>> notexist) {
        return this.storage.exists(IndexYaml.INDEX_YAML)
            .flatMap(
                exist -> {
                    final Single<Map<String, Object>>  result;
                    if (exist) {
                        result =
                            this.storage.value(IndexYaml.INDEX_YAML)
                                .flatMap(content -> new Concatenation(content).single())
                                .map(buf -> new String(new Remaining(buf).bytes()))
                                .map(content -> new Yaml().load(content));
                    } else {
                        result = notexist;
                    }
                    return result;
                }
            );
    }

    /**
     * Save index mapping to storage.
     * @param index Mapping for `index.yaml`
     * @return The operation result.
     */
    private Completable indexToStorage(final Map<String, Object> index) {
        return this.storage.save(
            IndexYaml.INDEX_YAML,
            new Content.From(
                new IndexYamlMapping(index).toString().getBytes(StandardCharsets.UTF_8)
            )
        );
    }
}
