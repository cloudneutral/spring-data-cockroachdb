package org.springframework.data.cockroachdb.annotations;

import java.lang.reflect.Field;

/**
 * Enumeration of supported, deprecated and unofficial session variables as of CockroachDB v22.1.
 * Each variable is assumed both mutable and official (documented) unless otherwise marked. Deprecated
 * variables are still around for backwards compatibility.
 * <p>
 * See {@link <a href="https://www.cockroachlabs.com/docs/stable/set-vars.html#supported-variables"/>}
 */
public enum Variable {
    @Unofficial
    alter_primary_region_super_region_override(""),

    application_name("", "The current application name for statistics collection"),

    @Unofficial
    @Immutable
    avoid_buffering("off"),

    @Deprecated
    @Immutable
    backslash_quote("safe_encoding"),

    bytea_output("hex", "The mode for conversions from STRING to BYTES"),

    @Unofficial
    @Immutable
    check_function_bodies("on"),

    @Deprecated
    @Immutable
    client_encoding("UTF8"),

    client_min_messages("notice", "The severity level of notices displayed in the SQL shell. "
            + "Accepted values include debug5, debug4, debug3, debug2, debug1, log, notice, warning, and error"),

    @Unofficial
    copy_fast_path_enabled("on"),

    @Unofficial
    copy_from_atomic_enabled("on"),

    @Unofficial
    cost_scans_with_default_col_size("off"),

    @Immutable
    crdb_version("CockroachDB OSS <version>", "The version of CockroachDB"),

    database("","The current database"),

    datestyle("ISO, MDY","The input string format for DATE and TIMESTAMP values. Accepted values include ISO,MDY, ISO,DMY, and ISO,YMD"),

    default_int_size("8","The size, in bytes, of an INT type"),

    @Immutable
    default_table_access_method("heap"),

    @Deprecated
    default_tablespace(""),

    @Immutable
    default_transaction_isolation("serializable"),

    default_transaction_priority("normal"),

    @Since("v22.1")
    default_transaction_quality_of_service("regular"),

    default_transaction_read_only("off"),

    default_transaction_use_follower_reads("off"),

    @Unofficial
    default_with_oids("off"),

    @Unofficial
    disable_hoist_projection_in_join_limitation("off"),

    @Unofficial
    disable_partially_distributed_plans("off"),

    @Unofficial
    disable_plan_gists("off"),

    disallow_full_table_scans("off"),

    distsql("auto"),

    @Unofficial
    distsql_workmem("64 MiB"),

    @Unofficial
    enable_auto_rehoming("off"),

    @Unofficial
    enable_experimental_alter_column_type_general("off"),

    @Unofficial
    enable_experimental_stream_replication("off"),

    enable_implicit_select_for_update("on"),

    @Unofficial
    enable_implicit_transaction_for_batch_statements("on"),

    enable_insert_fast_path("on"),

    @Unofficial
    enable_multiple_modifications_of_table("off"),

    @Unofficial
    enable_multiregion_placement_policy("off"),

    @Unofficial
    enable_seqscan("on"),

    @Unofficial
    enable_super_regions("off"),

    enable_zigzag_join("on"),

    @Unofficial
    enforce_home_region("off"),

    @Unofficial
    escape_string_warning("on"),

    @Unofficial
    expect_and_ignore_not_visible_columns_in_copy("off"),

    @Unofficial
    experimental_distsql_planning("off"),

    @Unofficial
    experimental_enable_auto_rehoming("off"),

    @Unofficial
    experimental_enable_implicit_column_partitioning("off"),

    @Unofficial
    experimental_enable_temp_tables("off"),

    @Unofficial
    experimental_enable_unique_without_index_constraints("off"),

    extra_float_digits("0"),

    force_savepoint_restart("off"),

    foreign_key_cascades_limit("10000"),

    idle_in_session_timeout("0"),

    idle_in_transaction_session_timeout("0"),

    @Unofficial
    idle_session_timeout("0"),

    @Unofficial
    index_join_streamer_batch_size("8.0 MiB"),

    @Since("v22.1")
    index_recommendations_enabled("on"),

    @Since("v22.1")
    inject_retry_errors_enabled("off"),

    @Deprecated
    integer_datetimes("on"),

    intervalstyle("postgres"),

    @Immutable
    is_superuser("on"),

    @Unofficial
    join_reader_index_join_strategy_batch_size("4.0 MiB"),

    @Unofficial
    join_reader_no_ordering_strategy_batch_size("2.0 MiB"),

    @Unofficial
    join_reader_ordering_strategy_batch_size("100 KiB"),

    @Immutable
    large_full_scan_rows("1000"),

    @Unofficial
    lc_collate("C.UTF-8"),

    @Unofficial
    lc_ctype("C.UTF-8"),

    @Unofficial
    lc_messages("C.UTF-8"),

    @Unofficial
    lc_monetary("C.UTF-8"),

    @Unofficial
    lc_numeric("C.UTF-8"),

    @Unofficial
    lc_time("C.UTF-8"),

    @Immutable
    locality(""),

    @Unofficial
    locality_optimized_partitioned_index_scan("on"),

    lock_timeout("0"),

    @Deprecated
    max_identifier_length("128"),

    @Deprecated
    max_index_keys("32"),

    @Immutable
    node_id(""),

    @Since("v22.1")
    null_ordered_last("off"),

    @Unofficial
    on_update_rehome_row_enabled("on"),

    @Unofficial
    opt_split_scan_limit("2048"),

    @Unofficial
    optimizer("on"),

    @Unofficial
    optimizer_use_forecasts("on"),

    @Immutable
    optimizer_use_histograms("on"),

    @Immutable
    optimizer_use_multicol_stats("on"),

    @Unofficial
    optimizer_use_not_visible_indexes("off"),

    @Unofficial
    override_multi_region_zone_config("on"),

    @Unofficial
    parallelize_multi_key_lookup_joins_enabled("off"),

    @Unofficial
    password_encryption("scram-sha-256"),

    prefer_lookup_joins_for_fks("off"),

    @Unofficial
    propagate_input_ordering("off"),

    reorder_joins_limit("8"),

    @Unofficial
    require_explicit_primary_keys("off"),

    results_buffer_size("16384"),
    @Unofficial

    role("none"),

    @Deprecated
    row_security("off"),

    search_path("public"),

    serial_normalization("rowid"),

    @Deprecated
    server_encoding("UTF8"),

    @Immutable
    server_version("13.0.0"),

    server_version_num(""),

    @Immutable
    session_id(""),

    @Immutable
    session_user(""),

    @Unofficial
    show_primary_key_constraint_on_not_visible_columns("on"),

    sql_safe_updates("off"),

    @Deprecated
    standard_conforming_strings("on"),

    statement_timeout("0"),

    stub_catalog_tables("on"),

    @Deprecated
    synchronize_seqscans("on"),

    @Deprecated
    synchronous_commit("on"),

    @Unofficial
    testing_optimizer_cost_perturbation("0"),

    @Unofficial
    testing_optimizer_disable_rule_probability("0"),

    @Unofficial
    testing_optimizer_random_seed("0"),

    @Unofficial
    testing_vectorize_inject_panics("off"),

    timezone("UTC"),

    tracing("off"),

    @Immutable
    transaction_isolation("serializable"),

    transaction_priority("normal"),

    transaction_read_only("off"),

    transaction_rows_read_err("0"),

    transaction_rows_read_log("0"),

    transaction_rows_written_err("0"),

    transaction_rows_written_log("0"),

    @Immutable
    transaction_status("NoTxn"),

    troubleshooting_mode("off"),

    @Deprecated
    unconstrained_non_covering_index_scan_enabled("off"),

    use_declarative_schema_changer("on"),

    @Unofficial
    variable_inequality_lookup_join_enabled("on"),

    vectorize("on"),

    @Unofficial
    xmloption("content");

    private final String description;

    private final String defaultValue;

    private final boolean mutable;

    private final boolean official;

    private final String version;

    Variable(String defaultValue) {
        this(defaultValue, null);
    }

    Variable(String defaultValue, String description) {
        this.defaultValue = defaultValue;
        this.description = description;
        try {
            Field f = getClass().getField(name());
            Since since = f.getAnnotation(Since.class);
            this.version = since != null ? since.value() : "(unknown)";

            Unofficial unofficial = f.getAnnotation(Unofficial.class);
            this.official = unofficial == null;

            this.mutable = f.getAnnotation(Immutable.class) == null;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getDefaultValue() {
        return "".equals(defaultValue) ? "(undefined)" : defaultValue;
    }

    public String getDescription() {
        return "".equals(description) ? "(n/a)" : description;
    }

    public boolean isMutable() {
        return mutable;
    }

    public boolean isOfficial() {
        return official;
    }

    public String getVersion() {
        return version;
    }
}
