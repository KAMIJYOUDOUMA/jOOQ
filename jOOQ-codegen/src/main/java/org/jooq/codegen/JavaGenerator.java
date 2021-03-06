/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Other licenses:
 * -----------------------------------------------------------------------------
 * Commercial licenses for this work are available. These replace the above
 * ASL 2.0 and offer limited warranties, support, maintenance, and commercial
 * database integrations.
 *
 * For more information, please visit: http://www.jooq.org/licenses
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package org.jooq.codegen;


import static java.util.Arrays.asList;
// ...
// ...
import static org.jooq.SQLDialect.MYSQL;
import static org.jooq.SQLDialect.POSTGRES;
import static org.jooq.SortOrder.DESC;
import static org.jooq.codegen.AbstractGenerator.Language.JAVA;
import static org.jooq.codegen.AbstractGenerator.Language.KOTLIN;
import static org.jooq.codegen.AbstractGenerator.Language.SCALA;
import static org.jooq.codegen.GenerationUtil.convertToIdentifier;
import static org.jooq.impl.DSL.name;
import static org.jooq.meta.AbstractTypedElementDefinition.getDataType;
import static org.jooq.tools.StringUtils.isBlank;

import java.beans.ConstructorProperties;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.jooq.AggregateFunction;
import org.jooq.Catalog;
import org.jooq.Check;
import org.jooq.Configuration;
import org.jooq.Constants;
import org.jooq.DataType;
import org.jooq.Domain;
import org.jooq.EnumType;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
// ...
import org.jooq.Name;
import org.jooq.OrderField;
import org.jooq.Parameter;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Row;
import org.jooq.Schema;
import org.jooq.Sequence;
import org.jooq.SortOrder;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UDT;
import org.jooq.UDTField;
import org.jooq.UniqueKey;
import org.jooq.codegen.GeneratorStrategy.Mode;
import org.jooq.exception.SQLDialectNotSupportedException;
import org.jooq.impl.AbstractRoutine;
// ...
import org.jooq.impl.CatalogImpl;
import org.jooq.impl.DAOImpl;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultDataType;
import org.jooq.impl.EmbeddableRecordImpl;
import org.jooq.impl.Internal;
import org.jooq.impl.PackageImpl;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.SchemaImpl;
import org.jooq.impl.TableImpl;
import org.jooq.impl.TableRecordImpl;
import org.jooq.impl.UDTImpl;
import org.jooq.impl.UDTRecordImpl;
import org.jooq.impl.UpdatableRecordImpl;
import org.jooq.meta.AbstractTypedElementDefinition;
import org.jooq.meta.ArrayDefinition;
import org.jooq.meta.AttributeDefinition;
import org.jooq.meta.CatalogDefinition;
import org.jooq.meta.CheckConstraintDefinition;
import org.jooq.meta.ColumnDefinition;
import org.jooq.meta.DataTypeDefinition;
import org.jooq.meta.Database;
import org.jooq.meta.DefaultDataTypeDefinition;
import org.jooq.meta.Definition;
import org.jooq.meta.DomainDefinition;
import org.jooq.meta.EmbeddableColumnDefinition;
import org.jooq.meta.EmbeddableDefinition;
import org.jooq.meta.EnumDefinition;
import org.jooq.meta.ForeignKeyDefinition;
import org.jooq.meta.IdentityDefinition;
import org.jooq.meta.IndexColumnDefinition;
import org.jooq.meta.IndexDefinition;
import org.jooq.meta.JavaTypeResolver;
import org.jooq.meta.PackageDefinition;
import org.jooq.meta.ParameterDefinition;
import org.jooq.meta.RoutineDefinition;
import org.jooq.meta.SchemaDefinition;
import org.jooq.meta.SequenceDefinition;
import org.jooq.meta.TableDefinition;
import org.jooq.meta.TypedElementDefinition;
import org.jooq.meta.UDTDefinition;
import org.jooq.meta.UniqueKeyDefinition;
import org.jooq.meta.jaxb.GeneratedAnnotationType;
// ...
// ...
// ...
// ...
import org.jooq.meta.postgres.PostgresDatabase;
import org.jooq.tools.JooqLogger;
import org.jooq.tools.StopWatch;
import org.jooq.tools.StringUtils;
import org.jooq.tools.reflect.Reflect;
import org.jooq.tools.reflect.ReflectException;
// ...


/**
 * A default implementation for code generation.
 * <p>
 * Replace this code with your own logic, if you need your database schema
 * represented in a different way.
 * <p>
 * Note that you can also extend this class to generate POJO's or other stuff
 * entirely independent of jOOQ.
 *
 * @author Lukas Eder
 */
public class JavaGenerator extends AbstractGenerator {

    private static final JooqLogger               log                          = JooqLogger.getLogger(JavaGenerator.class);

    /**
     * The Javadoc to be used for private constructors
     */
    private static final String                   NO_FURTHER_INSTANCES_ALLOWED = "No further instances allowed";

    /**
     * [#1459] Prevent large static initialisers by splitting nested classes
     */
    private static final int                      INITIALISER_SIZE             = 500;

    /**
     * [#4429] A map providing access to SQLDataType member literals
     */
    private static final Map<DataType<?>, String> SQLDATATYPE_LITERAL_LOOKUP;

    /**
     * [#6411] A set providing access to SQLDataTypes that can have length.
     */
    private static final Set<String>              SQLDATATYPE_WITH_LENGTH;

    /**
     * [#6411] A set providing access to SQLDataTypes that can have precision
     * (and scale).
     */
    private static final Set<String>              SQLDATATYPE_WITH_PRECISION;

    /**
     * Some reusable type references.
     */
    private static final String                   KLIST                        = "kotlin.collections.List";
    private static final String                   KMUTABLELIST                 = "kotlin.collections.MutableList";

    private static final Set<String>              PRIMITIVE_WRAPPERS           = new HashSet<>(Arrays.asList(
        Short.class.getName(),
        Integer.class.getName(),
        Long.class.getName(),
        Float.class.getName(),
        Double.class.getName(),
        Boolean.class.getName(),
        Character.class.getName(),
        "kotlin.Byte",
        "kotlin.Short",
        "kotlin.Int",
        "kotlin.Long",
        "kotlin.Float",
        "kotlin.Double",
        "kotlin.Boolean",
        "kotlin.Char"
    ));

    /**
     * An overall stop watch to measure the speed of source code generation
     */
    private final StopWatch                       watch                        = new StopWatch();

    /**
     * The underlying database of this generator
     */
    private Database                              database;

    /**
     * The code generation date, if needed.
     */
    private String                                isoDate;

    /**
     * The cached schema version numbers.
     */
    private Map<SchemaDefinition, String>         schemaVersions;

    /**
     * The cached catalog version numbers.
     */
    private Map<CatalogDefinition, String>        catalogVersions;

    /**
     * All files modified by this generator.
     */
    private Set<File>                             files                        = new LinkedHashSet<>();

    /**
     * These directories were not modified by this generator, but flagged as not
     * for removal (e.g. because of {@link #schemaVersions} or
     * {@link #catalogVersions}).
     */
    private Set<File>                             directoriesNotForRemoval     = new LinkedHashSet<>();

    private final boolean                         java;
    private final boolean                         scala;
    private final boolean                         kotlin;
    private final String                          tokenVoid;
    private final Files                           fileCache;

    static {
        SQLDATATYPE_LITERAL_LOOKUP = new IdentityHashMap<>();
        SQLDATATYPE_WITH_LENGTH = new HashSet<>();
        SQLDATATYPE_WITH_PRECISION = new HashSet<>();

        try {
            for (java.lang.reflect.Field f : SQLDataType.class.getFields()) {
                if (Modifier.isPublic(f.getModifiers()) &&
                    Modifier.isStatic(f.getModifiers()) &&
                    Modifier.isFinal(f.getModifiers()))
                    SQLDATATYPE_LITERAL_LOOKUP.put((DataType<?>) f.get(SQLDataType.class), f.getName());
            }

            for (java.lang.reflect.Method m : SQLDataType.class.getMethods()) {
                if (Modifier.isPublic(m.getModifiers()) &&
                    Modifier.isStatic(m.getModifiers()) &&
                    ((DataType<?>) SQLDataType.class.getField(m.getName()).get(SQLDataType.class)).hasPrecision())
                    SQLDATATYPE_WITH_PRECISION.add(m.getName());
            }

            for (java.lang.reflect.Method m : SQLDataType.class.getMethods()) {
                if (Modifier.isPublic(m.getModifiers()) &&
                    Modifier.isStatic(m.getModifiers()) &&
                    ((DataType<?>) SQLDataType.class.getField(m.getName()).get(SQLDataType.class)).hasLength() &&
                    !SQLDATATYPE_WITH_PRECISION.contains(m.getName()))
                    SQLDATATYPE_WITH_LENGTH.add(m.getName());
            }
        }
        catch (Exception e) {
            log.warn(e);
        }
    }

    public JavaGenerator() {
        this(JAVA);
    }

    JavaGenerator(Language language) {
        super(language);

        this.scala = (language == SCALA);
        this.kotlin = (language == KOTLIN);
        this.java = (language == JAVA);
        this.tokenVoid = (scala || kotlin ? "Unit" : "void");
        this.fileCache = new Files();
    }

    @Override
    public final void generate(Database db) {
        this.isoDate = DatatypeConverter.printDateTime(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
        this.schemaVersions = new LinkedHashMap<>();
        this.catalogVersions = new LinkedHashMap<>();

        this.database = db;
        this.database.addFilter(new AvoidAmbiguousClassesFilter());
        this.database.setIncludeRelations(generateRelations());
        this.database.setTableValuedFunctions(generateTableValuedFunctions());

        logDatabaseParameters(db);
        log.info("");
        log.info("JavaGenerator parameters");
        log.info("----------------------------------------------------------");
        log.info("  annotations (generated)", generateGeneratedAnnotation()
            + ((!generateGeneratedAnnotation && (useSchemaVersionProvider || useCatalogVersionProvider)) ?
                " (forced to true because of <schemaVersionProvider/> or <catalogVersionProvider/>)" : ""));
        log.info("  annotations (JPA: any)", generateJPAAnnotations());
        log.info("  annotations (JPA: version)", generateJPAVersion());
        log.info("  annotations (validation)", generateValidationAnnotations());
        log.info("  comments", generateComments());
        log.info("  comments on attributes", generateCommentsOnAttributes());
        log.info("  comments on catalogs", generateCommentsOnCatalogs());
        log.info("  comments on columns", generateCommentsOnColumns());
        log.info("  comments on keys", generateCommentsOnKeys());
        log.info("  comments on links", generateCommentsOnLinks());
        log.info("  comments on packages", generateCommentsOnPackages());
        log.info("  comments on parameters", generateCommentsOnParameters());
        log.info("  comments on queues", generateCommentsOnQueues());
        log.info("  comments on routines", generateCommentsOnRoutines());
        log.info("  comments on schemas", generateCommentsOnSchemas());
        log.info("  comments on sequences", generateCommentsOnSequences());
        log.info("  comments on tables", generateCommentsOnTables());
        log.info("  comments on udts", generateCommentsOnUDTs());
        log.info("  sources", generateSources());
        log.info("  sources on views", generateSourcesOnViews());
        log.info("  daos", generateDaos());
        log.info("  deprecated code", generateDeprecated());
        log.info("  global references (any)", generateGlobalObjectReferences());
        log.info("  global references (catalogs)", generateGlobalCatalogReferences());
        log.info("  global references (keys)", generateGlobalKeyReferences());
        log.info("  global references (links)", generateGlobalLinkReferences());
        log.info("  global references (queues)", generateGlobalQueueReferences());
        log.info("  global references (routines)", generateGlobalRoutineReferences());
        log.info("  global references (schemas)", generateGlobalSchemaReferences());
        log.info("  global references (sequences)", generateGlobalSequenceReferences());
        log.info("  global references (tables)", generateGlobalTableReferences());
        log.info("  global references (udts)", generateGlobalUDTReferences());
        log.info("  indexes", generateIndexes());
        log.info("  instance fields", generateInstanceFields());
        log.info("  interfaces", generateInterfaces()
              + ((!generateInterfaces && generateImmutableInterfaces) ? " (forced to true because of <immutableInterfaces/>)" : ""));
        log.info("  interfaces (immutable)", generateInterfaces());
        log.info("  javadoc", generateJavadoc());
        log.info("  keys", generateKeys());
        log.info("  links", generateLinks());
        log.info("  pojos", generatePojos()
              + ((!generatePojos && generateDaos) ? " (forced to true because of <daos/>)" :
                ((!generatePojos && generateImmutablePojos) ? " (forced to true because of <immutablePojos/>)" : "")));
        log.info("  pojos (immutable)", generateImmutablePojos());
        log.info("  queues", generateQueues());
        log.info("  records", generateRecords()
              + ((!generateRecords && generateDaos) ? " (forced to true because of <daos/>)" : ""));
        log.info("  routines", generateRoutines());
        log.info("  sequences", generateSequences());
        log.info("  sequenceFlags", generateSequenceFlags());
        log.info("  table-valued functions", generateTableValuedFunctions());
        log.info("  tables", generateTables()
              + ((!generateTables && generateRecords) ? " (forced to true because of <records/>)" :
                ((!generateTables && generateDaos) ? " (forced to true because of <daos/>)" : "")));
        log.info("  udts", generateUDTs());
        log.info("  relations", generateRelations()
            + ((!generateRelations && generateTables) ? " (forced to true because of <tables/>)" :
              ((!generateRelations && generateDaos) ? " (forced to true because of <daos/>)" : "")));
        log.info("----------------------------------------------------------");

        if (!generateInstanceFields()) {
            log.warn("");
            log.warn("Deprecation warnings");
            log.warn("----------------------------------------------------------");
            log.warn("  <generateInstanceFields/> = false is deprecated! This feature is no longer maintained and will be removed in jOOQ 4.0. Please adapt your configuration.");
        }

        log.info("");
        logGenerationRemarks(db);

        log.info("");
        log.info("----------------------------------------------------------");

        // ----------------------------------------------------------------------
        // XXX Generating catalogs
        // ----------------------------------------------------------------------
        log.info("Generating catalogs", "Total: " + database.getCatalogs().size());
        for (CatalogDefinition catalog : database.getCatalogs()) {
            try {
                if (generateCatalogIfEmpty(catalog))
                    generate(catalog);
                else
                    log.info("Excluding empty catalog", catalog);
            }
            catch (Exception e) {
                throw new GeneratorException("Error generating code for catalog " + catalog, e);
            }
        }

        // [#5556] Clean up common parent directory
        log.info("Removing excess files");
        empty(getStrategy().getFileRoot(), (scala ? ".scala" : kotlin ? ".kt" : ".java"), files, directoriesNotForRemoval);
        directoriesNotForRemoval.clear();
        files.clear();
    }

    private boolean generateCatalogIfEmpty(CatalogDefinition catalog) {
        if (generateEmptyCatalogs())
            return true;

        List<SchemaDefinition> schemas = catalog.getSchemata();
        if (schemas.isEmpty())
            return false;

        for (SchemaDefinition schema : schemas)
            if (generateSchemaIfEmpty(schema))
                return true;

        return false;
    }

    private final boolean generateSchemaIfEmpty(SchemaDefinition schema) {
        if (generateEmptySchemas())
            return true;

        if (database.getArrays(schema).isEmpty()
            && database.getEnums(schema).isEmpty()
            && database.getPackages(schema).isEmpty()
            && database.getRoutines(schema).isEmpty()
            && database.getSequences(schema).isEmpty()
            && database.getTables(schema).isEmpty()
            && database.getUDTs(schema).isEmpty())
            return false;

        return true;
    }

    private void generate(CatalogDefinition catalog) {
        String newVersion = catalog.getDatabase().getCatalogVersionProvider().version(catalog);

        if (StringUtils.isBlank(newVersion)) {
            log.info("No schema version is applied for catalog " + catalog.getInputName() + ". Regenerating.");
        }
        else {
            catalogVersions.put(catalog, newVersion);
            String oldVersion = readVersion(getFile(catalog), "catalog");

            if (StringUtils.isBlank(oldVersion)) {
                log.info("No previous version available for catalog " + catalog.getInputName() + ". Regenerating.");
            }
            else if (!oldVersion.equals(newVersion)) {
                log.info("Existing version " + oldVersion + " is not up to date with " + newVersion + " for catalog " + catalog.getInputName() + ". Regenerating.");
            }
            else {
                log.info("Existing version " + oldVersion + " is up to date with " + newVersion + " for catalog " + catalog.getInputName() + ". Ignoring catalog.");

                // [#5614] If a catalog is not regenerated, we must flag it as "not for removal", because its contents
                //         will not be listed in the files directory.
                directoriesNotForRemoval.add(getFile(catalog).getParentFile());
                return;
            }
        }

        generateCatalog(catalog);

        log.info("Generating schemata", "Total: " + catalog.getSchemata().size());
        for (SchemaDefinition schema : catalog.getSchemata()) {
            try {
                if (generateSchemaIfEmpty(schema))
                    generate(schema);
                else
                    log.info("Excluding empty schema", schema);
            }
            catch (Exception e) {
                throw new GeneratorException("Error generating code for schema " + schema, e);
            }
        }
    }

    private void generate(SchemaDefinition schema) {
        String newVersion = schema.getDatabase().getSchemaVersionProvider().version(schema);

        if (StringUtils.isBlank(newVersion)) {
            log.info("No schema version is applied for schema " + schema.getInputName() + ". Regenerating.");
        }
        else {
            schemaVersions.put(schema, newVersion);
            String oldVersion = readVersion(getFile(schema), "schema");

            if (StringUtils.isBlank(oldVersion)) {
                log.info("No previous version available for schema " + schema.getInputName() + ". Regenerating.");
            }
            else if (!oldVersion.equals(newVersion)) {
                log.info("Existing version " + oldVersion + " is not up to date with " + newVersion + " for schema " + schema.getInputName() + ". Regenerating.");
            }
            else {
                log.info("Existing version " + oldVersion + " is up to date with " + newVersion + " for schema " + schema.getInputName() + ". Ignoring schema.");

                // [#5614] If a schema is not regenerated, we must flag it as "not for removal", because its contents
                //         will not be listed in the files directory.
                directoriesNotForRemoval.add(getFile(schema).getParentFile());
                return;
            }
        }

        // ----------------------------------------------------------------------
        // XXX Initialising
        // ----------------------------------------------------------------------
        generateSchema(schema);

        if (generateGlobalSequenceReferences() && database.getSequences(schema).size() > 0)
            generateSequences(schema);

        if (generateTables() && database.getTables(schema).size() > 0)
            generateTables(schema);

        if (generateEmbeddables() && database.getEmbeddables(schema).size() > 0)
            generateEmbeddables(schema);

        if (generatePojos() && database.getTables(schema).size() > 0)
            generatePojos(schema);

        if (generateDaos() && database.getTables(schema).size() > 0)
            generateDaos(schema);

        if (generateGlobalTableReferences() && database.getTables(schema).size() > 0)
            generateTableReferences(schema);

        if (generateGlobalKeyReferences() && generateRelations() && database.getTables(schema).size() > 0)
            generateRelations(schema);

        if (generateGlobalIndexReferences() && database.getTables(schema).size() > 0)
            generateIndexes(schema);

        if (generateRecords() && database.getTables(schema).size() > 0)
            generateRecords(schema);

        if (generateInterfaces() && database.getTables(schema).size() > 0)
            generateInterfaces(schema);

        if (generateUDTs() && database.getUDTs(schema).size() > 0)
            generateUDTs(schema);

        if (generatePojos() && database.getUDTs(schema).size() > 0)
            generateUDTPojos(schema);

        if (generateUDTs() && generateRecords() && database.getUDTs(schema).size() > 0)
            generateUDTRecords(schema);

        if (generateInterfaces() && database.getUDTs(schema).size() > 0)
            generateUDTInterfaces(schema);

        if (generateUDTs() && generateRoutines() && database.getUDTs(schema).size() > 0)
            generateUDTRoutines(schema);

        if (generateGlobalUDTReferences() && database.getUDTs(schema).size() > 0)
            generateUDTReferences(schema);

        if (generateUDTs() && database.getArrays(schema).size() > 0)
            generateArrays(schema);

        if (generateUDTs() && database.getEnums(schema).size() > 0)
            generateEnums(schema);

        if (generateUDTs() && database.getDomains(schema).size() > 0)
            generateDomainReferences(schema);

        if (generateRoutines() && (database.getRoutines(schema).size() > 0 || hasTableValuedFunctions(schema)))
            generateRoutines(schema);











        // XXX [#651] Refactoring-cursor
        watch.splitInfo("Generation finished: " + schema.getQualifiedName());
        log.info("");
    }

    private class AvoidAmbiguousClassesFilter implements Database.Filter {

        private Map<String, String> included = new HashMap<>();

        @Override
        public boolean exclude(Definition definition) {

            // These definitions don't generate types of their own.
            if (    definition instanceof ColumnDefinition
                 || definition instanceof AttributeDefinition
                 || definition instanceof ParameterDefinition)
                return false;

            // Check if we've previously encountered a Java type of the same case-insensitive, fully-qualified name.
            String name = getStrategy().getFullJavaClassName(definition);
            String nameLC = name.toLowerCase();
            String existing = included.put(nameLC, name);

            if (existing == null)
                return false;

            log.warn("Ambiguous type name", "The object " + definition.getQualifiedOutputName() + " generates a type " + name + " which conflicts with the existing type " + existing + " on some operating systems. Use a custom generator strategy to disambiguate the types.");
            return true;
        }
    }





















































































    private boolean hasTableValuedFunctions(SchemaDefinition schema) {
        for (TableDefinition table : database.getTables(schema)) {
            if (table.isTableValuedFunction()) {
                return true;
            }
        }

        return false;
    }

    protected void generateRelations(SchemaDefinition schema) {
        log.info("Generating Keys");

        boolean empty = true;
        JavaWriter out = newJavaWriter(new File(getFile(schema).getParentFile(), "Keys.java"));
        printPackage(out, schema);
        printClassJavadoc(out,
            "A class modelling foreign key relationships and constraints of tables in " + schemaNameOrDefault(schema) + ".");
        printClassAnnotations(out, schema, Mode.DEFAULT);

        if (scala || kotlin)
            out.println("object Keys {");
        else
            out.println("public class Keys {");

        List<UniqueKeyDefinition> allUniqueKeys = new ArrayList<>();
        List<ForeignKeyDefinition> allForeignKeys = new ArrayList<>();

        // Unique keys
        out.header("UNIQUE and PRIMARY KEY definitions");
        out.println();

        for (TableDefinition table : database.getTables(schema)) {
            try {
                List<UniqueKeyDefinition> uniqueKeys = table.getUniqueKeys();

                for (UniqueKeyDefinition uniqueKey : uniqueKeys) {
                    empty = false;

                    final String keyType = out.ref(getStrategy().getFullJavaClassName(uniqueKey.getTable(), Mode.RECORD));
                    final String keyId = getStrategy().getJavaIdentifier(uniqueKey);
                    final int block = allUniqueKeys.size() / INITIALISER_SIZE;

                    if (scala || kotlin)
                        out.println("val %s = UniqueKeys%s.%s", keyId, block, keyId);
                    else
                        out.println("public static final %s<%s> %s = UniqueKeys%s.%s;", UniqueKey.class, keyType, keyId, block, keyId);

                    allUniqueKeys.add(uniqueKey);
                }
            }
            catch (Exception e) {
                log.error("Error while generating table " + table, e);
            }
        }

        // Foreign keys
        out.header("FOREIGN KEY definitions");
        out.println();

        for (TableDefinition table : database.getTables(schema)) {
            try {
                List<ForeignKeyDefinition> foreignKeys = table.getForeignKeys();

                for (ForeignKeyDefinition foreignKey : foreignKeys) {
                    empty = false;

                    final String keyType = out.ref(getStrategy().getFullJavaClassName(foreignKey.getKeyTable(), Mode.RECORD));
                    final String referencedType = out.ref(getStrategy().getFullJavaClassName(foreignKey.getReferencedTable(), Mode.RECORD));
                    final String keyId = getStrategy().getJavaIdentifier(foreignKey);
                    final int block = allForeignKeys.size() / INITIALISER_SIZE;

                    if (scala || kotlin)
                        out.println("val %s = ForeignKeys%s.%s", keyId, block, keyId);
                    else
                        out.println("public static final %s<%s, %s> %s = ForeignKeys%s.%s;", ForeignKey.class, keyType, referencedType, keyId, block, keyId);

                    allForeignKeys.add(foreignKey);
                }
            }
            catch (Exception e) {
                log.error("Error while generating reference " + table, e);
            }
        }

        // [#1459] Print nested classes for actual static field initialisations
        // keeping top-level initialiser small
        int uniqueKeyCounter = 0;
        int foreignKeyCounter = 0;

        out.header("[#1459] distribute members to avoid static initialisers > 64kb");

        // UniqueKeys
        // ----------

        for (UniqueKeyDefinition uniqueKey : allUniqueKeys) {
            printUniqueKey(out, uniqueKeyCounter, uniqueKey);
            uniqueKeyCounter++;
        }

        if (uniqueKeyCounter > 0)
            out.println("}");

        // ForeignKeys
        // -----------

        for (ForeignKeyDefinition foreignKey : allForeignKeys) {
            printForeignKey(out, foreignKeyCounter, foreignKey);
            foreignKeyCounter++;
        }

        if (foreignKeyCounter > 0)
            out.println("}");

        out.println("}");

        if (empty) {
            log.info("Skipping empty keys");
        }
        else {
            closeJavaWriter(out);
            watch.splitInfo("Keys generated");
        }
    }

    protected void generateIndexes(SchemaDefinition schema) {
        log.info("Generating Indexes");

        if (database.getIndexes(schema).isEmpty()) {
            log.info("Skipping empty indexes");
            return;
        }

        JavaWriter out = newJavaWriter(new File(getFile(schema).getParentFile(), "Indexes.java"));
        printPackage(out, schema);
        printClassJavadoc(out,
            "A class modelling indexes of tables in "  + schemaNameOrDefault(schema) + ".");
        printClassAnnotations(out, schema, Mode.DEFAULT);

        if (scala || kotlin)
            out.println("object Indexes {");
        else
            out.println("public class Indexes {");

        List<IndexDefinition> allIndexes = new ArrayList<>();

        out.header("INDEX definitions");
        out.println();

        for (TableDefinition table : database.getTables(schema)) {
            try {
                List<IndexDefinition> indexes = table.getIndexes();

                for (IndexDefinition index : indexes) {
                    final String keyId = getStrategy().getJavaIdentifier(index);
                    final int block = allIndexes.size() / INITIALISER_SIZE;

                    if (scala || kotlin)
                        out.println("val %s = Indexes%s.%s", keyId, block, keyId);
                    else
                        out.println("public static final %s %s = Indexes%s.%s;", Index.class, keyId, block, keyId);

                    allIndexes.add(index);
                }
            }
            catch (Exception e) {
                log.error("Error while generating table " + table, e);
            }
        }

        // [#1459] Print nested classes for actual static field initialisations
        // keeping top-level initialiser small
        int indexCounter = 0;
        out.header("[#1459] distribute members to avoid static initialisers > 64kb");

        // Indexes
        // -------

        for (IndexDefinition index : allIndexes) {
            printIndex(out, indexCounter, index);
            indexCounter++;
        }

        if (indexCounter > 0)
            out.println("}");

        out.println("}");
        closeJavaWriter(out);

        watch.splitInfo("Indexes generated");
    }

    protected void printIndex(JavaWriter out, int indexCounter, IndexDefinition index) {
        final int block = indexCounter / INITIALISER_SIZE;

        // Print new nested class
        if (indexCounter % INITIALISER_SIZE == 0) {
            if (indexCounter > 0)
                out.println("}");

            out.println();

            if (scala || kotlin)
                out.println("private object Indexes%s {", block);
            else
                out.println("private static class Indexes%s {", block);
        }

        if (scala || kotlin)
            out.print("val %s: %s = ",
                getStrategy().getJavaIdentifier(index),
                Index.class
            );
        else
            out.print("static final %s %s = ",
                Index.class,
                getStrategy().getJavaIdentifier(index)
            );

        printCreateIndex(out, index);

        if (scala || kotlin)
            out.println();
        else
            out.println(";");
    }

    private void printCreateIndex(JavaWriter out, IndexDefinition index) {
        String sortFieldSeparator = "";
        StringBuilder orderFields = new StringBuilder();

        for (IndexColumnDefinition column : index.getIndexColumns()) {
            orderFields.append(sortFieldSeparator);
            orderFields.append(out.ref(getStrategy().getFullJavaIdentifier(column.getColumn()), colRefSegments(null)));
            orderFields.append(column.getSortOrder() == DESC ? ".desc()" : "");

            sortFieldSeparator = ", ";
        }

        if (scala)
            out.print("%s.createIndex(%s.name(\"%s\"), %s, Array[%s [_] ](%s), %s)",
                Internal.class,
                DSL.class,
                escapeString(index.getOutputName()),
                out.ref(getStrategy().getFullJavaIdentifier(index.getTable()), 2),
                OrderField.class,
                orderFields,
                index.isUnique()
            );
        else if (kotlin)
            out.print("%s.createIndex(%s.name(\"%s\"), %s, arrayOf(%s), %s)",
                Internal.class,
                DSL.class,
                escapeString(index.getOutputName()),
                out.ref(getStrategy().getFullJavaIdentifier(index.getTable()), 2),
                orderFields,
                index.isUnique()
            );
        else
            out.print("%s.createIndex(%s.name(\"%s\"), %s, new %s[] { %s }, %s)",
                Internal.class,
                DSL.class,
                escapeString(index.getOutputName()),
                out.ref(getStrategy().getFullJavaIdentifier(index.getTable()), 2),
                OrderField.class,
                orderFields,
                index.isUnique()
            );
    }

    protected void printUniqueKey(JavaWriter out, int uniqueKeyCounter, UniqueKeyDefinition uniqueKey) {
        final int block = uniqueKeyCounter / INITIALISER_SIZE;

        // Print new nested class
        if (uniqueKeyCounter % INITIALISER_SIZE == 0) {
            if (uniqueKeyCounter > 0)
                out.println("}");

            out.println();

            if (scala || kotlin)
                out.println("private object UniqueKeys%s {", block);
            else
                out.println("private static class UniqueKeys%s {", block);
        }

        if (scala)
            out.print("val %s: %s[%s] = ",
                getStrategy().getJavaIdentifier(uniqueKey),
                UniqueKey.class,
                out.ref(getStrategy().getFullJavaClassName(uniqueKey.getTable(), Mode.RECORD)));
        else if (kotlin)
            out.print("val %s: %s<%s> = ",
                getStrategy().getJavaIdentifier(uniqueKey),
                UniqueKey.class,
                out.ref(getStrategy().getFullJavaClassName(uniqueKey.getTable(), Mode.RECORD)));
        else
            out.print("static final %s<%s> %s = ",
                UniqueKey.class,
                out.ref(getStrategy().getFullJavaClassName(uniqueKey.getTable(), Mode.RECORD)),
                getStrategy().getJavaIdentifier(uniqueKey));

        printCreateUniqueKey(out, uniqueKey);

        if (scala || kotlin)
            out.println();
        else
            out.println(";");
    }

    private void printCreateUniqueKey(JavaWriter out, UniqueKeyDefinition uniqueKey) {
        if (scala)
            out.print("%s.createUniqueKey(%s, %s.name(\"%s\"), Array([[%s]]).asInstanceOf[Array[%s[%s, _] ] ], %s)",
                Internal.class,
                out.ref(getStrategy().getFullJavaIdentifier(uniqueKey.getTable()), 2),
                DSL.class,
                escapeString(uniqueKey.getOutputName()),
                out.ref(getStrategy().getFullJavaIdentifiers(uniqueKey.getKeyColumns()), colRefSegments(null)),
                TableField.class,
                out.ref(getStrategy().getJavaClassName(uniqueKey.getTable(), Mode.RECORD)),
                uniqueKey.enforced());
        else if (kotlin)
            out.print("%s.createUniqueKey(%s, %s.name(\"%s\"), arrayOf([[%s]]), %s)",
                Internal.class,
                out.ref(getStrategy().getFullJavaIdentifier(uniqueKey.getTable()), 2),
                DSL.class,
                escapeString(uniqueKey.getOutputName()),
                out.ref(getStrategy().getFullJavaIdentifiers(uniqueKey.getKeyColumns()), colRefSegments(null)),
                uniqueKey.enforced());
        else
            out.print("%s.createUniqueKey(%s, %s.name(\"%s\"), new %s[] { [[%s]] }, %s)",
                Internal.class,
                out.ref(getStrategy().getFullJavaIdentifier(uniqueKey.getTable()), 2),
                DSL.class,
                escapeString(uniqueKey.getOutputName()),
                TableField.class,
                out.ref(getStrategy().getFullJavaIdentifiers(uniqueKey.getKeyColumns()), colRefSegments(null)),
                uniqueKey.enforced());
    }

    protected void printForeignKey(JavaWriter out, int foreignKeyCounter, ForeignKeyDefinition foreignKey) {
        final int block = foreignKeyCounter / INITIALISER_SIZE;

        // Print new nested class
        if (foreignKeyCounter % INITIALISER_SIZE == 0) {
            if (foreignKeyCounter > 0)
                out.println("}");

            out.println();

            if (scala || kotlin)
                out.println("private object ForeignKeys%s {", block);
            else
                out.println("private static class ForeignKeys%s {", block);
        }

        if (scala)
            out.println("val %s: %s[%s, %s] = %s.createForeignKey(%s, %s.name(\"%s\"), Array([[%s]]).asInstanceOf[Array[%s[%s, _] ] ], %s, Array([[%s]]).asInstanceOf[Array[%s[%s, _] ] ], %s)",
                getStrategy().getJavaIdentifier(foreignKey),
                ForeignKey.class,
                out.ref(getStrategy().getFullJavaClassName(foreignKey.getKeyTable(), Mode.RECORD)),
                out.ref(getStrategy().getFullJavaClassName(foreignKey.getReferencedTable(), Mode.RECORD)),
                Internal.class,
                out.ref(getStrategy().getFullJavaIdentifier(foreignKey.getKeyTable()), 2),
                DSL.class,
                escapeString(foreignKey.getOutputName()),
                out.ref(getStrategy().getFullJavaIdentifiers(foreignKey.getKeyColumns()), colRefSegments(null)),
                TableField.class,
                out.ref(getStrategy().getJavaClassName(foreignKey.getTable(), Mode.RECORD)),
                out.ref(getStrategy().getFullJavaIdentifier(foreignKey.getReferencedKey()), 2),
                out.ref(getStrategy().getFullJavaIdentifiers(foreignKey.getReferencedColumns()), colRefSegments(null)),
                TableField.class,
                out.ref(getStrategy().getJavaClassName(foreignKey.getReferencedTable(), Mode.RECORD)),
                foreignKey.enforced()
            );
        else if (kotlin)
            out.println("val %s: %s<%s, %s> = %s.createForeignKey(%s, %s.name(\"%s\"), arrayOf([[%s]]), %s, arrayOf([[%s]]), %s)",
                getStrategy().getJavaIdentifier(foreignKey),
                ForeignKey.class,
                out.ref(getStrategy().getFullJavaClassName(foreignKey.getKeyTable(), Mode.RECORD)),
                out.ref(getStrategy().getFullJavaClassName(foreignKey.getReferencedTable(), Mode.RECORD)),
                Internal.class,
                out.ref(getStrategy().getFullJavaIdentifier(foreignKey.getKeyTable()), 2),
                DSL.class,
                escapeString(foreignKey.getOutputName()),
                out.ref(getStrategy().getFullJavaIdentifiers(foreignKey.getKeyColumns()), colRefSegments(null)),
                out.ref(getStrategy().getFullJavaIdentifier(foreignKey.getReferencedKey()), 2),
                out.ref(getStrategy().getFullJavaIdentifiers(foreignKey.getReferencedColumns()), colRefSegments(null)),
                foreignKey.enforced()
            );
        else
            out.println("static final %s<%s, %s> %s = %s.createForeignKey(%s, %s.name(\"%s\"), new %s[] { [[%s]] }, %s, new %s[] { [[%s]] }, %s);",
                ForeignKey.class,
                out.ref(getStrategy().getFullJavaClassName(foreignKey.getKeyTable(), Mode.RECORD)),
                out.ref(getStrategy().getFullJavaClassName(foreignKey.getReferencedTable(), Mode.RECORD)),
                getStrategy().getJavaIdentifier(foreignKey),
                Internal.class,
                out.ref(getStrategy().getFullJavaIdentifier(foreignKey.getKeyTable()), 2),
                DSL.class,
                escapeString(foreignKey.getOutputName()),
                TableField.class,
                out.ref(getStrategy().getFullJavaIdentifiers(foreignKey.getKeyColumns()), colRefSegments(null)),
                out.ref(getStrategy().getFullJavaIdentifier(foreignKey.getReferencedKey()), 2),
                TableField.class,
                out.ref(getStrategy().getFullJavaIdentifiers(foreignKey.getReferencedColumns()), colRefSegments(null)),
                foreignKey.enforced()
            );
    }

    protected void generateRecords(SchemaDefinition schema) {
        log.info("Generating table records");

        for (TableDefinition table : database.getTables(schema)) {
            try {
                generateRecord(table);
            } catch (Exception e) {
                log.error("Error while generating table record " + table, e);
            }
        }

        watch.splitInfo("Table records generated");
    }


    protected void generateRecord(TableDefinition table) {
        JavaWriter out = newJavaWriter(getFile(table, Mode.RECORD));
        log.info("Generating record", out.file().getName());
        generateRecord(table, out);
        closeJavaWriter(out);
    }

    protected void generateUDTRecord(UDTDefinition udt) {
        JavaWriter out = newJavaWriter(getFile(udt, Mode.RECORD));
        log.info("Generating record", out.file().getName());
        generateRecord0(udt, out);
        closeJavaWriter(out);
    }

    protected void generateRecord(TableDefinition table, JavaWriter out) {
        generateRecord0(table, out);
    }

    protected void generateUDTRecord(UDTDefinition udt, JavaWriter out) {
        generateRecord0(udt, out);
    }

    private final void generateRecord0(Definition tableUdtOrEmbeddable, JavaWriter out) {
        final UniqueKeyDefinition key = (tableUdtOrEmbeddable instanceof TableDefinition)
            ? ((TableDefinition) tableUdtOrEmbeddable).getPrimaryKey()
            : null;
        final String className = getStrategy().getJavaClassName(tableUdtOrEmbeddable, Mode.RECORD);
        final String tableIdentifier = !(tableUdtOrEmbeddable instanceof EmbeddableDefinition)
            ? out.ref(getStrategy().getFullJavaIdentifier(tableUdtOrEmbeddable), 2)
            : null;
        final List<String> interfaces = out.ref(getStrategy().getJavaClassImplements(tableUdtOrEmbeddable, Mode.RECORD));
        final List<? extends TypedElementDefinition<?>> columns = getTypedElements(tableUdtOrEmbeddable);

        printPackage(out, tableUdtOrEmbeddable, Mode.RECORD);

        if (tableUdtOrEmbeddable instanceof TableDefinition)
            generateRecordClassJavadoc((TableDefinition) tableUdtOrEmbeddable, out);
        else if (tableUdtOrEmbeddable instanceof EmbeddableDefinition)
            generateEmbeddableClassJavadoc((EmbeddableDefinition) tableUdtOrEmbeddable, out);
        else
            generateUDTRecordClassJavadoc((UDTDefinition) tableUdtOrEmbeddable, out);

        printClassAnnotations(out, tableUdtOrEmbeddable, Mode.RECORD);
        if (tableUdtOrEmbeddable instanceof TableDefinition)
            printTableJPAAnnotation(out, (TableDefinition) tableUdtOrEmbeddable);

        Class<?> baseClass;
        if (tableUdtOrEmbeddable instanceof UDTDefinition)
            baseClass = UDTRecordImpl.class;
        else if (tableUdtOrEmbeddable instanceof EmbeddableDefinition)
            baseClass = EmbeddableRecordImpl.class;
        else if (generateRelations() && key != null)
            baseClass = UpdatableRecordImpl.class;
        else
            baseClass = TableRecordImpl.class;

        int degree = columns.size();
        String rowType = null;
        String rowTypeRecord = null;

        // [#3130] Invalid UDTs may have a degree of 0
        // [#6072] Generate these super types only if configured to do so
        if (generateRecordsImplementingRecordN() && degree > 0 && degree <= Constants.MAX_ROW_DEGREE) {
            rowType = refRowType(out, columns);

            if (scala)
                rowTypeRecord = out.ref(Record.class.getName() + degree) + "[" + rowType + "]";
            else
                rowTypeRecord = out.ref(Record.class.getName() + degree) + "<" + rowType + ">";

            interfaces.add(rowTypeRecord);
        }

        if (generateInterfaces())
            interfaces.add(out.ref(getStrategy().getFullJavaClassName(tableUdtOrEmbeddable, Mode.INTERFACE)));

        if (scala) {
            if (tableUdtOrEmbeddable instanceof EmbeddableDefinition) {
                out.println("private object %s {", className);
                out.println("val FIELDS: Array[%s [_] ] = Array(", Field.class);

                String separator = "  ";
                for (EmbeddableColumnDefinition column : ((EmbeddableDefinition) tableUdtOrEmbeddable).getColumns()) {
                    final String colIdentifier = out.ref(getStrategy().getFullJavaIdentifier(column.getColumn()), colRefSegments(column));

                    out.println("%s%s.field(%s.name(\"%s\"), %s.getDataType)", separator, DSL.class, DSL.class, column.getOutputName(), colIdentifier);
                    separator = ", ";
                }

                out.println(")");
                out.println("}");
                out.println();
            }
        }

        if (scala)
            if (tableUdtOrEmbeddable instanceof EmbeddableDefinition)
                out.println("class %s extends %s[%s](%s.FIELDS:_*)[[before= with ][separator= with ][%s]] {", className, baseClass, className, className, interfaces);
            else
                out.println("class %s extends %s[%s](%s)[[before= with ][separator= with ][%s]] {", className, baseClass, className, tableIdentifier, interfaces);
        else if (kotlin)
            if (tableUdtOrEmbeddable instanceof EmbeddableDefinition)
                out.println("class %s() : %s<%s>(*FIELDS)[[before=, ][%s]] {", className, baseClass, className, interfaces);
            else
                out.println("class %s() : %s<%s>(%s)[[before=, ][%s]] {", className, baseClass, className, tableIdentifier, interfaces);
        else
            out.println("public class %s extends %s<%s>[[before= implements ][%s]] {", className, baseClass, className, interfaces);

        out.printSerial();

        for (int i = 0; i < degree; i++) {
            TypedElementDefinition<?> column = columns.get(i);

            if (kotlin) {

                // TODO: The Mode should be INTERFACE
                final String member = getStrategy().getJavaMemberName(column, Mode.POJO);
                final String typeFull = getJavaType(column.getType(resolver(Mode.INTERFACE)), Mode.INTERFACE);
                final String type = out.ref(typeFull);

                out.println();
                out.println("%svar %s: %s?",
                    (generateInterfaces() ? "override " : ""), member, type);
                out.tab(1).println("get() = get(%s) as %s?", i, type);
                out.tab(1).println("set(value) = set(%s, value)", i);
            }
            else {
                if (tableUdtOrEmbeddable instanceof TableDefinition) {
                    generateRecordSetter(column, i, out);
                    generateRecordGetter(column, i, out);
                }
                else if (tableUdtOrEmbeddable instanceof EmbeddableDefinition) {
                    generateEmbeddableSetter(column, i, out);
                    generateEmbeddableGetter(column, i, out);
                }
                else {
                    generateUDTRecordSetter(column, i, out);
                    generateUDTRecordGetter(column, i, out);
                }
            }
        }

        if (tableUdtOrEmbeddable instanceof TableDefinition) {
            List<EmbeddableDefinition> embeddables = ((TableDefinition) tableUdtOrEmbeddable).getEmbeddables();

            for (int i = 0; i < embeddables.size(); i++) {
                EmbeddableDefinition embeddable = embeddables.get(i);

                // [#2530] TODO: Implement setters and getters for embeddables here
            }
        }

        if (generateRelations() && key != null) {
            int keyDegree = key.getKeyColumns().size();

            if (keyDegree <= Constants.MAX_ROW_DEGREE) {
                final String recordNType = out.ref(Record.class.getName() + keyDegree);
                final String keyType = refRowType(out, key.getKeyColumns());
                out.header("Primary key information");

                if (scala) {
                    out.println();
                    out.println("override def key: %s[%s] = super.key.asInstanceOf[ %s[%s] ]", recordNType, keyType, recordNType, keyType);
                }
                else if (kotlin) {
                    out.println();
                    out.println("override fun key(): %s<%s> = super.key() as %s<%s>", recordNType, keyType, recordNType, keyType);
                }
                else {
                    out.overrideInherit();
                    out.println("public %s<%s> key() {", recordNType, keyType);
                    out.println("return (%s) super.key();", recordNType);
                    out.println("}");
                }
            }
        }

        if (tableUdtOrEmbeddable instanceof UDTDefinition) {

            // [#799] Oracle UDT's can have member procedures
            for (RoutineDefinition routine : ((UDTDefinition) tableUdtOrEmbeddable).getRoutines()) {

                // Instance methods ship with a SELF parameter at the first position
                // [#1584] Static methods don't have that
                boolean instance = routine.getInParameters().size() > 0
                                && routine.getInParameters().get(0).getInputName().toUpperCase().equals("SELF");

                try {
                    if (!routine.isSQLUsable()) {
                        // Instance execute() convenience method
                        printConvenienceMethodProcedure(out, routine, instance);
                    }
                    else {
                        // Instance execute() convenience method
                        if (!routine.isAggregate()) {
                            printConvenienceMethodFunction(out, routine, instance);
                        }
                    }

                } catch (Exception e) {
                    log.error("Error while generating routine " + routine, e);
                }
            }
        }

        // [#3130] Invalid UDTs may have a degree of 0
        if (generateRecordsImplementingRecordN() && degree > 0 && degree <= Constants.MAX_ROW_DEGREE) {
            final String recordNType = out.ref(Row.class.getName() + degree);

            out.header("Record%s type implementation", degree);

            // fieldsRow()
            if (scala) {
                out.println();
                out.println("override def fieldsRow: %s[%s] = super.fieldsRow.asInstanceOf[ %s[%s] ]", recordNType, rowType, recordNType, rowType);
            }
            else if (kotlin) {
                out.println();
                out.println("override fun fieldsRow(): %s<%s> = super.fieldsRow() as %s<%s>", recordNType, rowType, recordNType, rowType);
            }
            else {
                out.overrideInherit();
                out.println("public %s<%s> fieldsRow() {", recordNType, rowType);
                out.println("return (%s) super.fieldsRow();", recordNType);
                out.println("}");
            }

            // valuesRow()
            if (scala) {
                out.println();
                out.println("override def valuesRow: %s[%s] = super.valuesRow.asInstanceOf[ %s[%s] ]", recordNType, rowType, recordNType, rowType);
            }
            else if (kotlin) {
                out.println("override fun valuesRow(): %s<%s> = super.valuesRow() as %s<%s>", recordNType, rowType, recordNType, rowType);
            }
            else {
                out.overrideInherit();
                out.println("public %s<%s> valuesRow() {", recordNType, rowType);
                out.println("return (%s) super.valuesRow();", recordNType);
                out.println("}");
            }

            // field[N]()
            for (int i = 1; i <= degree; i++) {
                TypedElementDefinition<?> column = columns.get(i - 1);

                if (column instanceof EmbeddableColumnDefinition)
                    column = ((EmbeddableColumnDefinition) column).getColumn();

                final String colTypeFull = getJavaType(column.getType(resolver()));
                final String colType = out.ref(colTypeFull);
                final String colIdentifier = out.ref(getStrategy().getFullJavaIdentifier(column), colRefSegments(column));

                if (scala) {
                    printDeprecationIfUnknownType(out, colTypeFull);

                    if (tableUdtOrEmbeddable instanceof EmbeddableDefinition)
                        out.println("override def field%s: %s[%s] = %s.FIELDS(%s).asInstanceOf[%s [%s] ]", i, Field.class, colType, i - 1, className, Field.class, colType);
                    else
                        out.println("override def field%s: %s[%s] = %s", i, Field.class, colType, colIdentifier);
                }
                else if (kotlin) {
                    printDeprecationIfUnknownType(out, colTypeFull);

                    if (tableUdtOrEmbeddable instanceof EmbeddableDefinition)
                        out.println("override fun field%s(): %s<%s?> = FIELDS[%s] as %s<%s>", i, Field.class, colType, i - 1, Field.class, colType);
                    else
                        out.println("override fun field%s(): %s<%s?> = %s", i, Field.class, colType, colIdentifier);
                }
                else {
                    if (printDeprecationIfUnknownType(out, colTypeFull))
                        out.override();
                    else
                        out.overrideInherit();

                    out.println("public %s<%s> field%s() {", Field.class, colType, i);

                    if (tableUdtOrEmbeddable instanceof EmbeddableDefinition)
                        out.println("return (%s<%s>) FIELDS[%s];", Field.class, colType, i - 1);
                    else
                        out.println("return %s;", colIdentifier);

                    out.println("}");
                }
            }

            // component[N]()
            for (int i = 1; i <= degree; i++) {
                TypedElementDefinition<?> column = columns.get(i - 1);

                final String colTypeFull = getJavaType(column.getType(resolver()));
                final String colType = out.ref(colTypeFull);
                final String colGetter = getStrategy().getJavaGetterName(column, Mode.RECORD);
                final String colMember = getStrategy().getJavaMemberName(column, Mode.POJO);

                if (scala) {
                    printDeprecationIfUnknownType(out, colTypeFull);
                    out.println("override def component%s: %s = %s", i, colType, colGetter);
                }
                else if (kotlin) {
                    printDeprecationIfUnknownType(out, colTypeFull);
                    out.println("override fun component%s(): %s? = %s", i, colType, colMember);
                }
                else {
                    if (printDeprecationIfUnknownType(out, colTypeFull))
                        out.override();
                    else
                        out.overrideInherit();

                    printNullableOrNonnullAnnotation(out, column);

                    out.println("public %s component%s() {", colType, i);
                    out.println("return %s();", colGetter);
                    out.println("}");
                }
            }

            // value[N]()
            for (int i = 1; i <= degree; i++) {
                TypedElementDefinition<?> column = columns.get(i - 1);

                final String colTypeFull = getJavaType(column.getType(resolver()));
                final String colType = out.ref(colTypeFull);
                final String colGetter = getStrategy().getJavaGetterName(column, Mode.RECORD);
                final String colMember = getStrategy().getJavaMemberName(column, Mode.POJO);

                if (scala) {
                    printDeprecationIfUnknownType(out, colTypeFull);
                    out.println("override def value%s: %s = %s", i, colType, colGetter);
                }
                else if (kotlin) {
                    printDeprecationIfUnknownType(out, colTypeFull);
                    out.println("override fun value%s(): %s? = %s", i, colType, colMember);
                }
                else {
                    if (printDeprecationIfUnknownType(out, colTypeFull))
                        out.override();
                    else
                        out.overrideInherit();

                    printNullableOrNonnullAnnotation(out, column);

                    out.println("public %s value%s() {", colType, i);
                    out.println("return %s();", colGetter);
                    out.println("}");
                }
            }

            // value[N](T[N])
            for (int i = 1; i <= degree; i++) {
                TypedElementDefinition<?> column = columns.get(i - 1);

                final String colTypeFull = getJavaType(column.getType(resolver()));
                final String colType = out.ref(colTypeFull);
                final String colSetter = getStrategy().getJavaSetterName(column, Mode.RECORD);
                final String colMember = getStrategy().getJavaMemberName(column, Mode.POJO);

                if (scala) {
                    out.println();
                    printDeprecationIfUnknownType(out, colTypeFull);
                    out.println("override def value%s(value: %s): %s = {", i, colType, className);
                    out.println("%s(value)", colSetter);
                    out.println("this");
                    out.println("}");
                }
                else if (kotlin) {
                    out.println();
                    printDeprecationIfUnknownType(out, colTypeFull);
                    out.println("override fun value%s(value: %s?): %s {", i, colType, className);
                    out.println("%s = value", colMember);
                    out.println("return this");
                    out.println("}");
                }
                else {
                    final String nullableAnnotation = nullableOrNonnullAnnotation(out, column);

                    if (printDeprecationIfUnknownType(out, colTypeFull))
                        out.override();
                    else
                        out.overrideInherit();

                    out.println("public %s value%s([[before=@][after= ][%s]]%s value) {", className, i, list(nullableAnnotation), varargsIfArray(colType));
                    out.println("%s(value);", colSetter);
                    out.println("return this;");
                    out.println("}");
                }
            }

            List<String> arguments = new ArrayList<>(degree);
            List<String> calls = new ArrayList<>(degree);
            for (int i = 1; i <= degree; i++) {
                TypedElementDefinition<?> column = columns.get(i - 1);

                final String colType = out.ref(getJavaType(column.getType(resolver())));

                if (scala) {
                    arguments.add("value" + i + " : " + colType);
                    calls.add("this.value" + i + "(value" + i + ")");
                }
                else if (kotlin) {
                    arguments.add("value" + i + ": " + colType + "?");
                    calls.add("this.value" + i + "(value" + i + ")");
                }
                else {
                    final String nullableAnnotation = nullableOrNonnullAnnotation(out, column);

                    arguments.add((nullableAnnotation == null ? "" : "@" + nullableAnnotation + " ") + colType + " value" + i);
                    calls.add("value" + i + "(value" + i + ");");
                }
            }

            if (scala) {
                out.println();
                out.println("override def values([[%s]]): %s = {", arguments, className);

                for (String call : calls)
                    out.println(call);

                out.println("this");
                out.println("}");
            }
            else if (kotlin) {
                out.println();
                out.println("override fun values([[%s]]): %s {", arguments, className);

                for (String call : calls)
                    out.println(call);

                out.println("return this");
                out.println("}");
            }
            else {
                out.overrideInherit();
                out.println("public %s values([[%s]]) {", className, arguments);

                for (String call : calls)
                    out.println(call);

                out.println("return this;");
                out.println("}");
            }
        }

        if (generateInterfaces())
            printFromAndInto(out, tableUdtOrEmbeddable, Mode.RECORD);


        if (scala) {}
        else if (kotlin) {
            if (tableUdtOrEmbeddable instanceof EmbeddableDefinition) {
                out.println();
                out.println("private companion object {");
                out.println("val FIELDS: Array<%s<*>> = arrayOf(", Field.class);

                String separator = "  ";
                for (EmbeddableColumnDefinition column : ((EmbeddableDefinition) tableUdtOrEmbeddable).getColumns()) {
                    final String colIdentifier = out.ref(getStrategy().getFullJavaIdentifier(column.getColumn()), colRefSegments(column));

                    out.println("%s%s.field(%s.name(\"%s\"), %s.dataType)", separator, DSL.class, DSL.class, column.getOutputName(), colIdentifier);
                    separator = ", ";
                }

                out.println(")");
                out.println("}");
            }
        }
        else {
            out.header("Constructors");

            if (tableUdtOrEmbeddable instanceof EmbeddableDefinition) {
                out.println();
                out.println("private static final %s<?>[] FIELDS = {", Field.class);

                for (EmbeddableColumnDefinition column : ((EmbeddableDefinition) tableUdtOrEmbeddable).getColumns()) {
                    final String colIdentifier = out.ref(getStrategy().getFullJavaIdentifier(column.getColumn()), colRefSegments(column));

                    out.println("%s.field(%s.name(\"%s\"), %s.getDataType()),", DSL.class, DSL.class, column.getOutputName(), colIdentifier);
                }

                out.println("};");
                out.println();
            }

            out.javadoc("Create a detached %s", className);

            out.println("public %s() {", className);
            if (tableUdtOrEmbeddable instanceof EmbeddableDefinition)
                out.println("super(FIELDS);");
            else
                out.println("super(%s);", tableIdentifier);
            out.println("}");
        }

        // [#3130] Invalid UDTs may have a degree of 0
        // [#3176] Avoid generating constructors for tables with more than 255 columns (Java's method argument limit)
        if (degree > 0 && degree < 256) {
            List<String> arguments = new ArrayList<>(degree);
            List<String> properties = new ArrayList<>(degree);

            for (int i = 0; i < degree; i++) {
                final TypedElementDefinition<?> column = columns.get(i);
                final String columnMember = getStrategy().getJavaMemberName(column, Mode.DEFAULT);
                final String type = out.ref(getJavaType(column.getType(resolver())));

                if (scala) {
                    arguments.add(columnMember + " : " + type);
                }
                else if (kotlin) {
                    arguments.add(columnMember + ": " + type + "? = null");
                }
                else {
                    final String nullableAnnotation = nullableOrNonnullAnnotation(out, column);

                    arguments.add((nullableAnnotation == null ? "" : "@" + nullableAnnotation + " ") + type + " " + columnMember);
                }

                properties.add("\"" + escapeString(columnMember) + "\"");
            }

            out.javadoc("Create a detached, initialised %s", className);

            if (scala) {
                out.println("def this([[%s]]) = {", arguments);
                out.println("this()", tableIdentifier);
                out.println();
            }
            else if (kotlin) {
                out.println("constructor([[%s]]): this() {", arguments);
            }
            else {
                if (generateConstructorPropertiesAnnotationOnRecords())
                    out.println("@%s({ [[%s]] })", ConstructorProperties.class, properties);

                out.println("public %s([[%s]]) {", className, arguments);

                if (tableUdtOrEmbeddable instanceof EmbeddableDefinition)
                    out.println("this();", tableIdentifier);
                else
                    out.println("super(%s);", tableIdentifier);

                out.println();
            }

            for (int i = 0; i < degree; i++) {
                final TypedElementDefinition<?> column = columns.get(i);
                final String columnMember = getStrategy().getJavaMemberName(column, Mode.DEFAULT);

                if (scala || kotlin)
                    out.println("set(%s, %s)", i, columnMember);
                else
                    out.println("set(%s, %s);", i, columnMember);
            }

            out.println("}");
        }

        if (tableUdtOrEmbeddable instanceof TableDefinition)
            generateRecordClassFooter((TableDefinition) tableUdtOrEmbeddable, out);
        else if (tableUdtOrEmbeddable instanceof EmbeddableDefinition)
            generateEmbeddableClassFooter((EmbeddableDefinition) tableUdtOrEmbeddable, out);
        else
            generateUDTRecordClassFooter((UDTDefinition) tableUdtOrEmbeddable, out);

        out.println("}");
    }

    /**
     * Subclasses may override this method to provide their own record setters.
     */
    protected void generateRecordSetter(TypedElementDefinition<?> column, int index, JavaWriter out) {
        generateRecordSetter0(column, index, out);
    }

    /**
     * Subclasses may override this method to provide their own embeddable setters.
     */
    protected void generateEmbeddableSetter(TypedElementDefinition<?> column, int index, JavaWriter out) {
        generateRecordSetter0(column, index, out);
    }

    /**
     * Subclasses may override this method to provide their own record setters.
     */
    protected void generateUDTRecordSetter(TypedElementDefinition<?> column, int index, JavaWriter out) {
        generateRecordSetter0(column, index, out);
    }

    private final void generateRecordSetter0(TypedElementDefinition<?> column, int index, JavaWriter out) {
        final String className = getStrategy().getJavaClassName(column.getContainer(), Mode.RECORD);
        final String setterReturnType = generateFluentSetters() ? className : tokenVoid;
        final String setter = getStrategy().getJavaSetterName(column, Mode.RECORD);
        final String typeFull = getJavaType(column.getType(resolver()));
        final String type = out.ref(typeFull);
        final String name = column.getQualifiedOutputName();
        final boolean isUDT = column.getType(resolver()).isUDT();
        final boolean isArray = column.getType(resolver()).isArray();
        final boolean isUDTArray = column.getType(resolver()).isArray() && database.getArray(column.getType(resolver()).getSchema(), column.getType(resolver()).getQualifiedUserType()).getElementType(resolver()).isUDT();
        boolean override = generateInterfaces() && !generateImmutableInterfaces() && !isUDT;

        // We cannot have covariant setters for arrays because of type erasure
        if (!(generateInterfaces() && isArray)) {
            if (!printDeprecationIfUnknownType(out, typeFull))
                out.javadoc("Setter for <code>%s</code>.[[before= ][%s]]", name, list(escapeEntities(comment(column))));

            if (scala) {
                out.println("def %s(value: %s): %s = {", setter, type, setterReturnType);
                out.println("set(%s, value)", index);

                if (generateFluentSetters())
                    out.println("this");

                out.println("}");
            }
            else if (kotlin) {
                out.println("%sfun %s(value: %s?): %s {", (override ? "override " : ""), setter, type, setterReturnType);
                out.println("set(%s, value)", index);

                if (generateFluentSetters())
                    out.println("return this");

                out.println("}");
            }
            else {
                final String nullableAnnotation = nullableOrNonnullAnnotation(out, column);

                out.overrideIf(override);
                out.println("public %s %s([[before=@][after= ][%s]]%s value) {", setterReturnType, setter, list(nullableAnnotation), varargsIfArray(type));
                out.println("set(%s, value);", index);

                if (generateFluentSetters())
                    out.println("return this;");

                out.println("}");
            }
        }

        // [#3117] Avoid covariant setters for UDTs when generating interfaces
        if (generateInterfaces() && !generateImmutableInterfaces() && (isUDT || isArray)) {
            final String columnTypeFull = getJavaType(column.getType(resolver(Mode.RECORD)), Mode.RECORD);
            final String columnType = out.ref(columnTypeFull);
            final String columnTypeInterface = out.ref(getJavaType(column.getType(resolver(Mode.INTERFACE)), Mode.INTERFACE));

            if (!printDeprecationIfUnknownType(out, columnTypeFull))
                out.javadoc("Setter for <code>%s</code>.[[before= ][%s]]", name, list(escapeEntities(comment(column))));

            out.override();

            if (scala) {
                // [#3082] TODO Handle <interfaces/> + ARRAY also for Scala

                out.println("def %s(value: %s): %s = {", setter, columnTypeInterface, setterReturnType);
                out.println("if (value == null)");
                out.println("set(%s, null)", index);
                out.println("else");
                out.println("set(%s, value.into(new %s()))", index, type);

                if (generateFluentSetters())
                    out.println("this");

                out.println("}");
            }
            else if (kotlin) {
                // TODO
            }
            else {
                final String nullableAnnotation = nullableOrNonnullAnnotation(out, column);

                out.println("public %s %s([[before=@][after= ][%s]]%s value) {", setterReturnType, setter, list(nullableAnnotation), varargsIfArray(columnTypeInterface));
                out.println("if (value == null)");
                out.println("set(%s, null);", index);

                if (isUDT) {
                    out.println("else");
                    out.println("set(%s, value.into(new %s()));", index, type);
                }
                else if (isArray) {
                    final ArrayDefinition array = database.getArray(column.getType(resolver()).getSchema(), column.getType(resolver()).getQualifiedUserType());
                    final String componentType = out.ref(getJavaType(array.getElementType(resolver(Mode.RECORD)), Mode.RECORD));
                    final String componentTypeInterface = out.ref(getJavaType(array.getElementType(resolver(Mode.INTERFACE)), Mode.INTERFACE));

                    out.println("else {");
                    out.println("%s a = new %s();", columnType, columnType);
                    out.println();
                    out.println("for (%s i : value)", componentTypeInterface);

                    if (isUDTArray)
                        out.println("a.add(i.into(new %s()));", componentType);
                    else
                        out.println("a.add(i);", componentType);

                    out.println();
                    out.println("set(1, a);");
                    out.println("}");
                }

                if (generateFluentSetters())
                    out.println("return this;");

                out.println("}");
            }
        }
    }

    /**
     * Subclasses may override this method to provide their own record getters.
     */
    protected void generateRecordGetter(TypedElementDefinition<?> column, int index, JavaWriter out) {
        generateRecordGetter0(column, index, out);
    }

    /**
     * Subclasses may override this method to provide their own embeddable getters.
     */
    protected void generateEmbeddableGetter(TypedElementDefinition<?> column, int index, JavaWriter out) {
        generateRecordGetter0(column, index, out);
    }

    /**
     * Subclasses may override this method to provide their own record getters.
     */
    protected void generateUDTRecordGetter(TypedElementDefinition<?> column, int index, JavaWriter out) {
        generateRecordGetter0(column, index, out);
    }

    private final void generateRecordGetter0(TypedElementDefinition<?> column, int index, JavaWriter out) {
        final String getter = getStrategy().getJavaGetterName(column, Mode.RECORD);
        final String typeFull = getJavaType(column.getType(resolver()));
        final String type = out.ref(typeFull);
        final String name = column.getQualifiedOutputName();

        if (!printDeprecationIfUnknownType(out, typeFull))
            out.javadoc("Getter for <code>%s</code>.[[before= ][%s]]", name, list(escapeEntities(comment(column))));

        if (column instanceof ColumnDefinition)
            printColumnJPAAnnotation(out, (ColumnDefinition) column);
        printValidationAnnotation(out, column);
        printNullableOrNonnullAnnotation(out, column);
        boolean override = generateInterfaces();

        if (scala) {
            out.println("def %s: %s = {", getter, type);
            out.println("val r = get(%s)", index);
            out.println("if (r == null)");
            out.println("null");
            out.println("else");
            out.println("r.asInstanceOf[%s]", type);
            out.println("}");
        }
        else if (kotlin) {
            out.println("%sfun %s(): %s? = get(%s) as %s?", (override ? "override " : ""), getter, type, index, type);
        }
        else {
            out.overrideIf(override);
            out.println("public %s %s() {", type, getter);

            // [#6705] Avoid generating code with a redundant (Object) cast
            if (Object.class.getName().equals(typeFull))
                out.println("return get(%s);", index);
            else
                out.println("return (%s) get(%s);", type, index);

            out.println("}");
        }
    }

    private int colRefSegments(TypedElementDefinition<?> column) {
        if (column != null && column.getContainer() instanceof UDTDefinition)
            return 2;

        if (!getStrategy().getInstanceFields())
            return 2;

        return 3;
    }

    /**
     * Subclasses may override this method to provide record class footer code.
     */
    @SuppressWarnings("unused")
    protected void generateRecordClassFooter(TableDefinition table, JavaWriter out) {}

    /**
     * Subclasses may override this method to provide their own Javadoc.
     */
    protected void generateRecordClassJavadoc(TableDefinition table, JavaWriter out) {
        if (generateCommentsOnTables())
            printClassJavadoc(out, table);
        else
            printClassJavadoc(out, "The table <code>" + table.getQualifiedInputName() + "</code>.");
    }

    /**
     * Subclasses may override this method to provide record class footer code.
     */
    @SuppressWarnings("unused")
    protected void generateEmbeddableClassFooter(EmbeddableDefinition embeddable, JavaWriter out) {}

    /**
     * Subclasses may override this method to provide their own Javadoc.
     */
    protected void generateEmbeddableClassJavadoc(EmbeddableDefinition embeddable, JavaWriter out) {
        printClassJavadoc(out, "The embeddable <code>" + embeddable.getQualifiedInputName() + "</code>.");
    }

    private String refRowType(JavaWriter out, Collection<? extends TypedElementDefinition<?>> columns) {
        StringBuilder result = new StringBuilder();
        String separator = "";

        for (TypedElementDefinition<?> column : columns) {
            result.append(separator);
            result.append(out.ref(getJavaType(column.getType(resolver()))));

            if (kotlin)
                result.append("?");

            separator = ", ";
        }

        return result.toString();
    }

    protected void generateInterfaces(SchemaDefinition schema) {
        log.info("Generating table interfaces");

        for (TableDefinition table : database.getTables(schema)) {
            try {
                generateInterface(table);
            } catch (Exception e) {
                log.error("Error while generating table interface " + table, e);
            }
        }

        watch.splitInfo("Table interfaces generated");
    }

    protected void generateInterface(TableDefinition table) {
        JavaWriter out = newJavaWriter(getFile(table, Mode.INTERFACE));
        log.info("Generating interface", out.file().getName());
        generateInterface(table, out);
        closeJavaWriter(out);
    }

    protected void generateUDTInterface(UDTDefinition udt) {
        JavaWriter out = newJavaWriter(getFile(udt, Mode.INTERFACE));
        log.info("Generating interface", out.file().getName());
        generateInterface0(udt, out);
        closeJavaWriter(out);
    }

    protected void generateInterface(TableDefinition table, JavaWriter out) {
        generateInterface0(table, out);
    }

    protected void generateUDTInterface(UDTDefinition udt, JavaWriter out) {
        generateInterface0(udt, out);
    }

    private final void generateInterface0(Definition tableOrUDT, JavaWriter out) {
        final String className = getStrategy().getJavaClassName(tableOrUDT, Mode.INTERFACE);
        final List<String> interfaces = out.ref(getStrategy().getJavaClassImplements(tableOrUDT, Mode.INTERFACE));

        printPackage(out, tableOrUDT, Mode.INTERFACE);
        if (tableOrUDT instanceof TableDefinition)
            generateInterfaceClassJavadoc((TableDefinition) tableOrUDT, out);
        else
            generateUDTInterfaceClassJavadoc((UDTDefinition) tableOrUDT, out);

        printClassAnnotations(out, tableOrUDT, Mode.INTERFACE);

        if (tableOrUDT instanceof TableDefinition)
            printTableJPAAnnotation(out, (TableDefinition) tableOrUDT);

        if (scala)
            out.println("trait %s[[before= extends ][%s]] {", className, interfaces);
        else if (kotlin)
            out.println("interface %s[[before= : ][%s]] {", className, interfaces);
        else
            out.println("public interface %s[[before= extends ][%s]] {", className, interfaces);

        List<? extends TypedElementDefinition<?>> typedElements = getTypedElements(tableOrUDT);
        for (int i = 0; i < typedElements.size(); i++) {
            TypedElementDefinition<?> column = typedElements.get(i);

            if (kotlin) {

                // TODO: The Mode should be INTERFACE
                final String member = getStrategy().getJavaMemberName(column, Mode.POJO);
                final String typeFull = getJavaType(column.getType(resolver(Mode.INTERFACE)), Mode.INTERFACE);
                final String type = out.ref(typeFull);

                out.println("%s %s: %s?", (generateImmutableInterfaces() ? "val" : "var"), member, type);
            }
            else {
                if (!generateImmutableInterfaces())
                    if (tableOrUDT instanceof TableDefinition)
                        generateInterfaceSetter(column, i, out);
                    else
                        generateUDTInterfaceSetter(column, i, out);

                if (tableOrUDT instanceof TableDefinition)
                    generateInterfaceGetter(column, i, out);
                else
                    generateUDTInterfaceGetter(column, i, out);
            }
        }

        if (!generateImmutableInterfaces()) {
            String local = getStrategy().getJavaClassName(tableOrUDT, Mode.INTERFACE);
            String qualified = out.ref(getStrategy().getFullJavaClassName(tableOrUDT, Mode.INTERFACE));

            out.header("FROM and INTO");

            out.javadoc("Load data from another generated Record/POJO implementing the common interface %s", local);

            if (scala)
                out.println("def from(from: %s)", qualified);
            else if (kotlin)
                out.println("fun from(from: %s)", qualified);
            else
                out.println("public void from(%s from);", qualified);

            // [#10191] Java and Kotlin can produce overloads for this method despite
            // generic type erasure, but Scala cannot, see
            // https://twitter.com/lukaseder/status/1262652304773259264
            if (scala) {}
            else {
                out.javadoc("Copy data into another generated Record/POJO implementing the common interface %s", local);

                if (kotlin)
                    out.println("fun <E : %s> into(into: E): E", qualified);
                else
                    out.println("public <E extends %s> E into(E into);", qualified);
            }
        }


        if (tableOrUDT instanceof TableDefinition)
            generateInterfaceClassFooter((TableDefinition) tableOrUDT, out);
        else
            generateUDTInterfaceClassFooter((UDTDefinition) tableOrUDT, out);

        out.println("}");
    }

    /**
     * Subclasses may override this method to provide their own interface setters.
     */
    protected void generateInterfaceSetter(TypedElementDefinition<?> column, int index, JavaWriter out) {
        generateInterfaceSetter0(column, index, out);
    }

    /**
     * Subclasses may override this method to provide their own interface setters.
     */
    protected void generateUDTInterfaceSetter(TypedElementDefinition<?> column, int index, JavaWriter out) {
        generateInterfaceSetter0(column, index, out);
    }

    private final void generateInterfaceSetter0(TypedElementDefinition<?> column, @SuppressWarnings("unused") int index, JavaWriter out) {
        final String className = getStrategy().getJavaClassName(column.getContainer(), Mode.INTERFACE);
        final String setterReturnType = generateFluentSetters() ? className : tokenVoid;
        final String setter = getStrategy().getJavaSetterName(column, Mode.INTERFACE);
        final String typeFull = getJavaType(column.getType(resolver(Mode.INTERFACE)), Mode.INTERFACE);
        final String type = out.ref(typeFull);
        final String name = column.getQualifiedOutputName();

        if (!printDeprecationIfUnknownType(out, typeFull))
            out.javadoc("Setter for <code>%s</code>.[[before= ][%s]]", name, list(escapeEntities(comment(column))));

        if (scala) {
            out.println("def %s(value: %s): %s", setter, type, setterReturnType);
        }
        else if (kotlin) {
            out.println("fun %s(value: %s?): %s", setter, type, setterReturnType);
        }
        else {
            final String nullableAnnotation = nullableOrNonnullAnnotation(out, column);

            out.println("public %s %s([[before=@][after= ][%s]]%s value);", setterReturnType, setter, list(nullableAnnotation), varargsIfArray(type));
        }
    }

    /**
     * Subclasses may override this method to provide their own interface getters.
     */
    protected void generateInterfaceGetter(TypedElementDefinition<?> column, int index, JavaWriter out) {
        generateInterfaceGetter0(column, index, out);
    }

    /**
     * Subclasses may override this method to provide their own interface getters.
     */
    protected void generateUDTInterfaceGetter(TypedElementDefinition<?> column, int index, JavaWriter out) {
        generateInterfaceGetter0(column, index, out);
    }

    private final void generateInterfaceGetter0(TypedElementDefinition<?> column, @SuppressWarnings("unused") int index, JavaWriter out) {
        final String getter = getStrategy().getJavaGetterName(column, Mode.INTERFACE);
        final String typeFull = getJavaType(column.getType(resolver(Mode.INTERFACE)), Mode.INTERFACE);
        final String type = out.ref(typeFull);
        final String name = column.getQualifiedOutputName();

        if (!printDeprecationIfUnknownType(out, typeFull))
            out.javadoc("Getter for <code>%s</code>.[[before= ][%s]]", name, list(escapeEntities(comment(column))));

        if (column instanceof ColumnDefinition)
            printColumnJPAAnnotation(out, (ColumnDefinition) column);

        printValidationAnnotation(out, column);
        printNullableOrNonnullAnnotation(out, column);

        if (scala)
            out.println("def %s: %s", getter, type);
        else if (kotlin)
            out.println("fun %s(): %s?", getter, type);
        else
            out.println("public %s %s();", type, getter);
    }

    /**
     * Subclasses may override this method to provide interface class footer
     * code.
     */
    @SuppressWarnings("unused")
    protected void generateInterfaceClassFooter(TableDefinition table, JavaWriter out) {}

    /**
     * Subclasses may override this method to provide their own Javadoc.
     */
    protected void generateInterfaceClassJavadoc(TableDefinition table, JavaWriter out) {
        if (generateCommentsOnTables())
            printClassJavadoc(out, table);
        else
            printClassJavadoc(out, "The table <code>" + table.getQualifiedInputName() + "</code>.");
    }

    protected void generateUDTs(SchemaDefinition schema) {
        log.info("Generating UDTs");

        for (UDTDefinition udt : database.getUDTs(schema)) {
            try {
                generateUDT(schema, udt);
            } catch (Exception e) {
                log.error("Error while generating udt " + udt, e);
            }
        }

        watch.splitInfo("UDTs generated");
    }

    @SuppressWarnings("unused")
    protected void generateUDT(SchemaDefinition schema, UDTDefinition udt) {
        JavaWriter out = newJavaWriter(getFile(udt));
        log.info("Generating UDT ", out.file().getName());
        generateUDT(udt, out);
        closeJavaWriter(out);
    }

    protected void generateUDT(UDTDefinition udt, JavaWriter out) {
        final SchemaDefinition schema = udt.getSchema();
        final PackageDefinition pkg = udt.getPackage();
        final boolean synthetic = udt.isSynthetic();
        final String className = getStrategy().getJavaClassName(udt);
        final String recordType = out.ref(getStrategy().getFullJavaClassName(udt, Mode.RECORD));
        final List<String> interfaces = out.ref(getStrategy().getJavaClassImplements(udt, Mode.DEFAULT));
        final String schemaId = out.ref(getStrategy().getFullJavaIdentifier(schema), 2);
        final String packageId = pkg == null ? null : out.ref(getStrategy().getFullJavaIdentifier(pkg), 2);
        final String udtId = out.ref(getStrategy().getJavaIdentifier(udt), 2);

        printPackage(out, udt);

        if (scala) {
            out.println("object %s {", className);
            printSingletonInstance(out, udt);

            for (AttributeDefinition attribute : udt.getAttributes()) {
                final String attrId = out.ref(getStrategy().getJavaIdentifier(attribute), 2);

                out.javadoc("The attribute <code>%s</code>.[[before= ][%s]]", attribute.getQualifiedOutputName(), list(escapeEntities(comment(attribute))));
                out.println("val %s = %s.%s", attrId, udtId, attrId);
            }

            out.println("}");
            out.println();
        }

        generateUDTClassJavadoc(udt, out);
        printClassAnnotations(out, udt, Mode.DEFAULT);







        if (scala) {
            out.println("class %s extends %s[%s](\"%s\", null, %s, %s)[[before= with ][separator= with ][%s]] {", className, UDTImpl.class, recordType, udt.getOutputName(), packageId, synthetic, interfaces);
        }
        else {
            out.println("public class %s extends %s<%s>[[before= implements ][%s]] {", className, UDTImpl.class, recordType, interfaces);
            out.printSerial();
            printSingletonInstance(out, udt);
        }

        printRecordTypeMethod(out, udt);

        for (AttributeDefinition attribute : udt.getAttributes()) {
            final String attrTypeFull = getJavaType(attribute.getType(resolver()));
            final String attrType = out.ref(attrTypeFull);
            final String attrTypeRef = getJavaTypeReference(attribute.getDatabase(), attribute.getType(resolver()));
            final String attrId = out.ref(getStrategy().getJavaIdentifier(attribute), 2);
            final String attrName = attribute.getName();
            final List<String> converter = out.ref(list(attribute.getType(resolver()).getConverter()));
            final List<String> binding = out.ref(list(attribute.getType(resolver()).getBinding()));

            if (scala) {
                printDeprecationIfUnknownType(out, attrTypeFull);
                out.println("private val %s: %s[%s, %s] = %s.createField(%s.name(\"%s\"), %s, this, \"%s\"" + converterTemplate(converter) + converterTemplate(binding) + ")",
                    attrId, UDTField.class, recordType, attrType, UDTImpl.class, DSL.class, attrName, attrTypeRef, escapeString(""), converter, binding);
            }
            else {
                if (!printDeprecationIfUnknownType(out, attrTypeFull))
                    out.javadoc("The attribute <code>%s</code>.[[before= ][%s]]", attribute.getQualifiedOutputName(), list(escapeEntities(comment(attribute))));

                out.println("public static final %s<%s, %s> %s = createField(%s.name(\"%s\"), %s, %s, \"%s\"" + converterTemplate(converter) + converterTemplate(binding) + ");",
                    UDTField.class, recordType, attrType, attrId, DSL.class, attrName, attrTypeRef, udtId, escapeString(""), converter, binding);
            }
        }

        // [#799] Oracle UDT's can have member procedures
        for (RoutineDefinition routine : udt.getRoutines()) {
            try {
                if (!routine.isSQLUsable()) {

                    // Static execute() convenience method
                    printConvenienceMethodProcedure(out, routine, false);
                }
                else {

                    // Static execute() convenience method
                    if (!routine.isAggregate())
                        printConvenienceMethodFunction(out, routine, false);

                    // Static asField() convenience method
                    printConvenienceMethodFunctionAsField(out, routine, false);
                    printConvenienceMethodFunctionAsField(out, routine, true);
                }

            } catch (Exception e) {
                log.error("Error while generating routine " + routine, e);
            }
        }

        if (scala) {
        }
        else {
            out.javadoc(NO_FURTHER_INSTANCES_ALLOWED);
            out.println("private %s() {", className);
            out.println("super(\"%s\", null, %s, %s);", udt.getOutputName(), packageId, synthetic);
            out.println("}");
        }

        if (scala) {
            out.println();
            out.println("override def getSchema: %s = %s", Schema.class, schemaId);
        }
        else {
            out.overrideInherit();
            out.println("public %s getSchema() {", Schema.class);
            out.println("return %s != null ? %s : new %s(%s.name(\"%s\"));", schemaId, schemaId, SchemaImpl.class, DSL.class, schema.getOutputName());
            out.println("}");
        }

        generateUDTClassFooter(udt, out);
        out.println("}");
        closeJavaWriter(out);
    }

    /**
     * Subclasses may override this method to provide udt class footer code.
     */
    @SuppressWarnings("unused")
    protected void generateUDTClassFooter(UDTDefinition udt, JavaWriter out) {}

    /**
     * Subclasses may override this method to provide their own Javadoc.
     */
    protected void generateUDTClassJavadoc(UDTDefinition udt, JavaWriter out) {
        if (generateCommentsOnUDTs())
            printClassJavadoc(out, udt);
        else
            printClassJavadoc(out, "The udt <code>" + udt.getQualifiedInputName() + "</code>.");
    }

    protected void generateUDTPojos(SchemaDefinition schema) {
        log.info("Generating UDT POJOs");

        for (UDTDefinition udt : database.getUDTs(schema)) {
            try {
                generateUDTPojo(udt);
            }
            catch (Exception e) {
                log.error("Error while generating UDT POJO " + udt, e);
            }
        }

        watch.splitInfo("UDT POJOs generated");
    }

    /**
     * Subclasses may override this method to provide UDT POJO class footer code.
     */
    @SuppressWarnings("unused")
    protected void generateUDTPojoClassFooter(UDTDefinition udt, JavaWriter out) {}

    /**
     * Subclasses may override this method to provide their own Javadoc.
     */
    protected void generateUDTPojoClassJavadoc(UDTDefinition udt, JavaWriter out) {
        if (generateCommentsOnUDTs())
            printClassJavadoc(out, udt);
        else
            printClassJavadoc(out, "The udt <code>" + udt.getQualifiedInputName() + "</code>.");
    }

    protected void generateUDTInterfaces(SchemaDefinition schema) {
        log.info("Generating UDT interfaces");

        for (UDTDefinition udt : database.getUDTs(schema)) {
            try {
                generateUDTInterface(udt);
            } catch (Exception e) {
                log.error("Error while generating UDT interface " + udt, e);
            }
        }

        watch.splitInfo("UDT interfaces generated");
    }

    /**
     * Subclasses may override this method to provide UDT interface class footer code.
     */
    @SuppressWarnings("unused")
    protected void generateUDTInterfaceClassFooter(UDTDefinition udt, JavaWriter out) {}

    /**
     * Subclasses may override this method to provide their own Javadoc.
     */
    protected void generateUDTInterfaceClassJavadoc(UDTDefinition udt, JavaWriter out) {
        if (generateCommentsOnUDTs())
            printClassJavadoc(out, udt);
        else
            printClassJavadoc(out, "The udt <code>" + udt.getQualifiedInputName() + "</code>.");
    }

    /**
     * Generating UDT record classes
     */
    protected void generateUDTRecords(SchemaDefinition schema) {
        log.info("Generating UDT records");

        for (UDTDefinition udt : database.getUDTs(schema)) {
            try {
                generateUDTRecord(udt);
            } catch (Exception e) {
                log.error("Error while generating UDT record " + udt, e);
            }
        }

        watch.splitInfo("UDT records generated");
    }

    /**
     * Subclasses may override this method to provide udt record class footer code.
     */
    @SuppressWarnings("unused")
    protected void generateUDTRecordClassFooter(UDTDefinition udt, JavaWriter out) {}

    /**
     * Subclasses may override this method to provide their own Javadoc.
     */
    protected void generateUDTRecordClassJavadoc(UDTDefinition udt, JavaWriter out) {
        if (generateCommentsOnUDTs())
            printClassJavadoc(out, udt);
        else
            printClassJavadoc(out, "The udt <code>" + udt.getQualifiedInputName() + "</code>.");
    }

    protected void generateUDTRoutines(SchemaDefinition schema) {
        for (UDTDefinition udt : database.getUDTs(schema)) {
            if (udt.getRoutines().size() > 0) {
                try {
                    log.info("Generating member routines");

                    for (RoutineDefinition routine : udt.getRoutines()) {
                        try {
                            generateRoutine(schema, routine);
                        } catch (Exception e) {
                            log.error("Error while generating member routines " + routine, e);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error while generating UDT " + udt, e);
                }

                watch.splitInfo("Member procedures routines");
            }
        }
    }

    /**
     * Generating central static udt access
     */
    protected void generateUDTReferences(SchemaDefinition schema) {
        log.info("Generating UDT references");
        JavaWriter out = newJavaWriter(new File(getFile(schema).getParentFile(), "UDTs.java"));

        printPackage(out, schema);
        printClassJavadoc(out, "Convenience access to all UDTs in " + schemaNameOrDefault(schema) + ".");
        printClassAnnotations(out, schema, Mode.DEFAULT);

        if (scala)
            out.println("object UDTs {");
        else
            out.println("public class UDTs {");

        for (UDTDefinition udt : database.getUDTs(schema)) {
            final String className = out.ref(getStrategy().getFullJavaClassName(udt));
            final String id = getStrategy().getJavaIdentifier(udt);
            final String fullId = getStrategy().getFullJavaIdentifier(udt);

            out.javadoc("The type <code>%s</code>", udt.getQualifiedOutputName());

            if (scala)
                out.println("val %s = %s", id, fullId);
            else
                out.println("public static final %s %s = %s;", className, id, fullId);
        }

        out.println("}");
        closeJavaWriter(out);

        watch.splitInfo("UDT references generated");
    }

    /**
     * Generating central static domain access
     */
    protected void generateDomainReferences(SchemaDefinition schema) {
        log.info("Generating DOMAIN references");
        JavaWriter out = newJavaWriter(new File(getFile(schema).getParentFile(), "Domains.java"));

        printPackage(out, schema);
        printClassJavadoc(out, "Convenience access to all Domains in " + schemaNameOrDefault(schema) + ".");
        printClassAnnotations(out, schema, Mode.DOMAIN);

        if (scala || kotlin)
            out.println("object Domains {");
        else
            out.println("public class Domains {");

        for (DomainDefinition domain : database.getDomains(schema)) {
            final String id = getStrategy().getJavaIdentifier(domain);
            final String schemaId = out.ref(getStrategy().getFullJavaIdentifier(schema), 2);
            final String domainTypeFull = getJavaType(domain.getType(resolver()));
            final String domainType = out.ref(domainTypeFull);
            final String domainTypeRef = getJavaTypeReference(domain.getDatabase(), domain.getType(resolver()));

            out.javadoc("The domain <code>%s</code>.", domain.getQualifiedOutputName());

            if (scala) {
                out.println("val %s: %s[%s] = %s.createDomain(", id, Domain.class, domainType, Internal.class);
                out.println("  %s", schemaId);
                out.println(", %s.name(\"%s\")", DSL.class, escapeString(domain.getOutputName()));
                out.println(", %s", domainTypeRef);

                for (String check : domain.getCheckClauses())
                    out.println(", %s.createCheck(null, null, \"%s\")", Internal.class, escapeString(check));

                out.println(")");
            }
            else if (kotlin) {
                out.println("val %s: %s<%s> = %s.createDomain(", id, Domain.class, domainType, Internal.class);
                out.println("  %s", schemaId);
                out.println(", %s.name(\"%s\")", DSL.class, escapeString(domain.getOutputName()));
                out.println(", %s", domainTypeRef);

                for (String check : domain.getCheckClauses())
                    out.println(", %s.createCheck<%s>(null, null, \"%s\")", Internal.class, Record.class, escapeString(check));

                out.println(")");
            }
            else {
                out.println("public static final %s<%s> %s = %s.createDomain(", Domain.class, domainType, id, Internal.class);
                out.println("  %s", schemaId);
                out.println(", %s.name(\"%s\")", DSL.class, escapeString(domain.getOutputName()));
                out.println(", %s", domainTypeRef);

                for (String check : domain.getCheckClauses())
                    out.println(", %s.createCheck(null, null, \"%s\")", Internal.class, escapeString(check));

                out.println(");");
            }
        }

        out.println("}");
        closeJavaWriter(out);

        watch.splitInfo("DOMAIN references generated");
    }

    protected void generateArrays(SchemaDefinition schema) {
        log.info("Generating ARRAYs");

        for (ArrayDefinition array : database.getArrays(schema)) {
            try {
                generateArray(schema, array);
            } catch (Exception e) {
                log.error("Error while generating ARRAY record " + array, e);
            }
        }

        watch.splitInfo("ARRAYs generated");
    }

    @SuppressWarnings("unused")
    protected void generateArray(SchemaDefinition schema, ArrayDefinition array) {
        JavaWriter out = newJavaWriter(getFile(array, Mode.RECORD));
        log.info("Generating ARRAY", out.file().getName());
        generateArray(array, out);
        closeJavaWriter(out);
    }


    protected void generateArray(ArrayDefinition array, JavaWriter out) {


































































































    }

    /**
     * Subclasses may override this method to provide array class footer code.
     */
    @SuppressWarnings("unused")
    protected void generateArrayClassFooter(ArrayDefinition array, JavaWriter out) {}

    /**
     * Subclasses may override this method to provide their own Javadoc.
     */
    protected void generateArrayClassJavadoc(ArrayDefinition array, JavaWriter out) {
        if (generateCommentsOnUDTs())
            printClassJavadoc(out, array);
        else
            printClassJavadoc(out, "The type <code>" + array.getQualifiedInputName() + "</code>.");
    }

    protected void generateEnums(SchemaDefinition schema) {
        log.info("Generating ENUMs");

        for (EnumDefinition e : database.getEnums(schema)) {
            try {
                generateEnum(e);
            } catch (Exception ex) {
                log.error("Error while generating enum " + e, ex);
            }
        }

        watch.splitInfo("Enums generated");
    }

    /**
     * @deprecated - [#681] - 3.14.0 - This method is no longer being called
     */
    @Deprecated
    protected void generateDomains(SchemaDefinition schema) {}

    protected void generateEnum(EnumDefinition e) {
        JavaWriter out = newJavaWriter(getFile(e, Mode.ENUM));
        log.info("Generating ENUM", out.file().getName());
        generateEnum(e, out);
        closeJavaWriter(out);
    }

    protected void generateEnum(EnumDefinition e, JavaWriter out) {
        final String className = getStrategy().getJavaClassName(e, Mode.ENUM);
        final List<String> interfaces = out.ref(getStrategy().getJavaClassImplements(e, Mode.ENUM));
        final List<String> literals = e.getLiterals();
        final List<String> identifiers = new ArrayList<>(literals.size());

        for (int i = 0; i < literals.size(); i++) {
            String identifier = convertToIdentifier(literals.get(i), language);

            // [#2781] Disambiguate collisions with the leading package name
            if (identifier.equals(getStrategy().getJavaPackageName(e).replaceAll("\\..*", "")))
                identifier += "_";

            identifiers.add(identifier);
        }

        printPackage(out, e);
        generateEnumClassJavadoc(e, out);
        printClassAnnotations(out, e, Mode.ENUM);


        boolean enumHasNoSchema = e.isSynthetic() || !(e.getDatabase() instanceof PostgresDatabase);
        if (scala) {
            out.println("object %s {", className);
            out.println();

            for (int i = 0; i < identifiers.size(); i++)
                out.println("val %s: %s = %s.%s", identifiers.get(i), className, getStrategy().getJavaPackageName(e), identifiers.get(i));

            out.println();
            out.println("def values: %s[%s] = %s(",
                out.ref("scala.Array"),
                className,
                out.ref("scala.Array"));

            for (int i = 0; i < identifiers.size(); i++) {
                out.print((i > 0 ? ", " : "  "));
                out.println(identifiers.get(i));
            }

            out.println(")");
            out.println();

            out.println("def valueOf(s: %s): %s = s match {", String.class, className);
            for (int i = 0; i < identifiers.size(); i++) {
                out.println("case \"%s\" => %s", literals.get(i), identifiers.get(i));
            }
            out.println("case _ => throw new %s()", IllegalArgumentException.class);
            out.println("}");
            out.println("}");

            out.println();
            out.println("sealed trait %s extends %s[[before= with ][%s]] {", className, EnumType.class, interfaces);

            if (enumHasNoSchema)
                out.println("override def getCatalog: %s = null", Catalog.class);
            else
                out.println("override def getCatalog: %s = if (getSchema == null) null else getSchema().getCatalog()", Catalog.class);

            // [#2135] Only the PostgreSQL database supports schema-scoped enum types
            out.println("override def getSchema: %s = %s",
                Schema.class,
                enumHasNoSchema
                    ? "null"
                    : out.ref(getStrategy().getFullJavaIdentifier(e.getSchema()), 2));
            out.println("override def getName: %s = %s",
                String.class,
                e.isSynthetic() ? "null" : "\"" + escapeString(e.getName()) + "\"");

            generateEnumClassFooter(e, out);
            out.println("}");

            for (int i = 0; i < literals.size(); i++) {
                out.println();
                out.println("case object %s extends %s {", identifiers.get(i), className);
                out.println("override def getLiteral: %s = \"%s\"",
                    String.class,
                    literals.get(i));
                out.println("}");
            }
        }
        else if (kotlin) {
            interfaces.add(out.ref(EnumType.class));
            out.println("enum class %s(@get:JvmName(\"literal\") val literal: String)[[before= : ][%s]] {", className, interfaces);

            for (int i = 0; i < literals.size(); i++)
                out.println("%s(\"%s\")%s", identifiers.get(i), literals.get(i), (i == literals.size() - 1) ? ";" : ",");

            out.println("override fun getCatalog(): %s? = %s",
                Catalog.class, enumHasNoSchema ? "null" : "getSchema() == null ? null : getSchema().getCatalog()");

            // [#2135] Only the PostgreSQL database supports schema-scoped enum types
            out.println("override fun getSchema(): %s? = %s",
                Schema.class, enumHasNoSchema ? "null" : out.ref(getStrategy().getFullJavaIdentifier(e.getSchema()), 2));

            out.println("override fun getName(): %s? = %s",
                String.class, e.isSynthetic() ? "null" : "\"" + escapeString(e.getName()) + "\"");

            out.println("override fun getLiteral(): String = literal");

            generateEnumClassFooter(e, out);
            out.println("}");
        }
        else {
            interfaces.add(out.ref(EnumType.class));
            out.println("public enum %s[[before= implements ][%s]] {", className, interfaces);

            for (int i = 0; i < literals.size(); i++) {
                out.println();
                out.println("%s(\"%s\")%s", identifiers.get(i), literals.get(i), (i == literals.size() - 1) ? ";" : ",");
            }

            out.println();
            out.println("private final %s literal;", String.class);
            out.println();
            out.println("private %s(%s literal) {", className, String.class);
            out.println("this.literal = literal;");
            out.println("}");

            out.overrideInherit();
            out.println("public %s getCatalog() {", Catalog.class);

            if (enumHasNoSchema)
                out.println("return null;");
            else
                out.println("return getSchema() == null ? null : getSchema().getCatalog();");

            out.println("}");

            // [#2135] Only the PostgreSQL database supports schema-scoped enum types
            out.overrideInherit();
            out.println("public %s getSchema() {", Schema.class);
            out.println("return %s;",
                enumHasNoSchema
                    ? "null"
                    : out.ref(getStrategy().getFullJavaIdentifier(e.getSchema()), 2));
            out.println("}");

            out.overrideInherit();
            out.println("public %s getName() {", String.class);
            out.println("return %s;", e.isSynthetic() ? "null" : "\"" + escapeString(e.getName()) + "\"");
            out.println("}");

            out.overrideInherit();
            out.println("public %s getLiteral() {", String.class);
            out.println("return literal;");
            out.println("}");

            generateEnumClassFooter(e, out);
            out.println("}");
        }
    }

    /**
     * Subclasses may override this method to provide enum class footer code.
     */
    @SuppressWarnings("unused")
    protected void generateEnumClassFooter(EnumDefinition e, JavaWriter out) {}

    /**
     * Subclasses may override this method to provide their own Javadoc.
     */
    protected void generateEnumClassJavadoc(EnumDefinition e, JavaWriter out) {
        if (generateCommentsOnUDTs())
            printClassJavadoc(out, e);
        else
            printClassJavadoc(out, "The enum <code>" + e.getQualifiedInputName() + "</code>.");
    }

    /**
     * @deprecated - [#681] - 3.14.0 - This method is no longer being called
     */
    @Deprecated
    protected void generateDomain(DomainDefinition d) {}

    /**
     * @deprecated - [#681] - 3.14.0 - This method is no longer being called
     */
    @Deprecated
    protected void generateDomain(DomainDefinition d, JavaWriter out) {}

    /**
     * @deprecated - [#681] - 3.14.0 - This method is no longer being called
     */
    @Deprecated
    @SuppressWarnings("unused")
    protected void generateDomainClassFooter(DomainDefinition d, JavaWriter out) {}

    /**
     * @deprecated - [#681] - 3.14.0 - This method is no longer being called
     */
    @Deprecated
    protected void generateDomainClassJavadoc(DomainDefinition e, JavaWriter out) {}

    protected void generateRoutines(SchemaDefinition schema) {
        log.info("Generating routines and table-valued functions");

        if (generateGlobalRoutineReferences()) {
            JavaWriter out = newJavaWriter(new File(getFile(schema).getParentFile(), "Routines.java"));
            printPackage(out, schema);

            if (!kotlin) {
                printClassJavadoc(out, "Convenience access to all stored procedures and functions in " + schemaNameOrDefault(schema) + ".");
                printClassAnnotations(out, schema, Mode.DEFAULT);
            }

            if (scala)
                out.println("object Routines {");
            else if (kotlin) {}
            else
                out.println("public class Routines {");

            for (RoutineDefinition routine : database.getRoutines(schema))
                printRoutine(out, routine);

            for (TableDefinition table : database.getTables(schema))
                if (table.isTableValuedFunction())
                    printTableValuedFunction(out, table, getStrategy().getJavaMethodName(table, Mode.DEFAULT));

            if (kotlin) {}
            else
                out.println("}");

            closeJavaWriter(out);
        }

        for (RoutineDefinition routine : database.getRoutines(schema)) {
            try {
                generateRoutine(schema, routine);
            }
            catch (Exception e) {
                log.error("Error while generating routine " + routine, e);
            }
        }

        watch.splitInfo("Routines generated");
    }

    protected void printConstant(JavaWriter out, AttributeDefinition constant) {


























    }

    protected void printRoutine(JavaWriter out, RoutineDefinition routine) {
        if (!routine.isSQLUsable()) {

            // Static execute() convenience method
            printConvenienceMethodProcedure(out, routine, false);
        }
        else {

            // Static execute() convenience method
            // [#457] This doesn't make any sense for user-defined aggregates
            if (!routine.isAggregate())
                printConvenienceMethodFunction(out, routine, false);

            // Static asField() convenience method
            printConvenienceMethodFunctionAsField(out, routine, false);
            printConvenienceMethodFunctionAsField(out, routine, true);
        }
    }

    protected void printTableValuedFunction(JavaWriter out, TableDefinition table, String javaMethodName) {
        printConvenienceMethodTableValuedFunction(out, table, javaMethodName);
        printConvenienceMethodTableValuedFunctionAsField(out, table, false, javaMethodName);
        printConvenienceMethodTableValuedFunctionAsField(out, table, true, javaMethodName);
    }

    protected void generatePackages(SchemaDefinition schema) {













    }

    @SuppressWarnings("unused")
    protected void generatePackage(SchemaDefinition schema, PackageDefinition pkg) {






    }

    protected void generatePackage(PackageDefinition pkg, JavaWriter out) {



















































    }

    /**
     * Subclasses may override this method to provide package class footer code.
     */
    @SuppressWarnings("unused")
    protected void generatePackageClassFooter(PackageDefinition pkg, JavaWriter out) {}

    /**
     * Subclasses may override this method to provide their own Javadoc.
     */
    protected void generatePackageClassJavadoc(PackageDefinition pkg, JavaWriter out) {
        printClassJavadoc(out, "Convenience access to all stored procedures and functions in " + pkg.getName());
    }

    /**
     * Generating central static table access
     */
    protected void generateTableReferences(SchemaDefinition schema) {
        log.info("Generating table references");
        JavaWriter out = newJavaWriter(new File(getFile(schema).getParentFile(), "Tables.java"));

        printPackage(out, schema);

        if (!kotlin) {
            printClassJavadoc(out, "Convenience access to all tables in " + schemaNameOrDefault(schema) + ".");
            printClassAnnotations(out, schema, Mode.DEFAULT);
        }

        if (scala)
            out.println("object Tables {");
        else if (kotlin) {}
        else
            out.println("public class Tables {");

        for (TableDefinition table : database.getTables(schema)) {
            final String className = getStrategy().getJavaClassName(table);
            final String fullClassName = scala || kotlin
                ? ""
                : out.ref(getStrategy().getFullJavaClassName(table));
            final String id = getStrategy().getJavaIdentifier(table);

            // [#8863] Use the imported table class to dereference the singleton
            //         table instance, *only* if the class name is not equal to
            //         the instance name. Otherwise, we would get a
            //         "error: self-reference in initializer" compilation error
            final String referencedId = className.equals(id)
                ? getStrategy().getFullJavaIdentifier(table)
                : out.ref(getStrategy().getFullJavaIdentifier(table), 2);
            final String comment = escapeEntities(comment(table));

            out.javadoc(isBlank(comment) ? "The table <code>" + table.getQualifiedOutputName() + "</code>." : comment);

            if (scala || kotlin)
                out.println("val %s = %s", id, referencedId);
            else
                out.println("public static final %s %s = %s;", fullClassName, id, referencedId);

            // [#3797] Table-valued functions generate two different literals in
            // globalObjectReferences
            if (table.isTableValuedFunction())
                printTableValuedFunction(out, table, getStrategy().getJavaIdentifier(table));
        }

        if (kotlin) {}
        else
            out.println("}");

        closeJavaWriter(out);

        watch.splitInfo("Table refs generated");
    }

    private String schemaNameOrDefault(SchemaDefinition schema) {
        return StringUtils.isEmpty(schema.getOutputName()) ? "the default schema" : schema.getOutputName();
    }

    protected void generateDaos(SchemaDefinition schema) {
        log.info("Generating DAOs");

        for (TableDefinition table : database.getTables(schema)) {
            try {
                generateDao(table);
            }
            catch (Exception e) {
                log.error("Error while generating table DAO " + table, e);
            }
        }

        watch.splitInfo("Table DAOs generated");
    }

    protected void generateDao(TableDefinition table) {
        JavaWriter out = newJavaWriter(getFile(table, Mode.DAO));
        log.info("Generating DAO", out.file().getName());
        generateDao(table, out);
        closeJavaWriter(out);
    }

    protected void generateDao(TableDefinition table, JavaWriter out) {
        UniqueKeyDefinition key = table.getPrimaryKey();
        if (key == null) {
            log.info("Skipping DAO generation", out.file().getName());
            return;
        }

        final String className = getStrategy().getJavaClassName(table, Mode.DAO);
        final List<String> interfaces = out.ref(getStrategy().getJavaClassImplements(table, Mode.DAO));
        final String tableRecord = out.ref(getStrategy().getFullJavaClassName(table, Mode.RECORD));
        final String daoImpl = out.ref(DAOImpl.class);
        final String tableIdentifier = out.ref(getStrategy().getFullJavaIdentifier(table), 2);

        String tType = (scala || kotlin ? "Unit" : "Void");
        String pType = out.ref(getStrategy().getFullJavaClassName(table, Mode.POJO));

        List<ColumnDefinition> keyColumns = key.getKeyColumns();

        if (keyColumns.size() == 1) {
            tType = getJavaType(keyColumns.get(0).getType(resolver()), Mode.POJO);
        }
        else if (keyColumns.size() <= Constants.MAX_ROW_DEGREE) {
            String generics = "";
            String separator = "";

            for (ColumnDefinition column : keyColumns) {
                generics += separator + out.ref(getJavaType(column.getType(resolver())));
                separator = ", ";
            }

            if (scala)
                tType = Record.class.getName() + keyColumns.size() + "[" + generics + "]";
            else
                tType = Record.class.getName() + keyColumns.size() + "<" + generics + ">";
        }
        else {
            tType = Record.class.getName();
        }

        tType = out.ref(tType);

        printPackage(out, table, Mode.DAO);
        generateDaoClassJavadoc(table, out);
        printClassAnnotations(out, table, Mode.DAO);

        if (generateSpringAnnotations())
            out.println("@%s", out.ref("org.springframework.stereotype.Repository"));

        if (scala)
            out.println("class %s(configuration: %s) extends %s[%s, %s, %s](%s, classOf[%s], configuration)[[before= with ][separator= with ][%s]] {",
                className, Configuration.class, daoImpl, tableRecord, pType, tType, tableIdentifier, pType, interfaces);
        else if (kotlin)
            out.println("class %s(configuration: %s?) : %s<%s, %s, %s>(%s, %s::class.java, configuration)[[before=, ][%s]] {",
                className, Configuration.class, daoImpl, tableRecord, pType, tType, tableIdentifier, pType, interfaces);
        else
            out.println("public class %s extends %s<%s, %s, %s>[[before= implements ][%s]] {", className, daoImpl, tableRecord, pType, tType, interfaces);

        // Default constructor
        // -------------------
        out.javadoc("Create a new %s without any configuration", className);

        if (scala) {
            out.println("def this() = this(null)");
        }
        else if (kotlin) {
            out.println("constructor(): this(null)");
        }
        else {
            out.println("public %s() {", className);
            out.println("super(%s, %s.class);", tableIdentifier, pType);
            out.println("}");
        }

        // Initialising constructor
        // ------------------------

        if (!scala && !kotlin) {
            out.javadoc("Create a new %s with an attached configuration", className);

            if (generateSpringAnnotations())
                out.println("@%s", out.ref("org.springframework.beans.factory.annotation.Autowired"));

            out.println("public %s(%s configuration) {", className, Configuration.class);
            out.println("super(%s, %s.class, configuration);", tableIdentifier, pType);
            out.println("}");
        }

        // Template method implementations
        // -------------------------------
        if (scala) {
            out.println();
            out.print("override def getId(o: %s): %s = ", pType, tType);
        }
        else if (kotlin) {
            out.println();
            out.print("override fun getId(o: %s): %s? = ", pType, tType);
        }
        else {
            out.overrideInherit();
            out.println("public %s getId(%s object) {", tType, pType);
        }

        if (keyColumns.size() == 1) {
            if (scala)
                out.println("o.%s", getStrategy().getJavaGetterName(keyColumns.get(0), Mode.POJO));
            else if (kotlin)
                out.println("o.%s", getStrategy().getJavaMemberName(keyColumns.get(0), Mode.POJO));
            else
                out.println("return object.%s();", getStrategy().getJavaGetterName(keyColumns.get(0), Mode.POJO));
        }

        // [#2574] This should be replaced by a call to a method on the target table's Key type
        else {
            String params = "";
            String separator = "";

            for (ColumnDefinition column : keyColumns) {
                if (scala)
                    params += separator + "o." + getStrategy().getJavaGetterName(column, Mode.POJO);
                else if (kotlin)
                    params += separator + "o." + getStrategy().getJavaMemberName(column, Mode.POJO);
                else
                    params += separator + "object." + getStrategy().getJavaGetterName(column, Mode.POJO) + "()";

                separator = ", ";
            }

            if (scala || kotlin)
                out.println("compositeKeyRecord(%s)", params);
            else
                out.println("return compositeKeyRecord(%s);", params);
        }

        if (scala || kotlin) {}
        else
            out.println("}");

        for (ColumnDefinition column : table.getColumns()) {
            final String colName = column.getOutputName();
            final String colClass = getStrategy().getJavaClassName(column);
            final String colTypeFull = getJavaType(column.getType(resolver()));
            final String colType = out.ref(colTypeFull);
            final String colIdentifier = out.ref(getStrategy().getFullJavaIdentifier(column), colRefSegments(column));

            // fetchRangeOf[Column]([T]...)
            // -----------------------
            if (!printDeprecationIfUnknownType(out, colTypeFull))
                out.javadoc("Fetch records that have <code>%s BETWEEN lowerInclusive AND upperInclusive</code>", colName);

            if (scala) {
                out.println("def fetchRangeOf%s(lowerInclusive: %s, upperInclusive: %s): %s[%s] = fetchRange(%s, lowerInclusive, upperInclusive)",
                    colClass, colType, colType, List.class, pType, colIdentifier);
            }
            else if (kotlin) {
                out.println("fun fetchRangeOf%s(lowerInclusive: %s?, upperInclusive: %s?): %s<%s> = fetchRange(%s, lowerInclusive, upperInclusive)",
                    colClass, colType, colType, out.ref(KLIST), pType, colIdentifier);
            }
            else {
                out.println("public %s<%s> fetchRangeOf%s(%s lowerInclusive, %s upperInclusive) {", List.class, pType, colClass, colType, colType);
                out.println("return fetchRange(%s, lowerInclusive, upperInclusive);", colIdentifier);
                out.println("}");
            }

            // fetchBy[Column]([T]...)
            // -----------------------
            if (!printDeprecationIfUnknownType(out, colTypeFull))
                out.javadoc("Fetch records that have <code>%s IN (values)</code>", colName);

            if (scala) {
                out.println("def fetchBy%s(values: %s*): %s[%s] = fetch(%s, values:_*)", colClass, colType, List.class, pType, colIdentifier);
            }
            else if (kotlin) {
                String toTypedArray = PRIMITIVE_WRAPPERS.contains(colTypeFull) ? ".toTypedArray()" : "";
                out.println("fun fetchBy%s(vararg values: %s): %s<%s> = fetch(%s, *values%s)", colClass, colType, out.ref(KLIST), pType, colIdentifier, toTypedArray);
            }
            else {
                out.println("public %s<%s> fetchBy%s(%s... values) {", List.class, pType, colClass, colType);
                out.println("return fetch(%s, values);", colIdentifier);
                out.println("}");
            }

            // fetchOneBy[Column]([T])
            // -----------------------
            ukLoop:
            for (UniqueKeyDefinition uk : column.getUniqueKeys()) {

                // If column is part of a single-column unique key...
                if (uk.getKeyColumns().size() == 1 && uk.getKeyColumns().get(0).equals(column)) {
                    if (!printDeprecationIfUnknownType(out, colTypeFull))
                        out.javadoc("Fetch a unique record that has <code>%s = value</code>", colName);

                    if (scala) {
                        out.println("def fetchOneBy%s(value: %s): %s = fetchOne(%s, value)", colClass, colType, pType, colIdentifier);
                    }
                    else if (kotlin) {
                        out.println("fun fetchOneBy%s(value: %s): %s? = fetchOne(%s, value)", colClass, colType, pType, colIdentifier);
                    }
                    else {
                        out.println("public %s fetchOneBy%s(%s value) {", pType, colClass, colType);
                        out.println("return fetchOne(%s, value);", colIdentifier);
                        out.println("}");
                    }

                    break ukLoop;
                }
            }
        }

        generateDaoClassFooter(table, out);
        out.println("}");
    }

    /**
     * Subclasses may override this method to provide dao class footer code.
     */
    @SuppressWarnings("unused")
    protected void generateDaoClassFooter(TableDefinition table, JavaWriter out) {}

    /**
     * Subclasses may override this method to provide their own Javadoc.
     */
    protected void generateDaoClassJavadoc(TableDefinition table, JavaWriter out) {
        if (generateCommentsOnTables())
            printClassJavadoc(out, table);
        else
            printClassJavadoc(out, "The table <code>" + table.getQualifiedInputName() + "</code>.");
    }

    protected void generatePojos(SchemaDefinition schema) {
        log.info("Generating table POJOs");

        for (TableDefinition table : database.getTables(schema)) {
            try {
                generatePojo(table);
            }
            catch (Exception e) {
                log.error("Error while generating table POJO " + table, e);
            }
        }

        watch.splitInfo("Table POJOs generated");
    }

    protected void generatePojo(TableDefinition table) {
        JavaWriter out = newJavaWriter(getFile(table, Mode.POJO));
        log.info("Generating POJO", out.file().getName());
        generatePojo(table, out);
        closeJavaWriter(out);
    }

    protected void generateUDTPojo(UDTDefinition udt) {
        JavaWriter out = newJavaWriter(getFile(udt, Mode.POJO));
        log.info("Generating POJO", out.file().getName());
        generatePojo0(udt, out);
        closeJavaWriter(out);
    }

    protected void generatePojo(TableDefinition table, JavaWriter out) {
        generatePojo0(table, out);
    }

    protected void generateUDTPojo(UDTDefinition udt, JavaWriter out) {
        generatePojo0(udt, out);
    }

    private final void generatePojo0(Definition tableOrUDT, JavaWriter out) {
        final String className = getStrategy().getJavaClassName(tableOrUDT, Mode.POJO);
        final String interfaceName = generateInterfaces()
            ? out.ref(getStrategy().getFullJavaClassName(tableOrUDT, Mode.INTERFACE))
            : "";
        final String superName = out.ref(getStrategy().getJavaClassExtends(tableOrUDT, Mode.POJO));
        final List<String> interfaces = out.ref(getStrategy().getJavaClassImplements(tableOrUDT, Mode.POJO));

        if (generateInterfaces())
            interfaces.add(interfaceName);

        final List<String> superTypes = list(superName, interfaces);
        printPackage(out, tableOrUDT, Mode.POJO);

        if (tableOrUDT instanceof TableDefinition)
            generatePojoClassJavadoc((TableDefinition) tableOrUDT, out);
        else
            generateUDTPojoClassJavadoc((UDTDefinition) tableOrUDT, out);

        printClassAnnotations(out, tableOrUDT, Mode.POJO);

        if (tableOrUDT instanceof TableDefinition)
            printTableJPAAnnotation(out, (TableDefinition) tableOrUDT);

        int maxLength = 0;
        for (TypedElementDefinition<?> column : getTypedElements(tableOrUDT))
            maxLength = Math.max(maxLength, out.ref(getJavaType(column.getType(resolver(Mode.POJO)), Mode.POJO)).length());

        if (scala) {
            out.println("%sclass %s(", (generatePojosAsScalaCaseClasses() ? "case " : ""), className);

            String separator = "  ";
            for (TypedElementDefinition<?> column : getTypedElements(tableOrUDT)) {
                out.println("%s%s %s: %s",
                    separator,
                    generateImmutablePojos() ? "val" : "var",
                    getStrategy().getJavaMemberName(column, Mode.POJO),
                    out.ref(getJavaType(column.getType(resolver(Mode.POJO)), Mode.POJO)));

                separator = ", ";
            }

            out.println(")[[before= extends ][%s]][[before= with ][separator= with ][%s]] {", first(superTypes), remaining(superTypes));
        }
        else if (kotlin) {
            out.println("%sclass %s(", (generatePojosAsKotlinDataClasses() ? "data " : ""), className);

            String separator = "  ";
            for (TypedElementDefinition<?> column : getTypedElements(tableOrUDT)) {
                final String member = getStrategy().getJavaMemberName(column, Mode.POJO);

                out.println("%s%s%s %s: %s? = null",
                    separator,
                    generateInterfaces() ? "override " : "",
                    generateImmutablePojos() ? "val" : "var",
                    member,
                    out.ref(getJavaType(column.getType(resolver(Mode.POJO)), Mode.POJO)));

                separator = ", ";
            }

            out.println(")[[before=: ][%s]] {", superTypes);
        }
        else {
            out.println("public class %s[[before= extends ][%s]][[before= implements ][%s]] {", className, list(superName), interfaces);

            if (generateSerializablePojos() || generateSerializableInterfaces())
                out.printSerial();

            out.println();

            for (TypedElementDefinition<?> column : getTypedElements(tableOrUDT)) {
                out.println("private %s%s %s;",
                    generateImmutablePojos() ? "final " : "",
                    StringUtils.rightPad(out.ref(getJavaType(column.getType(resolver(Mode.POJO)), Mode.POJO)), maxLength),
                    getStrategy().getJavaMemberName(column, Mode.POJO));
            }
        }

        // Constructors
        // ---------------------------------------------------------------------

        // Default constructor
        if (!generateImmutablePojos())
            generatePojoDefaultConstructor(tableOrUDT, out);

        if (!kotlin) {

            // [#1363] [#7055] copy constructor
            generatePojoCopyConstructor(tableOrUDT, out);

            // Multi-constructor
            generatePojoMultiConstructor(tableOrUDT, out);

            List<? extends TypedElementDefinition<?>> elements = getTypedElements(tableOrUDT);
            for (int i = 0; i < elements.size(); i++) {
                TypedElementDefinition<?> column = elements.get(i);

                if (tableOrUDT instanceof TableDefinition)
                    generatePojoGetter(column, i, out);
                else
                    generateUDTPojoGetter(column, i, out);

                // Setter
                if (!generateImmutablePojos())
                    if (tableOrUDT instanceof TableDefinition)
                        generatePojoSetter(column, i, out);
                    else
                        generateUDTPojoSetter(column, i, out);
            }
        }

        if (generatePojosEqualsAndHashCode())
            generatePojoEqualsAndHashCode(tableOrUDT, out);

        if (generatePojosToString())
            generatePojoToString(tableOrUDT, out);

        if (generateInterfaces() && !generateImmutablePojos())
            printFromAndInto(out, tableOrUDT, Mode.POJO);

        if (tableOrUDT instanceof TableDefinition)
            generatePojoClassFooter((TableDefinition) tableOrUDT, out);
        else
            generateUDTPojoClassFooter((UDTDefinition) tableOrUDT, out);

        out.println("}");
        closeJavaWriter(out);
    }
    /**
     * Subclasses may override this method to provide their own pojo copy constructors.
     */
    protected void generatePojoMultiConstructor(Definition tableOrUDT, JavaWriter out) {
        final String className = getStrategy().getJavaClassName(tableOrUDT, Mode.POJO);
        final List<String> properties = new ArrayList<>();

        int maxLength = 0;
        for (TypedElementDefinition<?> column : getTypedElements(tableOrUDT)) {
            maxLength = Math.max(maxLength, out.ref(getJavaType(column.getType(resolver(Mode.POJO)), Mode.POJO)).length());
            properties.add("\"" + escapeString(getStrategy().getJavaMemberName(column, Mode.POJO)) + "\"");
        }

        if (scala) {
        }

        // [#3010] Invalid UDTs may have no attributes. Avoid generating this constructor in that case
        // [#3176] Avoid generating constructors for tables with more than 255 columns (Java's method argument limit)
        else if (getTypedElements(tableOrUDT).size() > 0 &&
                 getTypedElements(tableOrUDT).size() < 256) {
            out.println();

            if (generateConstructorPropertiesAnnotationOnPojos())
                out.println("@%s({ [[%s]] })", ConstructorProperties.class, properties);

            out.print("public %s(", className);

            String separator1 = "";
            for (TypedElementDefinition<?> column : getTypedElements(tableOrUDT)) {
                final String nullableAnnotation = nullableOrNonnullAnnotation(out, column);

                out.println(separator1);
                out.print("[[before=@][after= ][%s]]%s %s",
                    list(nullableAnnotation),
                    StringUtils.rightPad(out.ref(getJavaType(column.getType(resolver(Mode.POJO)), Mode.POJO)), maxLength),
                    getStrategy().getJavaMemberName(column, Mode.POJO));
                separator1 = ",";
            }

            out.println();
            out.println(") {");

            for (TypedElementDefinition<?> column : getTypedElements(tableOrUDT)) {
                final String columnMember = getStrategy().getJavaMemberName(column, Mode.POJO);

                out.println("this.%s = %s;", columnMember, columnMember);
            }

            out.println("}");
        }
    }

    /**
     * Subclasses may override this method to provide their own pojo copy constructors.
     */
    protected void generatePojoCopyConstructor(Definition tableOrUDT, JavaWriter out) {
        final String className = getStrategy().getJavaClassName(tableOrUDT, Mode.POJO);
        final String interfaceName = generateInterfaces()
            ? out.ref(getStrategy().getFullJavaClassName(tableOrUDT, Mode.INTERFACE))
            : "";

        out.println();

        if (scala) {
            out.println("def this(value: %s) = this(", generateInterfaces() ? interfaceName : className);

            String separator = "  ";
            for (TypedElementDefinition<?> column : getTypedElements(tableOrUDT)) {
                out.println("%svalue.%s",
                    separator,
                    generateInterfaces()
                        ? getStrategy().getJavaGetterName(column, Mode.INTERFACE)
                        : getStrategy().getJavaMemberName(column, Mode.POJO));

                separator = ", ";
            }

            out.println(")");
        }
        else {
            out.println("public %s(%s value) {", className, generateInterfaces() ? interfaceName : className);

            for (TypedElementDefinition<?> column : getTypedElements(tableOrUDT)) {
                out.println("this.%s = value.%s%s;",
                    getStrategy().getJavaMemberName(column, Mode.POJO),
                    generateInterfaces()
                        ? getStrategy().getJavaGetterName(column, Mode.INTERFACE)
                        : getStrategy().getJavaMemberName(column, Mode.POJO),
                    generateInterfaces()
                        ? "()"
                        : "");
            }

            out.println("}");
        }
    }

    /**
     * Subclasses may override this method to provide their own pojo default constructors.
     */
    protected void generatePojoDefaultConstructor(Definition tableOrUDT, JavaWriter out) {
        final String className = getStrategy().getJavaClassName(tableOrUDT, Mode.POJO);

        out.println();
        int size = getTypedElements(tableOrUDT).size();

        if (scala) {

            // [#3010] Invalid UDTs may have no attributes. Avoid generating this constructor in that case
            if (size > 0) {
                List<String> nulls = new ArrayList<>(size);
                for (TypedElementDefinition<?> column : getTypedElements(tableOrUDT))

                    // Avoid ambiguities between a single-T-value constructor
                    // and the copy constructor
                    if (size == 1)
                        nulls.add("null: " + out.ref(getJavaType(column.getType(resolver(Mode.POJO)), Mode.POJO)));
                    else
                        nulls.add("null");

                out.println("def this() = this([[%s]])", nulls);
            }
        }

        // [#6248] [#10288] The no-args constructor isn't needed because we have named, defaulted parameters
        else if (kotlin) {}
        else {
            out.println("public %s() {}", className);
        }
    }

    /**
     * Subclasses may override this method to provide their own pojo getters.
     */
    protected void generatePojoGetter(TypedElementDefinition<?> column, int index, JavaWriter out) {
        generatePojoGetter0(column, index, out);
    }

    /**
     * Subclasses may override this method to provide their own pojo getters.
     */
    protected void generateUDTPojoGetter(TypedElementDefinition<?> column, int index, JavaWriter out) {
        generatePojoGetter0(column, index, out);
    }

    private final void generatePojoGetter0(TypedElementDefinition<?> column, @SuppressWarnings("unused") int index, JavaWriter out) {
        final String columnTypeFull = getJavaType(column.getType(resolver(Mode.POJO)), Mode.POJO);
        final String columnType = out.ref(columnTypeFull);
        final String columnGetter = getStrategy().getJavaGetterName(column, Mode.POJO);
        final String columnMember = getStrategy().getJavaMemberName(column, Mode.POJO);
        final String name = column.getQualifiedOutputName();

        // Getter
        if (!printDeprecationIfUnknownType(out, columnTypeFull))
            out.javadoc("Getter for <code>%s</code>.[[before= ][%s]]", name, list(escapeEntities(comment(column))));

        if (column instanceof ColumnDefinition)
            printColumnJPAAnnotation(out, (ColumnDefinition) column);

        printValidationAnnotation(out, column);
        printNullableOrNonnullAnnotation(out, column);

        if (scala) {
            out.println("def %s: %s = this.%s", columnGetter, columnType, columnMember);
        }
        else {
            out.overrideIf(generateInterfaces());
            out.println("public %s %s() {", columnType, columnGetter);
            out.println("return this.%s;", columnMember);
            out.println("}");
        }
    }

    /**
     * Subclasses may override this method to provide their own pojo setters.
     */
    protected void generatePojoSetter(TypedElementDefinition<?> column, int index, JavaWriter out) {
        generatePojoSetter0(column, index, out);
    }

    /**
     * Subclasses may override this method to provide their own pojo setters.
     */
    protected void generateUDTPojoSetter(TypedElementDefinition<?> column, int index, JavaWriter out) {
        generatePojoSetter0(column, index, out);
    }

    private final void generatePojoSetter0(TypedElementDefinition<?> column, @SuppressWarnings("unused") int index, JavaWriter out) {
        final String className = getStrategy().getJavaClassName(column.getContainer(), Mode.POJO);
        final String columnTypeFull = getJavaType(column.getType(resolver(Mode.POJO)), Mode.POJO);
        final String columnType = out.ref(columnTypeFull);
        final String columnSetterReturnType = generateFluentSetters() ? className : tokenVoid;
        final String columnSetter = getStrategy().getJavaSetterName(column, Mode.POJO);
        final String columnMember = getStrategy().getJavaMemberName(column, Mode.POJO);
        final boolean isUDT = column.getType(resolver()).isUDT();
        final boolean isUDTArray = column.getType(resolver()).isArray() && database.getArray(column.getType(resolver()).getSchema(), column.getType(resolver()).getQualifiedUserType()).getElementType(resolver()).isUDT();
        final String name = column.getQualifiedOutputName();

        // We cannot have covariant setters for arrays because of type erasure
        if (!(generateInterfaces() && isUDTArray)) {
            if (!printDeprecationIfUnknownType(out, columnTypeFull))
                out.javadoc("Setter for <code>%s</code>.[[before= ][%s]]", name, list(escapeEntities(comment(column))));

            if (scala) {
                out.println("def %s(%s: %s): %s = {", columnSetter, columnMember, columnType, columnSetterReturnType);
                out.println("this.%s = %s", columnMember, columnMember);

                if (generateFluentSetters())
                    out.println("this");

                out.println("}");
            }
            else {
                final String nullableAnnotation = nullableOrNonnullAnnotation(out, column);

                out.overrideIf(generateInterfaces() && !generateImmutableInterfaces() && !isUDT);
                out.println("public %s %s([[before=@][after= ][%s]]%s %s) {", columnSetterReturnType, columnSetter, list(nullableAnnotation), varargsIfArray(columnType), columnMember);
                out.println("this.%s = %s;", columnMember, columnMember);

                if (generateFluentSetters())
                    out.println("return this;");

                out.println("}");
            }
        }

        // [#3117] To avoid covariant setters on POJOs, we need to generate two setter overloads
        if (generateInterfaces() && (isUDT || isUDTArray)) {
            final String columnTypeInterface = out.ref(getJavaType(column.getType(resolver(Mode.INTERFACE)), Mode.INTERFACE));

            out.println();

            if (scala) {
                // [#3082] TODO Handle <interfaces/> + ARRAY also for Scala

                out.println("def %s(%s: %s): %s = {", columnSetter, columnMember, columnTypeInterface, columnSetterReturnType);
                out.println("if (%s == null)", columnMember);
                out.println("this.%s = null", columnMember);
                out.println("else");
                out.println("this.%s = %s.into(new %s)", columnMember, columnMember, columnType);

                if (generateFluentSetters())
                    out.println("this");

                out.println("}");
            }
            else {
                out.override();
                out.println("public %s %s(%s %s) {", columnSetterReturnType, columnSetter, varargsIfArray(columnTypeInterface), columnMember);
                out.println("if (%s == null)", columnMember);
                out.println("this.%s = null;", columnMember);

                if (isUDT) {
                    out.println("else");
                    out.println("this.%s = %s.into(new %s());", columnMember, columnMember, columnType);
                }
                else if (isUDTArray) {
                    final ArrayDefinition array = database.getArray(column.getType(resolver()).getSchema(), column.getType(resolver()).getQualifiedUserType());
                    final String componentType = out.ref(getJavaType(array.getElementType(resolver(Mode.POJO)), Mode.POJO));
                    final String componentTypeInterface = out.ref(getJavaType(array.getElementType(resolver(Mode.INTERFACE)), Mode.INTERFACE));

                    out.println("else {");
                    out.println("this.%s = new %s();", columnMember, ArrayList.class);
                    out.println();
                    out.println("for (%s i : %s)", componentTypeInterface, columnMember);
                    out.println("this.%s.add(i.into(new %s()));", columnMember, componentType);
                    out.println("}");
                }

                if (generateFluentSetters())
                    out.println("return this;");

                out.println("}");
            }
        }
    }

    protected void generatePojoEqualsAndHashCode(Definition tableOrUDT, JavaWriter out) {
        if (scala && generatePojosAsScalaCaseClasses())
            return;
        if (kotlin && generatePojosAsKotlinDataClasses())
            return;
        if (java && generatePojosAsJavaRecordClasses())
            return;

        final String className = getStrategy().getJavaClassName(tableOrUDT, Mode.POJO);

        out.println();

        if (scala) {
            out.println("override def equals(obj: Any): scala.Boolean = {");
            out.println("if (this eq obj.asInstanceOf[AnyRef])");
            out.println("return true");
            out.println("if (obj == null)");
            out.println("return false");
            out.println("if (getClass() != obj.getClass())");
            out.println("return false");

            out.println("val other = obj.asInstanceOf[%s]", className);

            for (TypedElementDefinition<?> column : getTypedElements(tableOrUDT)) {
                final String columnMember = getStrategy().getJavaMemberName(column, Mode.POJO);

                out.println("if (%s == null) {", columnMember);
                out.println("if (other.%s != null)", columnMember);
                out.println("return false");
                out.println("}");

                if (getJavaType(column.getType(resolver())).endsWith("[]"))
                    out.println("else if (!%s.equals(%s, other.%s))", Arrays.class, columnMember, columnMember);
                else
                    out.println("else if (!%s.equals(other.%s))", columnMember, columnMember);

                out.println("return false");
            }

            out.println("true");
            out.println("}");
        }
        else if (kotlin) {
            out.println("override fun equals(obj: Any?): Boolean {");
            out.println("if (this === obj)");
            out.println("return true");
            out.println("if (obj === null)");
            out.println("return false");
            out.println("if (this::class != obj::class)");
            out.println("return false");

            out.println("val other: %s = obj as %s", className, className);

            for (TypedElementDefinition<?> column : getTypedElements(tableOrUDT)) {
                final String columnMember = getStrategy().getJavaMemberName(column, Mode.POJO);

                out.println("if (%s === null) {", columnMember);
                out.println("if (other.%s !== null)", columnMember);
                out.println("return false");
                out.println("}");

                if (getJavaType(column.getType(resolver())).endsWith("[]"))
                    out.println("else if (!%s.equals(%s, other.%s))", Arrays.class, columnMember, columnMember);
                else
                    out.println("else if (%s != other.%s)", columnMember, columnMember);

                out.println("return false");
            }

            out.println("return true");
            out.println("}");
        }
        else {
            out.println("@Override");
            out.println("public boolean equals(%s obj) {", Object.class);
            out.println("if (this == obj)");
            out.println("return true;");
            out.println("if (obj == null)");
            out.println("return false;");
            out.println("if (getClass() != obj.getClass())");
            out.println("return false;");

            out.println("final %s other = (%s) obj;", className, className);

            for (TypedElementDefinition<?> column : getTypedElements(tableOrUDT)) {
                final String columnMember = getStrategy().getJavaMemberName(column, Mode.POJO);

                out.println("if (%s == null) {", columnMember);
                out.println("if (other.%s != null)", columnMember);
                out.println("return false;");
                out.println("}");

                if (getJavaType(column.getType(resolver())).endsWith("[]"))
                    out.println("else if (!%s.equals(%s, other.%s))", Arrays.class, columnMember, columnMember);
                else
                    out.println("else if (!%s.equals(other.%s))", columnMember, columnMember);

                out.println("return false;");
            }

            out.println("return true;");
            out.println("}");
        }

        out.println();

        if (scala) {
            out.println("override def hashCode: Int = {");
            out.println("val prime = 31");
            out.println("var result = 1");

            for (TypedElementDefinition<?> column : getTypedElements(tableOrUDT)) {
                final String columnMember = getStrategy().getJavaMemberName(column, Mode.POJO);

                if (getJavaType(column.getType(resolver())).endsWith("[]"))
                    out.println("result = prime * result + (if (this.%s == null) 0 else %s.hashCode(this.%s))", columnMember, Arrays.class, columnMember);
                else
                    out.println("result = prime * result + (if (this.%s == null) 0 else this.%s.hashCode())", columnMember, columnMember);
            }

            out.println("return result");
            out.println("}");
        }
        else if (kotlin) {
            out.println("override fun hashCode(): Int {");
            out.println("val prime = 31");
            out.println("var result = 1");

            for (TypedElementDefinition<?> column : getTypedElements(tableOrUDT)) {
                final String columnMember = getStrategy().getJavaMemberName(column, Mode.POJO);

                if (getJavaType(column.getType(resolver())).endsWith("[]"))
                    out.println("result = prime * result + (if (this.%s === null) 0 else %s.hashCode(this.%s))", columnMember, Arrays.class, columnMember);
                else
                    out.println("result = prime * result + (if (this.%s === null) 0 else this.%s.hashCode())", columnMember, columnMember);
            }

            out.println("return result");
            out.println("}");
        }
        else {
            out.println("@Override");
            out.println("public int hashCode() {");
            out.println("final int prime = 31;");
            out.println("int result = 1;");

            for (TypedElementDefinition<?> column : getTypedElements(tableOrUDT)) {
                final String columnMember = getStrategy().getJavaMemberName(column, Mode.POJO);

                if (getJavaType(column.getType(resolver())).endsWith("[]"))
                    out.println("result = prime * result + ((this.%s == null) ? 0 : %s.hashCode(this.%s));", columnMember, Arrays.class, columnMember);
                else
                    out.println("result = prime * result + ((this.%s == null) ? 0 : this.%s.hashCode());", columnMember, columnMember);
            }

            out.println("return result;");
            out.println("}");
        }
    }

    protected void generatePojoToString(Definition tableOrUDT, JavaWriter out) {
        if (scala && generatePojosAsScalaCaseClasses())
            return;
        if (kotlin && generatePojosAsKotlinDataClasses())
            return;
        if (java && generatePojosAsJavaRecordClasses())
            return;

        final String className = getStrategy().getJavaClassName(tableOrUDT, Mode.POJO);

        out.println();

        if (scala) {
            out.println("override def toString: String = {");

            out.println("val sb = new %s(\"%s (\")", StringBuilder.class, className);
            out.println();

            String separator = "";
            for (TypedElementDefinition<?> column : getTypedElements(tableOrUDT)) {
                final String columnMember = getStrategy().getJavaMemberName(column, Mode.POJO);
                final String columnType = getJavaType(column.getType(resolver()));

                if (columnType.equals("scala.Array[scala.Byte]"))
                    out.println("sb%s.append(\"[binary...]\")", separator);
                else
                    out.println("sb%s.append(%s)", separator, columnMember);

                separator = ".append(\", \")";
            }

            out.println();
            out.println("sb.append(\")\")");

            out.println("sb.toString");
            out.println("}");
        }
        else if (kotlin) {
            out.println("override fun toString(): String {");
            out.println("val sb = %s(\"%s (\")", StringBuilder.class, className);
            out.println();

            String separator = "";
            for (TypedElementDefinition<?> column : getTypedElements(tableOrUDT)) {
                final String columnMember = getStrategy().getJavaMemberName(column, Mode.POJO);
                final String columnType = getJavaType(column.getType(resolver()));
                final boolean array = columnType.endsWith("[]");

                if (array && columnType.equals("kotlin.ByteArray"))
                    out.println("sb%s.append(\"[binary...]\")", separator);
                else if (array)
                    out.println("sb%s.append(%s.toString(%s))", separator, Arrays.class, columnMember);
                else
                    out.println("sb%s.append(%s)", separator, columnMember);

                separator = ".append(\", \")";
            }

            out.println();
            out.println("sb.append(\")\")");
            out.println("return sb.toString()");
            out.println("}");
        }
        else {
            out.println("@Override");
            out.println("public String toString() {");
            out.println("%s sb = new %s(\"%s (\");", StringBuilder.class, StringBuilder.class, className);
            out.println();

            String separator = "";
            for (TypedElementDefinition<?> column : getTypedElements(tableOrUDT)) {
                final String columnMember = getStrategy().getJavaMemberName(column, Mode.POJO);
                final String columnType = getJavaType(column.getType(resolver()));
                final boolean array = columnType.endsWith("[]");

                if (array && columnType.equals("byte[]"))
                    out.println("sb%s.append(\"[binary...]\");", separator);
                else if (array)
                    out.println("sb%s.append(%s.toString(%s));", separator, Arrays.class, columnMember);
                else
                    out.println("sb%s.append(%s);", separator, columnMember);

                separator = ".append(\", \")";
            }

            out.println();
            out.println("sb.append(\")\");");
            out.println("return sb.toString();");
            out.println("}");
        }
    }

    private List<? extends TypedElementDefinition<? extends Definition>> getTypedElements(Definition definition) {
        if (definition instanceof TableDefinition)
            return ((TableDefinition) definition).getColumns();
        else if (definition instanceof EmbeddableDefinition)
            return ((EmbeddableDefinition) definition).getColumns();
        else if (definition instanceof UDTDefinition)
            return ((UDTDefinition) definition).getAttributes();
        else if (definition instanceof RoutineDefinition)
            return ((RoutineDefinition) definition).getAllParameters();
        else
            throw new IllegalArgumentException("Unsupported type : " + definition);
    }

    /**
     * Subclasses may override this method to provide POJO class footer code.
     */
    @SuppressWarnings("unused")
    protected void generatePojoClassFooter(TableDefinition table, JavaWriter out) {}

    /**
     * Subclasses may override this method to provide their own Javadoc.
     */
    protected void generatePojoClassJavadoc(TableDefinition table, JavaWriter out) {
        if (generateCommentsOnTables())
            printClassJavadoc(out, table);
        else
            printClassJavadoc(out, "The table <code>" + table.getQualifiedInputName() + "</code>.");
    }

    protected void generateTables(SchemaDefinition schema) {
        log.info("Generating tables");

        for (TableDefinition table : database.getTables(schema)) {
            try {
                generateTable(schema, table);
            }
            catch (Exception e) {
                log.error("Error while generating table " + table, e);
            }
        }

        watch.splitInfo("Tables generated");
    }

    @SuppressWarnings("unused")
    protected void generateTable(SchemaDefinition schema, TableDefinition table) {
        JavaWriter out = newJavaWriter(getFile(table));
        generateTable(table, out);
        closeJavaWriter(out);
    }

    protected void generateTable(TableDefinition table, JavaWriter out) {
        final SchemaDefinition schema = table.getSchema();
        final UniqueKeyDefinition primaryKey = table.getPrimaryKey();

        final String className = getStrategy().getJavaClassName(table);
        final String tableId = scala
            ? out.ref(getStrategy().getFullJavaIdentifier(table), 2)
            : getStrategy().getJavaIdentifier(table);
        final String recordType = out.ref(getStrategy().getFullJavaClassName(table, Mode.RECORD));
        final List<String> interfaces = out.ref(getStrategy().getJavaClassImplements(table, Mode.DEFAULT));
        final String schemaId = out.ref(getStrategy().getFullJavaIdentifier(schema), 2);
        final String tableType = table.isTemporary()
            ? "temporaryTable"
            : table.isView()
            ? "view"
            : table.isMaterializedView()
            ? "materializedView"
            : table.isTableValuedFunction()
            ? "function"
            : "table";
        final List<ParameterDefinition> parameters = table.getParameters();

        log.info("Generating table", out.file().getName() +
            " [input=" + table.getInputName() +
            ", output=" + table.getOutputName() +
            ", pk=" + (primaryKey != null ? primaryKey.getName() : "N/A") +
            "]");

        printPackage(out, table);

        if (scala) {
            out.println("object %s {", className);
            printSingletonInstance(out, table);
            out.println("}");
            out.println();
        }

        generateTableClassJavadoc(table, out);
        printClassAnnotations(out, table, Mode.DEFAULT);

        if (scala) {
            out.println("class %s(", className);
            out.println("alias: %s,", Name.class);
            out.println("child: %s[_ <: %s],", Table.class, Record.class);
            out.println("path: %s[_ <: %s, %s],", ForeignKey.class, Record.class, recordType);
            out.println("aliased: %s[%s],", Table.class, recordType);
            out.println("parameters: %s[ %s[_] ]", out.ref("scala.Array"), Field.class);
            out.println(")");
            out.println("extends %s[%s](", TableImpl.class, recordType);
            out.println("alias,");
            out.println("%s,", schemaId);
            out.println("child,");
            out.println("path,");
            out.println("aliased,");
            out.println("parameters,");
            out.println("%s.comment(\"%s\"),", DSL.class, escapeString(comment(table)));

            if (generateSourcesOnViews() && table.isView() && table.getSource() != null)
                out.println("%s.%s(\"%s\")", TableOptions.class, tableType, escapeString(table.getSource()));
            else
                out.println("%s.%s", TableOptions.class, tableType);

            out.println(")[[before= with ][separator= with ][%s]] {", interfaces);
        }
        else if (kotlin) {
            out.println("class %s(", className);
            out.println("alias: %s,", Name.class);
            out.println("child: %s<out %s>?,", Table.class, Record.class);
            out.println("path: %s<out %s, %s>?,", ForeignKey.class, Record.class, recordType);
            out.println("aliased: %s<%s>?,", Table.class, recordType);
            out.println("parameters: Array<%s<*>?>?", Field.class);
            out.println("): %s<%s>(", TableImpl.class, recordType);
            out.println("alias,");
            out.println("%s,", schemaId);
            out.println("child,");
            out.println("path,");
            out.println("aliased,");
            out.println("parameters,");
            out.println("%s.comment(\"%s\"),", DSL.class, escapeString(comment(table)));

            if (generateSourcesOnViews() && table.isView() && table.getSource() != null)
                out.println("%s.%s(\"%s\")", TableOptions.class, tableType, escapeString(table.getSource()));
            else
                out.println("%s.%s()", TableOptions.class, tableType);

            out.println(")[[before=, ][%s]] {", interfaces);

            out.println("companion object {");
            printSingletonInstance(out, table);
            out.println("}");
        }
        else {
            out.println("public class %s extends %s<%s>[[before= implements ][%s]] {", className, TableImpl.class, recordType, interfaces);
            out.printSerial();
            printSingletonInstance(out, table);
        }

        printRecordTypeMethod(out, table);

        for (ColumnDefinition column : table.getColumns()) {
            final String columnTypeFull = getJavaType(column.getType(resolver()));
            final String columnType = out.ref(columnTypeFull);
            final String columnTypeRef = getJavaTypeReference(column.getDatabase(), column.getType(resolver()));
            final String columnId = out.ref(getStrategy().getJavaIdentifier(column), colRefSegments(column));
            final String columnName = column.getName();
            final List<String> converter = out.ref(list(column.getType(resolver()).getConverter()));
            final List<String> binding = out.ref(list(column.getType(resolver()).getBinding()));

            if (!printDeprecationIfUnknownType(out, columnTypeFull))
                out.javadoc("The column <code>%s</code>.[[before= ][%s]]", column.getQualifiedOutputName(), list(escapeEntities(comment(column))));

            if (scala) {
                out.println("val %s: %s[%s, %s] = createField(%s.name(\"%s\"), %s, \"%s\"" + converterTemplate(converter) + converterTemplate(binding) + ")",
                        columnId, TableField.class, recordType, columnType, DSL.class, columnName, columnTypeRef, escapeString(comment(column)), converter, binding);
            }
            else if (kotlin) {
                out.println("val %s: %s<%s, %s?> = createField(%s.name(\"%s\"), %s, this, \"%s\"" + converterTemplate(converter) + converterTemplate(binding) + ")",
                    columnId, TableField.class, recordType, columnType, DSL.class, columnName, columnTypeRef, escapeString(comment(column)), converter, binding);
            }
            else {
                String isStatic = generateInstanceFields() ? "" : "static ";
                String tableRef = generateInstanceFields() ? "this" : out.ref(getStrategy().getJavaIdentifier(table), 2);

                out.println("public %sfinal %s<%s, %s> %s = createField(%s.name(\"%s\"), %s, %s, \"%s\"" + converterTemplate(converter) + converterTemplate(binding) + ");",
                    isStatic, TableField.class, recordType, columnType, columnId, DSL.class, columnName, columnTypeRef, tableRef, escapeString(comment(column)), converter, binding);
            }
        }

        // [#2530] Embeddable types
        for (EmbeddableDefinition embeddable : table.getEmbeddables()) {
            final String columnId = out.ref(getStrategy().getJavaIdentifier(embeddable), colRefSegments(null));
            final String columnType = out.ref(getStrategy().getFullJavaClassName(embeddable, Mode.RECORD));

            final List<String> columnIds = new ArrayList<>();
            for (EmbeddableColumnDefinition column : embeddable.getColumns())
                columnIds.add(out.ref(getStrategy().getJavaIdentifier(column), colRefSegments(column)));

            out.javadoc("The embeddable type <code>%s</code>.", embeddable.getOutputName());

            if (scala)
                out.println("val %s: %s[%s, %s] = %s.createEmbeddable(%s.name(\"%s\"), classOf[%s], this, [[%s]])",
                        columnId, TableField.class, recordType, columnType, Internal.class, DSL.class, embeddable.getName(), columnType, columnIds);
            else if (kotlin)
                out.println("val %s: %s<%s, %s> = %s.createEmbeddable(%s.name(\"%s\"), %s::class.java, this, [[%s]])",
                    columnId, TableField.class, recordType, columnType, Internal.class, DSL.class, embeddable.getName(), columnType, columnIds);
            else
                out.println("public final %s<%s, %s> %s = %s.createEmbeddable(%s.name(\"%s\"), %s.class, this, [[%s]]);",
                    TableField.class, recordType, columnType, columnId, Internal.class, DSL.class, embeddable.getName(), columnType, columnIds);
        }

        out.println();

        // [#10191] This constructor must be generated first in Scala to prevent
        // "called constructor's definition must precede calling constructor's definition"
        if (scala) {
            if (table.isTableValuedFunction())
                out.println("private def this(alias: %s, aliased: %s[%s]) = this(alias, null, null, aliased, new %s[ %s[_] ](%s))",
                    Name.class, Table.class, recordType, out.ref("scala.Array"), Field.class, parameters.size());
            else
                out.println("private def this(alias: %s, aliased: %s[%s]) = this(alias, null, null, aliased, null)",
                    Name.class, Table.class, recordType);
        }
        else if (kotlin) {
            if (table.isTableValuedFunction())
                out.println("private constructor(alias: %s, aliased: %s<%s>?): this(alias, null, null, aliased, arrayOf())",
                    Name.class, Table.class, recordType, Field.class, parameters.size());
            else
                out.println("private constructor(alias: %s, aliased: %s<%s>?): this(alias, null, null, aliased, null)",
                    Name.class, Table.class, recordType);

            out.println("private constructor(alias: %s, aliased: %s<%s>?, parameters: Array<%s<*>?>?): this(alias, null, null, aliased, parameters)",
                Name.class, Table.class, recordType, Field.class);
        }
        else {
            out.println("private %s(%s alias, %s<%s> aliased) {", className, Name.class, Table.class, recordType);
            if (table.isTableValuedFunction())
                out.println("this(alias, aliased, new %s[%s]);", Field.class, parameters.size());
            else
                out.println("this(alias, aliased, null);");

            out.println("}");

            out.println();
            out.println("private %s(%s alias, %s<%s> aliased, %s<?>[] parameters) {", className, Name.class, Table.class, recordType, Field.class);

            if (generateSourcesOnViews() && table.isView() && table.getSource() != null)
                out.println("super(alias, null, aliased, parameters, %s.comment(\"%s\"), %s.%s(\"%s\"));", DSL.class, escapeString(comment(table)), TableOptions.class, tableType, escapeString(table.getSource()));
            else
                out.println("super(alias, null, aliased, parameters, %s.comment(\"%s\"), %s.%s());", DSL.class, escapeString(comment(table)), TableOptions.class, tableType);

            out.println("}");
        }

        if (scala) {
            out.javadoc("Create an aliased <code>%s</code> table reference", table.getQualifiedOutputName());
            out.println("def this(alias: %s) = this(%s.name(alias), %s)", String.class, DSL.class, tableId);

            out.javadoc("Create an aliased <code>%s</code> table reference", table.getQualifiedOutputName());
            out.println("def this(alias: %s) = this(alias, %s)", Name.class, tableId);
        }
        else if (kotlin) {
            out.javadoc("Create an aliased <code>%s</code> table reference", table.getQualifiedOutputName());
            out.println("constructor(alias: %s): this(%s.name(alias))", String.class, DSL.class);

            out.javadoc("Create an aliased <code>%s</code> table reference", table.getQualifiedOutputName());
            out.println("constructor(alias: %s): this(alias, null)", Name.class, tableId);
        }

        // [#117] With instance fields, it makes sense to create a
        // type-safe table alias
        // [#1255] With instance fields, the table constructor may
        // be public, as tables are no longer singletons
        else if (generateInstanceFields()) {
            out.javadoc("Create an aliased <code>%s</code> table reference", table.getQualifiedOutputName());
            out.println("public %s(%s alias) {", className, String.class);
            out.println("this(%s.name(alias), %s);", DSL.class, tableId);
            out.println("}");

            out.javadoc("Create an aliased <code>%s</code> table reference", table.getQualifiedOutputName());
            out.println("public %s(%s alias) {", className, Name.class);
            out.println("this(alias, %s);", tableId);
            out.println("}");
        }

        if (scala) {
            out.javadoc("Create a <code>%s</code> table reference", table.getQualifiedOutputName());
            out.println("def this() = this(%s.name(\"%s\"))", DSL.class, escapeString(table.getOutputName()));
        }
        else if (kotlin) {
            out.javadoc("Create a <code>%s</code> table reference", table.getQualifiedOutputName());
            out.println("constructor(): this(%s.name(\"%s\"))", DSL.class, escapeString(table.getOutputName()));
        }
        else {
            // [#1255] With instance fields, the table constructor may
            // be public, as tables are no longer singletons
            if (generateInstanceFields()) {
                out.javadoc("Create a <code>%s</code> table reference", table.getQualifiedOutputName());
                out.println("public %s() {", className);
            }
            else {
                out.javadoc(NO_FURTHER_INSTANCES_ALLOWED);
                out.println("private %s() {", className);
            }

            out.println("this(%s.name(\"%s\"), null);", DSL.class, escapeString(table.getOutputName()));
            out.println("}");
        }

        if (generateImplicitJoinPathsToOne() && generateGlobalKeyReferences() && !table.isTableValuedFunction()) {
            out.println();

            if (scala) {
                out.println("def this(child: %s[_ <: %s], key: %s[_ <: %s, %s]) = this(%s.createPathAlias(child, key), child, key, %s, null)",
                    Table.class, Record.class, ForeignKey.class, Record.class, recordType, Internal.class, tableId);
            }
            else if (kotlin) {
                out.println("constructor(child: %s<out %s>, key: %s<out %s, %s>): this(%s.createPathAlias(child, key), child, key, %s, null)",
                    Table.class, Record.class, ForeignKey.class, Record.class, recordType, Internal.class, tableId);
            }
            else {
                out.println("public <O extends %s> %s(%s<O> child, %s<O, %s> key) {", Record.class, className, Table.class, ForeignKey.class, recordType);
                out.println("super(child, key, %s);", tableId);
                out.println("}");
            }
        }

        if (scala) {
            out.println();
            out.println("override def getSchema: %s = %s", Schema.class, schemaId);
        }
        else if (kotlin) {
            out.println("override fun getSchema(): %s = %s", Schema.class, schemaId);
        }
        else {
            out.overrideInherit();
            out.println("public %s getSchema() {", Schema.class);
            out.println("return %s;", schemaId);
            out.println("}");
        }

        // Add index information
        if (generateIndexes()) {
            List<IndexDefinition> indexes = table.getIndexes();

            if (!indexes.isEmpty()) {
                if (generateGlobalIndexReferences()) {
                    final List<String> indexFullIds = out.ref(getStrategy().getFullJavaIdentifiers(indexes), 2);

                    if (scala) {
                        out.println();
                        out.println("override def getIndexes: %s[%s] = %s.asList[ %s ]([[%s]])",
                            List.class, Index.class, Arrays.class, Index.class, indexFullIds);
                    }
                    else if (kotlin) {
                        out.println("override fun getIndexes(): %s<%s> = listOf([[%s]])", out.ref(KLIST), Index.class, indexFullIds);
                    }
                    else {
                        out.overrideInherit();
                        out.println("public %s<%s> getIndexes() {", List.class, Index.class);
                        out.println("return %s.<%s>asList([[%s]]);", Arrays.class, Index.class, indexFullIds);
                        out.println("}");
                    }
                }
                else {
                    String separator = "";

                    if (scala) {
                        out.println();
                        out.println("override def getIndexes: %s[%s] = %s.asList[%s](", List.class, Index.class, Arrays.class, Index.class);

                        for (IndexDefinition index : indexes) {
                            out.print("%s", separator);
                            printCreateIndex(out, index);
                            out.println();
                            separator = ", ";
                        }

                        out.println(")");
                    }
                    else if (kotlin) {
                        out.println("override fun getIndexes(): %s<%s> = listOf(", out.ref(KLIST), Index.class);

                        for (IndexDefinition index : indexes) {
                            out.print("%s", separator);
                            printCreateIndex(out, index);
                            out.println();
                            separator = ", ";
                        }

                        out.println(")");
                    }
                    else {
                        out.overrideInherit();
                        out.println("public %s<%s> getIndexes() {", List.class, Index.class);
                        out.println("return %s.<%s>asList(", Arrays.class, Index.class);

                        for (IndexDefinition index : indexes) {
                            out.print("%s", separator);
                            printCreateIndex(out, index);
                            out.println();
                            separator = ", ";
                        }

                        out.println(");");
                        out.println("}");
                    }
                }
            }
        }

        // Add primary / unique / foreign key information
        if (generateRelations()) {
            IdentityDefinition identity = table.getIdentity();

            // The identity column
            if (identity != null) {
                final String identityTypeFull = getJavaType(identity.getColumn().getType(resolver()));
                final String identityType = out.ref(identityTypeFull);

                if (scala) {
                    out.println();

                    printDeprecationIfUnknownType(out, identityTypeFull);
                    out.println("override def getIdentity: %s[%s, %s] = super.getIdentity.asInstanceOf[ %s[%s, %s] ]", Identity.class, recordType, identityType, Identity.class, recordType, identityType);
                }
                else if (kotlin) {
                    printDeprecationIfUnknownType(out, identityTypeFull);
                    out.println("override fun getIdentity(): %s<%s, %s?> = super.getIdentity() as %s<%s, %s?>", Identity.class, recordType, identityType, Identity.class, recordType, identityType);
                }
                else {
                    if (printDeprecationIfUnknownType(out, identityTypeFull))
                        out.override();
                    else
                        out.overrideInherit();

                    out.println("public %s<%s, %s> getIdentity() {", Identity.class, recordType, identityType);
                    out.println("return (%s<%s, %s>) super.getIdentity();", Identity.class, recordType, identityType);
                    out.println("}");
                }
            }

            // The primary / main unique key
            if (primaryKey != null) {
                final String keyFullId = generateGlobalKeyReferences()
                    ? out.ref(getStrategy().getFullJavaIdentifier(primaryKey), 2)
                    : null;

                if (scala) {
                    out.println();
                    out.print("override def getPrimaryKey: %s[%s] = ", UniqueKey.class, recordType);

                    if (keyFullId != null)
                        out.print("%s", keyFullId);
                    else
                        printCreateUniqueKey(out, primaryKey);

                    out.println();
                }
                else if (kotlin) {
                    out.print("override fun getPrimaryKey(): %s<%s> = ", UniqueKey.class, recordType);

                    if (keyFullId != null)
                        out.print("%s", keyFullId);
                    else
                        printCreateUniqueKey(out, primaryKey);

                    out.println();
                }
                else {
                    out.overrideInherit();
                    out.println("public %s<%s> getPrimaryKey() {", UniqueKey.class, recordType);
                    out.print("return ");

                    if (keyFullId != null)
                        out.print("%s", keyFullId);
                    else
                        printCreateUniqueKey(out, primaryKey);

                    out.println(";");
                    out.println("}");
                }
            }

            // The remaining unique keys
            List<UniqueKeyDefinition> uniqueKeys = table.getUniqueKeys();
            if (uniqueKeys.size() > 0) {
                if (generateGlobalKeyReferences()) {
                    final List<String> keyFullIds = out.ref(getStrategy().getFullJavaIdentifiers(uniqueKeys), 2);

                    if (scala) {
                        out.println();
                        out.println("override def getKeys: %s[ %s[%s] ] = %s.asList[ %s[%s] ]([[%s]])",
                            List.class, UniqueKey.class, recordType, Arrays.class, UniqueKey.class, recordType, keyFullIds);
                    }
                    else if (kotlin) {
                        out.println("override fun getKeys(): %s<%s<%s>> = listOf([[%s]])", out.ref(KLIST), UniqueKey.class, recordType, keyFullIds);
                    }
                    else {
                        out.overrideInherit();
                        out.println("public %s<%s<%s>> getKeys() {", List.class, UniqueKey.class, recordType);
                        out.println("return %s.<%s<%s>>asList([[%s]]);", Arrays.class, UniqueKey.class, recordType, keyFullIds);
                        out.println("}");
                    }
                }
                else {
                    String separator = "  ";

                    if (scala) {
                        out.println();
                        out.println("override def getKeys: %s[ %s[%s] ] = %s.asList[ %s[%s] ](",
                            List.class, UniqueKey.class, recordType, Arrays.class, UniqueKey.class, recordType);

                        for (UniqueKeyDefinition uniqueKey : uniqueKeys) {
                            out.print("%s", separator);
                            printCreateUniqueKey(out, uniqueKey);
                            out.println();
                            separator = ", ";
                        }

                        out.println(")");
                    }
                    else if (kotlin) {
                        out.println("override fun getKeys(): %s<%s<%s>> = listOf(", out.ref(KLIST), UniqueKey.class, recordType);

                        for (UniqueKeyDefinition uniqueKey : uniqueKeys) {
                            out.print("%s", separator);
                            printCreateUniqueKey(out, uniqueKey);
                            out.println();
                            separator = ", ";
                        }

                        out.println(")");
                    }
                    else {
                        out.overrideInherit();
                        out.println("public %s<%s<%s>> getKeys() {", List.class, UniqueKey.class, recordType);
                        out.println("return %s.<%s<%s>>asList(", Arrays.class, UniqueKey.class, recordType);

                        for (UniqueKeyDefinition uniqueKey : uniqueKeys) {
                            out.print("%s", separator);
                            printCreateUniqueKey(out, uniqueKey);
                            out.println();
                            separator = ", ";
                        }

                        out.println(");");
                        out.println("}");
                    }
                }
            }

            // Foreign keys
            List<ForeignKeyDefinition> foreignKeys = table.getForeignKeys();

            // [#7554] [#8028] Not yet supported with global key references turned off
            if (foreignKeys.size() > 0 && generateGlobalKeyReferences()) {
                final List<String> keyFullIds = out.ref(getStrategy().getFullJavaIdentifiers(foreignKeys), 2);

                if (scala) {
                    out.println();
                    out.println("override def getReferences: %s[ %s[%s, _] ] = %s.asList[ %s[%s, _] ]([[%s]])",
                        List.class, ForeignKey.class, recordType, Arrays.class, ForeignKey.class, recordType, keyFullIds);
                }
                else if (kotlin) {
                    out.println("override fun getReferences(): %s<%s<%s, *>> = listOf([[%s]])", out.ref(KLIST), ForeignKey.class, recordType, keyFullIds);
                }
                else {
                    out.overrideInherit();
                    out.println("public %s<%s<%s, ?>> getReferences() {", List.class, ForeignKey.class, recordType);
                    out.println("return %s.<%s<%s, ?>>asList([[%s]]);", Arrays.class, ForeignKey.class, recordType, keyFullIds);
                    out.println("}");
                }

                // Outbound (to-one) implicit join paths
                if (generateImplicitJoinPathsToOne()) {
                    for (ForeignKeyDefinition foreignKey : foreignKeys) {
                        final String keyFullId = out.ref(getStrategy().getFullJavaIdentifier(foreignKey), 2);
                        final String referencedTableClassName = out.ref(getStrategy().getFullJavaClassName(foreignKey.getReferencedTable()));
                        final String keyMethodName = out.ref(getStrategy().getJavaMethodName(foreignKey));

                        if (scala) {
                            out.println("def %s: %s = new %s(this, %s)", keyMethodName, referencedTableClassName, referencedTableClassName, keyFullId);
                        }
                        else if (kotlin) {
                            out.println("fun %s(): %s = %s(this, %s)", keyMethodName, referencedTableClassName, referencedTableClassName, keyFullId);
                        }
                        else {
                            out.println();
                            out.println("public %s %s() {", referencedTableClassName, keyMethodName);
                            out.println("return new %s(this, %s);", referencedTableClassName, keyFullId);
                            out.println("}");
                        }
                    }
                }
            }
        }

        List<CheckConstraintDefinition> cc = table.getCheckConstraints();

        if (!cc.isEmpty()) {
            if (scala) {
                out.println("override def getChecks: %s[ %s[%s] ] = %s.asList[ %s[%s] ](",
                    List.class, Check.class, recordType, Arrays.class, Check.class, recordType);
            }
            else if (kotlin) {
                out.println("override fun getChecks(): %s<%s<%s>> = listOf(",
                    out.ref(KLIST), Check.class, recordType);
            }
            else {
                out.overrideInherit();
                out.println("public %s<%s<%s>> getChecks() {", List.class, Check.class, recordType);
                out.println("return %s.<%s<%s>>asList(", Arrays.class, Check.class, recordType);
            }

            String separator = "  ";
            for (CheckConstraintDefinition c : cc) {
                out.println("%s%s.createCheck(this, %s.name(\"%s\"), \"%s\", %s)", separator, Internal.class, DSL.class, escapeString(c.getName()), escapeString(c.getCheckClause()), c.enforced());
                separator = ", ";
            }

            if (scala || kotlin) {
                out.println(")");
            }
            else {
                out.println(");");
                out.println("}");
            }
        }

        // [#1596] Updatable tables can provide fields for optimistic locking if properly configured.
        // [#7904] Records being updatable isn't a strict requirement. Version and timestamp values
        //         can still be generated
        versionLoop: for (String pattern : database.getRecordVersionFields()) {
            Pattern p = Pattern.compile(pattern, Pattern.COMMENTS);

            for (ColumnDefinition column : table.getColumns()) {
                if ((p.matcher(column.getName()).matches() ||
                     p.matcher(column.getQualifiedName()).matches())) {

                    final String columnTypeFull = getJavaType(column.getType(resolver()));
                    final String columnType = out.ref(columnTypeFull);
                    final String columnId = getStrategy().getJavaIdentifier(column);

                    if (scala) {
                        printDeprecationIfUnknownType(out, columnTypeFull);
                        out.println("override def getRecordVersion: %s[%s, %s] = %s", TableField.class, recordType, columnType, columnId);
                    }
                    else if (kotlin) {
                        printDeprecationIfUnknownType(out, columnTypeFull);
                        out.println("override fun getRecordVersion(): %s<%s, %s?> = %s", TableField.class, recordType, columnType, columnId);
                    }
                    else {
                        if (printDeprecationIfUnknownType(out, columnTypeFull))
                            out.override();
                        else
                            out.overrideInherit();

                        out.println("public %s<%s, %s> getRecordVersion() {", TableField.class, recordType, columnType);
                        out.println("return %s;", columnId);
                        out.println("}");
                    }

                    // Avoid generating this method twice
                    break versionLoop;
                }
            }
        }

        timestampLoop: for (String pattern : database.getRecordTimestampFields()) {
            Pattern p = Pattern.compile(pattern, Pattern.COMMENTS);

            for (ColumnDefinition column : table.getColumns()) {
                if ((p.matcher(column.getName()).matches() ||
                     p.matcher(column.getQualifiedName()).matches())) {

                    final String columnTypeFull = getJavaType(column.getType(resolver()));
                    final String columnType = out.ref(columnTypeFull);
                    final String columnId = getStrategy().getJavaIdentifier(column);

                    if (scala) {
                        printDeprecationIfUnknownType(out, columnTypeFull);
                        out.println("override def getRecordTimestamp: %s[%s, %s] = %s", TableField.class, recordType, columnType, columnId);
                    }
                    else if (kotlin) {
                        printDeprecationIfUnknownType(out, columnTypeFull);
                        out.println("override fun getRecordTimestamp(): %s<%s, %s?> = %s", TableField.class, recordType, columnType, columnId);
                    }
                    else {
                        if (printDeprecationIfUnknownType(out, columnTypeFull))
                            out.override();
                        else
                            out.overrideInherit();

                        out.println("public %s<%s, %s> getRecordTimestamp() {", TableField.class, recordType, columnType);
                        out.println("return %s;", columnId);
                        out.println("}");
                    }

                    // Avoid generating this method twice
                    break timestampLoop;
                }
            }
        }

        if (scala) {
            out.print("override def as(alias: %s): %s = ", String.class, className);

            if (table.isTableValuedFunction())
                out.println("new %s(%s.name(alias), null, null, this, parameters)", className, DSL.class);
            else
                out.println("new %s(%s.name(alias), this)", className, DSL.class);

            out.print("override def as(alias: %s): %s = ", Name.class, className);

            if (table.isTableValuedFunction())
                out.println("new %s(alias, null, null, this, parameters)", className);
            else
                out.println("new %s(alias, this)", className);
        }
        else if (kotlin) {
            out.print("override fun `as`(alias: %s): %s = ", String.class, className);

            if (table.isTableValuedFunction())
                out.println("%s(%s.name(alias), this, parameters)", className, DSL.class);
            else
                out.println("%s(%s.name(alias), this)", className, DSL.class);

            out.print("override fun `as`(alias: %s): %s = ", Name.class, className);

            if (table.isTableValuedFunction())
                out.println("%s(alias, this, parameters)", className);
            else
                out.println("%s(alias, this)", className);
        }

        // [#117] With instance fields, it makes sense to create a
        // type-safe table alias
        else if (generateInstanceFields()) {
            out.overrideInherit();
            out.println("public %s as(%s alias) {", className, String.class);

            if (table.isTableValuedFunction())
                out.println("return new %s(%s.name(alias), this, parameters);", className, DSL.class);
            else
                out.println("return new %s(%s.name(alias), this);", className, DSL.class);

            out.println("}");


            out.overrideInherit();
            out.println("public %s as(%s alias) {", className, Name.class);

            if (table.isTableValuedFunction())
                out.println("return new %s(alias, this, parameters);", className);
            else
                out.println("return new %s(alias, this);", className);

            out.println("}");
        }

        if (scala) {
            out.javadoc("Rename this table");
            out.print("override def rename(name: %s): %s = ", String.class, className);

            if (table.isTableValuedFunction())
                out.println("new %s(%s.name(name), null, null, null, parameters)", className, DSL.class);
            else
                out.println("new %s(%s.name(name), null)", className, DSL.class);

            out.javadoc("Rename this table");
            out.print("override def rename(name: %s): %s = ", Name.class, className);

            if (table.isTableValuedFunction())
                out.println("new %s(name, null, null, null, parameters)", className);
            else
                out.println("new %s(name, null)", className);
        }

        else if (kotlin) {
            out.javadoc("Rename this table");
            out.print("override fun rename(name: %s): %s = ", String.class, className);

            if (table.isTableValuedFunction())
                out.println("%s(%s.name(name), null, parameters)", className, DSL.class);
            else
                out.println("%s(%s.name(name), null)", className, DSL.class);

            out.javadoc("Rename this table");
            out.print("override fun rename(name: %s): %s = ", Name.class, className);

            if (table.isTableValuedFunction())
                out.println("%s(name, null, parameters)", className);
            else
                out.println("%s(name, null)", className);
        }

        // [#2921] With instance fields, tables can be renamed.
        else if (generateInstanceFields()) {
            out.javadoc("Rename this table");
            out.override();
            out.println("public %s rename(%s name) {", className, String.class);

            if (table.isTableValuedFunction())
                out.println("return new %s(%s.name(name), null, parameters);", className, DSL.class);
            else
                out.println("return new %s(%s.name(name), null);", className, DSL.class);

            out.println("}");

            out.javadoc("Rename this table");
            out.override();
            out.println("public %s rename(%s name) {", className, Name.class);

            if (table.isTableValuedFunction())
                out.println("return new %s(name, null, parameters);", className);
            else
                out.println("return new %s(name, null);", className);

            out.println("}");
        }

        // [#7809] fieldsRow()
        int degree = table.getColumns().size();
        String rowType = refRowType(out, table.getColumns());

        if (generateRecordsImplementingRecordN() && degree > 0 && degree <= Constants.MAX_ROW_DEGREE) {
            final String rowNType = out.ref(Row.class.getName() + degree);

            out.header("Row%s type methods", degree);
            if (scala) {
                out.println("override def fieldsRow: %s[%s] = super.fieldsRow.asInstanceOf[ %s[%s] ]", rowNType, rowType, rowNType, rowType);
            }
            else if (kotlin) {
                out.println("override fun fieldsRow(): %s<%s> = super.fieldsRow() as %s<%s>", rowNType, rowType, rowNType, rowType);
            }
            else {
                out.overrideInherit();
                out.println("public %s<%s> fieldsRow() {", rowNType, rowType);
                out.println("return (%s) super.fieldsRow();", rowNType);
                out.println("}");
            }
        }

        // [#1070] Table-valued functions should generate an additional set of call() methods
        if (table.isTableValuedFunction()) {
            for (boolean parametersAsField : new boolean[] { false, true }) {

                // Don't overload no-args call() methods
                if (parametersAsField && parameters.size() == 0)
                    break;

                out.javadoc("Call this table-valued function");

                if (scala) {
                    out.print("def call(").printlnIf(!parameters.isEmpty());
                    printParameterDeclarations(out, parameters, parametersAsField, "  ");
                    out.print("): %s = ", className);

                    out.print("Option(new %s(%s.name(\"%s\"), null, null, null, %s(", className, DSL.class, escapeString(table.getOutputName()), out.ref("scala.Array")).printlnIf(!parameters.isEmpty());
                    String separator = "  ";
                    for (ParameterDefinition parameter : parameters) {
                        final String paramArgName = getStrategy().getJavaMemberName(parameter);
                        final String paramTypeRef = getJavaTypeReference(parameter.getDatabase(), parameter.getType(resolver()));
                        final List<String> converter = out.ref(list(parameter.getType(resolver()).getConverter()));
                        final List<String> binding = out.ref(list(parameter.getType(resolver()).getBinding()));

                        out.print(separator);

                        if (parametersAsField)
                            out.println("%s", paramArgName);
                        else
                            out.println("%s.value(%s, %s" + converterTemplateForTableValuedFunction(converter) + converterTemplateForTableValuedFunction(binding) + ")", DSL.class, paramArgName, paramTypeRef, converter, binding);

                        separator = ", ";
                    }

                    out.println("))).map(r => if (aliased()) r.as(getUnqualifiedName) else r).get");
                }
                else if (kotlin) {
                    out.print("fun call(").printlnIf(!parameters.isEmpty());
                    printParameterDeclarations(out, parameters, parametersAsField, "  ");
                    out.print("): %s = %s(%s.name(\"%s\"), null, arrayOf(", className, className, DSL.class, escapeString(table.getOutputName()), Field.class).printlnIf(!parameters.isEmpty());

                    String separator = "  ";
                    for (ParameterDefinition parameter : parameters) {
                        final String paramArgName = getStrategy().getJavaMemberName(parameter);
                        final String paramTypeRef = getJavaTypeReference(parameter.getDatabase(), parameter.getType(resolver()));
                        final List<String> converter = out.ref(list(parameter.getType(resolver()).getConverter()));
                        final List<String> binding = out.ref(list(parameter.getType(resolver()).getBinding()));

                        if (parametersAsField)
                            out.println("%s%s", separator, paramArgName);
                        else
                            out.println("%s%s.value(%s, %s" + converterTemplateForTableValuedFunction(converter) + converterTemplateForTableValuedFunction(binding) + ")", separator, DSL.class, paramArgName, paramTypeRef, converter, binding);

                        separator = ", ";
                    }

                    out.println(")).let { if (aliased()) it.`as`(unqualifiedName) else it }");
                }
                else {
                    out.print("public %s call(", className).printlnIf(!parameters.isEmpty());
                    printParameterDeclarations(out, parameters, parametersAsField, "  ");
                    out.println(") {");

                    out.print("%s result = new %s(%s.name(\"%s\"), null, new %s[] {", className, className, DSL.class, escapeString(table.getOutputName()), Field.class).printlnIf(!parameters.isEmpty());
                    String separator = "  ";
                    for (ParameterDefinition parameter : parameters) {
                        final String paramArgName = getStrategy().getJavaMemberName(parameter);
                        final String paramTypeRef = getJavaTypeReference(parameter.getDatabase(), parameter.getType(resolver()));
                        final List<String> converter = out.ref(list(parameter.getType(resolver()).getConverter()));
                        final List<String> binding = out.ref(list(parameter.getType(resolver()).getBinding()));

                        out.print(separator);

                        if (parametersAsField)
                            out.println("%s", paramArgName);
                        else
                            out.println("%s.val(%s, %s" + converterTemplateForTableValuedFunction(converter) + converterTemplateForTableValuedFunction(binding) + ")", DSL.class, paramArgName, paramTypeRef, converter, binding);

                        separator = ", ";
                    }

                    out.println("});");
                    out.println();
                    out.println("return aliased() ? result.as(getUnqualifiedName()) : result;");
                    out.println("}");
                }
            }
        }

        generateTableClassFooter(table, out);
        out.println("}");
        closeJavaWriter(out);
    }

    protected void generateEmbeddables(SchemaDefinition schema) {
        log.info("Generating embeddables");

        for (EmbeddableDefinition embeddable : database.getEmbeddables(schema)) {
            try {
                generateEmbeddable(schema, embeddable);
            }
            catch (Exception e) {
                log.error("Error while generating embeddable " + embeddable, e);
            }
        }

        watch.splitInfo("Tables generated");
    }

    @SuppressWarnings("unused")
    protected void generateEmbeddable(SchemaDefinition schema, EmbeddableDefinition embeddable) {
        JavaWriter out = newJavaWriter(getFile(embeddable, Mode.RECORD));
        generateRecord0(embeddable, out);
        closeJavaWriter(out);
    }

    private String converterTemplate(List<String> converter) {
        if (converter == null || converter.isEmpty())
            return "[[]]";
        if (converter.size() > 1)
            throw new IllegalArgumentException();
        switch (GenerationUtil.expressionType(converter.get(0))) {
            case CONSTRUCTOR_REFERENCE:
                return "[[before=, ][new %s()]]";
            case EXPRESSION:
                return "[[before=, ][%s]]";
            default:
                throw new IllegalArgumentException();
        }
    }

    private String converterTemplateForTableValuedFunction(List<String> converter) {
        if (converter == null || converter.isEmpty())
            return "[[]]";
        if (converter.size() > 1)
            throw new IllegalArgumentException();
        switch (GenerationUtil.expressionType(converter.get(0))) {
            case CONSTRUCTOR_REFERENCE:
                return "[[before=.asConvertedDataType(][after=)][new %s()]]";
            case EXPRESSION:
                return "[[before=.asConvertedDataType(][after=)][%s]]";
            default:
                throw new IllegalArgumentException();
        }
    }

    private String escapeString(String comment) {

        if (comment == null)
            return null;

        // [#3450] Escape also the escape sequence, among other things that break Java strings.
        String result = comment.replace("\\", "\\\\")
                               .replace("\"", "\\\"")
                               .replace("\n", "\\n")
                               .replace("\r", "\\r");

        // [#10007] [#10318] Very long strings cannot be handled by the javac compiler.
        int max = 16384;
        if (result.length() <= max)
            return result;

        StringBuilder sb = new StringBuilder("\" + \"");
        for (int i = 0; i < result.length(); i += max) {
            if (i > 0)
                sb.append("\".toString() + \"");

            sb.append(result.substring(i, Math.min(i + max, result.length())));
        }

        return sb.append("\".toString() + \"").toString();
    }

    /**
     * Subclasses may override this method to provide table class footer code.
     */
    @SuppressWarnings("unused")
    protected void generateTableClassFooter(TableDefinition table, JavaWriter out) {}

    /**
     * Subclasses may override this method to provide their own Javadoc.
     */
    protected void generateTableClassJavadoc(TableDefinition table, JavaWriter out) {
        if (generateCommentsOnTables())
            printClassJavadoc(out, table);
        else
            printClassJavadoc(out, "The table <code>" + table.getQualifiedInputName() + "</code>.");
    }

    protected void generateSequences(SchemaDefinition schema) {
        log.info("Generating sequences");
        JavaWriter out = newJavaWriter(new File(getFile(schema).getParentFile(), "Sequences.java"));

        printPackage(out, schema);
        printClassJavadoc(out, "Convenience access to all sequences in " + schemaNameOrDefault(schema) + ".");
        printClassAnnotations(out, schema, Mode.DEFAULT);

        if (scala || kotlin)
            out.println("object Sequences {");
        else
            out.println("public class Sequences {");

        boolean qualifySequenceClassReferences = containsConflictingDefinition(schema, database.getSequences(schema));

        for (SequenceDefinition sequence : database.getSequences(schema)) {
            final String seqTypeFull = getJavaType(sequence.getType(resolver()));
            final String seqType = out.ref(seqTypeFull);
            final String seqId = getStrategy().getJavaIdentifier(sequence);
            final String seqName = sequence.getOutputName();
            final String schemaId = qualifySequenceClassReferences ? getStrategy().getFullJavaIdentifier(schema)
                : out.ref(getStrategy().getFullJavaIdentifier(schema), 2);
            final String typeRef = getJavaTypeReference(sequence.getDatabase(), sequence.getType(resolver()));

            if (!printDeprecationIfUnknownType(out, seqTypeFull))
                out.javadoc("The sequence <code>%s</code>", sequence.getQualifiedOutputName());

            boolean flags = generateSequenceFlags();

            if (scala)
                out.println("val %s: %s[%s] = %s.createSequence(\"%s\", %s, %s, %s, %s, %s, %s, %s, %s)",
                    seqId,
                    Sequence.class,
                    seqType,
                    Internal.class,
                    seqName,
                    schemaId,
                    typeRef,
                    flags ? numberLiteral(sequence.getStartWith()) : "null",
                    flags ? numberLiteral(sequence.getIncrementBy()) : "null",
                    flags ? numberLiteral(sequence.getMinvalue()) : "null",
                    flags ? numberLiteral(sequence.getMaxvalue()) : "null",
                    flags && sequence.getCycle(),
                    flags ? numberLiteral(sequence.getCache()) : "null"
                );
            else if (kotlin)
                out.println("val %s: %s<%s> = %s.createSequence(\"%s\", %s, %s, %s, %s, %s, %s, %s, %s)",
                    seqId,
                    Sequence.class,
                    seqType,
                    Internal.class,
                    seqName,
                    schemaId,
                    typeRef,
                    flags ? numberLiteral(sequence.getStartWith()) : "null",
                    flags ? numberLiteral(sequence.getIncrementBy()) : "null",
                    flags ? numberLiteral(sequence.getMinvalue()) : "null",
                    flags ? numberLiteral(sequence.getMaxvalue()) : "null",
                    flags && sequence.getCycle(),
                    flags ? numberLiteral(sequence.getCache()) : "null"
                );
            else
                out.println("public static final %s<%s> %s = %s.createSequence(\"%s\", %s, %s, %s, %s, %s, %s, %s, %s);",
                    Sequence.class,
                    seqType,
                    seqId,
                    Internal.class,
                    seqName,
                    schemaId,
                    typeRef,
                    flags ? numberLiteral(sequence.getStartWith()) : "null",
                    flags ? numberLiteral(sequence.getIncrementBy()) : "null",
                    flags ? numberLiteral(sequence.getMinvalue()) : "null",
                    flags ? numberLiteral(sequence.getMaxvalue()) : "null",
                    flags && sequence.getCycle(),
                    flags ? numberLiteral(sequence.getCache()) : "null"
                );
        }

        out.println("}");
        closeJavaWriter(out);

        watch.splitInfo("Sequences generated");
    }

    private String numberLiteral(Number n) {
        if (n instanceof BigInteger) {
            BigInteger bi = (BigInteger) n;
            int bitLength = ((BigInteger) n).bitLength();
            if (bitLength > Long.SIZE - 1)
                return "new java.math.BigInteger(\"" + bi.toString() + "\")";
            else if (bitLength > Integer.SIZE - 1)
                return Long.toString(n.longValue()) + 'L';
            else
                return Integer.toString(n.intValue());
        }
        else if (n instanceof Integer || n instanceof Short || n instanceof Byte)
            return Integer.toString(n.intValue());
        else if (n != null)
            return Long.toString(n.longValue()) + 'L';
        return "null";
    }

    private boolean containsConflictingDefinition(SchemaDefinition schema, List<? extends Definition> definitions) {
        final String unqualifiedSchemaId = getStrategy().getJavaIdentifier(schema);
        for (Definition def : definitions)
            if (unqualifiedSchemaId.equals(getStrategy().getJavaIdentifier(def)))
                return true;
        return false;
    }

    protected void generateCatalog(CatalogDefinition catalog) {
        JavaWriter out = newJavaWriter(getFile(catalog));
        log.info("");
        log.info("Generating catalog", out.file().getName());
        log.info("==========================================================");
        generateCatalog(catalog, out);
        closeJavaWriter(out);
    }

    protected void generateCatalog(CatalogDefinition catalog, JavaWriter out) {
        final String catalogId = getStrategy().getJavaIdentifier(catalog);
        final String catalogName = !catalog.getQualifiedOutputName().isEmpty() ? catalog.getQualifiedOutputName() : catalogId;
        final String className = getStrategy().getJavaClassName(catalog);
        final List<String> interfaces = out.ref(getStrategy().getJavaClassImplements(catalog, Mode.DEFAULT));

        printPackage(out, catalog);

        if (scala) {
            out.println("object %s {", className);
            out.javadoc("The reference instance of <code>%s</code>", catalogName);
            out.println("val %s = new %s", catalogId, className);
            out.println("}");
            out.println();
        }

        generateCatalogClassJavadoc(catalog, out);
        printClassAnnotations(out, catalog, Mode.DEFAULT);

        if (scala) {
            out.println("class %s extends %s(\"%s\")[[before= with ][separator= with ][%s]] {", className, CatalogImpl.class, catalog.getOutputName(), interfaces);
        }
        else if (kotlin) {
            out.println("class %s : %s(\"%s\")[[before=, ][%s]] {", className, CatalogImpl.class, catalog.getOutputName(), interfaces);

            out.println("companion object {");
            out.javadoc("The reference instance of <code>%s</code>", catalogName);
            out.println("val %s = %s()", catalogId, className);
            out.println("}");
        }
        else {
            out.println("public class %s extends %s[[before= implements ][%s]] {", className, CatalogImpl.class, interfaces);
            out.printSerial();
            out.javadoc("The reference instance of <code>%s</code>", catalogName);
            out.println("public static final %s %s = new %s();", className, catalogId, className);
        }

        List<SchemaDefinition> schemas = new ArrayList<>();
        if (generateGlobalSchemaReferences()) {
            Set<String> fieldNames = new HashSet<>();
            fieldNames.add(catalogId);
            for (SchemaDefinition schema : catalog.getSchemata())
                if (generateSchemaIfEmpty(schema))
                    fieldNames.add(getStrategy().getJavaIdentifier(schema));

            for (SchemaDefinition schema : catalog.getSchemata()) {
                if (generateSchemaIfEmpty(schema)) {
                    schemas.add(schema);

                    final String schemaClassName = out.ref(getStrategy().getFullJavaClassName(schema));
                    final String schemaId = getStrategy().getJavaIdentifier(schema);
                    final String schemaFullId = getStrategy().getFullJavaIdentifier(schema);
                    String schemaShortId = out.ref(getStrategy().getFullJavaIdentifier(schema), 2);
                    if (fieldNames.contains(schemaShortId.substring(0, schemaShortId.indexOf('.'))))
                        schemaShortId = schemaFullId;
                    final String schemaComment = escapeEntities(comment(schema));

                    out.javadoc(isBlank(schemaComment) ? ("The schema <code>" + (!schema.getQualifiedOutputName().isEmpty() ? schema.getQualifiedOutputName() : schemaId) + "</code>.") : schemaComment);

                    if (scala)
                        out.println("val %s = %s", schemaId, schemaShortId);
                    else if (kotlin)
                        out.println("val %s get() = %s", schemaId, schemaShortId);
                    else
                        out.println("public final %s %s = %s;", schemaClassName, schemaId, schemaShortId);
                }
            }
        }

        if (scala || kotlin)
            ;
        else {
            out.javadoc(NO_FURTHER_INSTANCES_ALLOWED);
            out.println("private %s() {", className);
            out.println("super(\"%s\");", catalog.getOutputName());
            out.println("}");
        }

        printReferences(out, schemas, Schema.class, false);

        generateCatalogClassFooter(catalog, out);
        out.println("}");
    }

    /**
     * Subclasses may override this method to provide catalog class footer code.
     */
    @SuppressWarnings("unused")
    protected void generateCatalogClassFooter(CatalogDefinition schema, JavaWriter out) {}

    /**
     * Subclasses may override this method to provide their own Javadoc.
     */
    protected void generateCatalogClassJavadoc(CatalogDefinition catalog, JavaWriter out) {
        if (generateCommentsOnCatalogs())
            printClassJavadoc(out, catalog);
        else
            printClassJavadoc(out, "The catalog <code>" + catalog.getQualifiedInputName() + "</code>.");
    }

    protected void generateSchema(SchemaDefinition schema) {
        JavaWriter out = newJavaWriter(getFile(schema));
        log.info("Generating schema", out.file().getName());
        log.info("----------------------------------------------------------");
        generateSchema(schema, out);
        closeJavaWriter(out);
    }

    protected void generateSchema(SchemaDefinition schema, JavaWriter out) {
        final String catalogId = out.ref(getStrategy().getFullJavaIdentifier(schema.getCatalog()), 2);
        final String schemaId = getStrategy().getJavaIdentifier(schema);
        final String schemaName = !schema.getQualifiedOutputName().isEmpty() ? schema.getQualifiedOutputName() : schemaId;
        final String className = getStrategy().getJavaClassName(schema);
        final List<String> interfaces = out.ref(getStrategy().getJavaClassImplements(schema, Mode.DEFAULT));

        printPackage(out, schema);

        if (scala) {
            out.println("object %s {", className);
            out.javadoc("The reference instance of <code>%s</code>", schemaName);
            out.println("val %s = new %s", schemaId, className);
            out.println("}");
            out.println();
        }

        generateSchemaClassJavadoc(schema, out);
        printClassAnnotations(out, schema, Mode.DEFAULT);

        if (scala) {
            out.println("class %s extends %s(\"%s\", %s)[[before= with ][separator= with ][%s]] {", className, SchemaImpl.class, schema.getOutputName(), catalogId, interfaces);
        }
        else if (kotlin) {
            out.println("class %s : %s(\"%s\", %s)[[before=, ][%s]] {", className, SchemaImpl.class, schema.getOutputName(), catalogId, interfaces);

            out.println("companion object {");
            out.javadoc("The reference instance of <code>%s</code>", schemaName);
            out.println("val %s = %s()", schemaId, className);
            out.println("}");
        }
        else {
            out.println("public class %s extends %s[[before= implements ][%s]] {", className, SchemaImpl.class, interfaces);
            out.printSerial();
            out.javadoc("The reference instance of <code>%s</code>", schemaName);
            out.println("public static final %s %s = new %s();", className, schemaId, className);
        }

        if (generateGlobalTableReferences()) {
            Set<String> memberNames = getMemberNames(schema);

            for (TableDefinition table : schema.getTables()) {

                // [#10191] In Scala, methods and attributes are in the same
                //          namespace because parentheses can be omitted. This
                //          means that parameter-less table valued functions
                //          produce a clash if we generate both the global table
                //          reference in the schema, and the function call
                if (scala && table.isTableValuedFunction() && table.getParameters().isEmpty())
                    continue;

                final String tableClassName = out.ref(getStrategy().getFullJavaClassName(table));
                final String tableId = getStrategy().getJavaIdentifier(table);
                final String tableShortId = getShortId(out, memberNames, table);
                final String tableComment = escapeEntities(comment(table));

                out.javadoc(isBlank(tableComment) ? "The table <code>" + table.getQualifiedOutputName() + "</code>." : tableComment);

                if (scala)
                    out.println("val %s = %s", tableId, tableShortId);
                else if (kotlin)
                    out.println("val %s get() = %s", tableId, tableShortId);
                else
                    out.println("public final %s %s = %s;", tableClassName, tableId, tableShortId);

                // [#3797] Table-valued functions generate two different literals in
                // globalObjectReferences
                if (table.isTableValuedFunction())
                    printTableValuedFunction(out, table, getStrategy().getJavaIdentifier(table));
            }
        }

        if (!scala && !kotlin) {
            out.javadoc(NO_FURTHER_INSTANCES_ALLOWED);
            out.println("private %s() {", className);
            out.println("super(\"%s\", null);", schema.getOutputName());
            out.println("}");
        }

        out.println();
        if (scala) {
            out.println("override def getCatalog: %s = %s", Catalog.class, catalogId);
        }
        else if (kotlin) {
            out.println("override fun getCatalog(): %s = %s", Catalog.class, catalogId);
        }
        else {
            out.overrideInherit();
            out.println("public %s getCatalog() {", Catalog.class);
            out.println("return %s;", catalogId);
            out.println("}");
        }

        // [#2255] Avoid referencing sequence literals, if they're not generated
        if (generateGlobalSequenceReferences())
            printReferences(out, database.getSequences(schema), Sequence.class, true);

        // [#9685] Avoid referencing table literals if they're not generated
        if (generateTables())
            printReferences(out, database.getTables(schema), Table.class, true);

        // [#9685] Avoid referencing UDT literals if they're not generated
        if (generateUDTs())
            printReferences(out, database.getUDTs(schema), UDT.class, true);

        generateSchemaClassFooter(schema, out);
        out.println("}");
    }

    private Set<String> getMemberNames(CatalogDefinition catalog) {
        Set<String> members = new HashSet<>();
        members.add(getStrategy().getJavaIdentifier(catalog));

        for (SchemaDefinition table : catalog.getSchemata())
            members.add(getStrategy().getJavaIdentifier(table));

        return members;
    }

    private Set<String> getMemberNames(SchemaDefinition schema) {
        Set<String> members = new HashSet<>();
        members.add(getStrategy().getJavaIdentifier(schema));

        for (TableDefinition table : schema.getTables())
            members.add(getStrategy().getJavaIdentifier(table));

        return members;
    }

    private String getShortId(JavaWriter out, Set<String> memberNames, Definition table) {
        String shortId = out.ref(getStrategy().getFullJavaIdentifier(table), 2);

        if (memberNames.contains(shortId.substring(0, shortId.indexOf('.'))))
            shortId = getStrategy().getFullJavaIdentifier(table);

        return shortId;
    }

    /**
     * Subclasses may override this method to provide schema class footer code.
     */
    @SuppressWarnings("unused")
    protected void generateSchemaClassFooter(SchemaDefinition schema, JavaWriter out) {}

    /**
     * Subclasses may override this method to provide their own Javadoc.
     */
    protected void generateSchemaClassJavadoc(SchemaDefinition schema, JavaWriter out) {
        if (generateCommentsOnSchemas())
            printClassJavadoc(out, schema);
        else
            printClassJavadoc(out, "The schema <code>" + schema.getQualifiedInputName() + "</code>.");
    }

    protected void printFromAndInto(JavaWriter out, TableDefinition table) {
        printFromAndInto(out, table, Mode.DEFAULT);
    }

    private void printFromAndInto(JavaWriter out, Definition tableOrUDT, Mode mode) {
        String qualified = out.ref(getStrategy().getFullJavaClassName(tableOrUDT, Mode.INTERFACE));

        out.header("FROM and INTO");
        boolean override = generateInterfaces() && !generateImmutableInterfaces();

        if (scala) {
            out.println();
            out.println("%sdef from(from: %s) {", (override ? "override " : ""), qualified);
        }
        else if (kotlin) {
            out.println();
            out.println("%sfun from(from: %s) {", (override ? "override " : ""), qualified);
        }
        else {
            out.overrideInheritIf(override);
            out.println("public void from(%s from) {", qualified);
        }

        for (TypedElementDefinition<?> column : getTypedElements(tableOrUDT)) {
            final String setter = getStrategy().getJavaSetterName(column, Mode.INTERFACE);
            final String getter = getStrategy().getJavaGetterName(column, Mode.INTERFACE);

            // TODO: Use appropriate Mode here
            final String member = getStrategy().getJavaMemberName(column, Mode.POJO);

            if (scala)
                out.println("%s(from.%s)", setter, getter);
            else if (kotlin)
                out.println("%s = from.%s", member, member);
            else
                out.println("%s(from.%s());", setter, getter);
        }

        out.println("}");

        if (override) {
            // [#10191] Java and Kotlin can produce overloads for this method despite
            // generic type erasure, but Scala cannot, see
            // https://twitter.com/lukaseder/status/1262652304773259264

            if (scala) {
                if (mode != Mode.POJO) {
                    out.println();
                    out.println("override def into [E](into: E): E = {", qualified);
                    out.println("if (into.isInstanceOf[%s])", qualified);
                    out.println("into.asInstanceOf[%s].from(this)", qualified);
                    out.println("else");
                    out.println("super.into(into)");
                    out.println("into");
                    out.println("}");
                }
            }
            else if (kotlin) {
                out.println();
                out.println("%sfun <E : %s> into(into: E): E {", (override ? "override " : ""), qualified);
                out.println("into.from(this)");
                out.println("return into");
                out.println("}");
            }
            else {
                out.overrideInherit();
                out.println("public <E extends %s> E into(E into) {", qualified);
                out.println("into.from(this);");
                out.println("return into;");
                out.println("}");
            }
        }
    }

    protected void printReferences(JavaWriter out, List<? extends Definition> definitions, Class<?> type, boolean isGeneric) {
        if (out != null && !definitions.isEmpty()) {
            final String generic = isGeneric ? (scala ? "[_]" : kotlin ? "<*>" : "<?>") : "";
            final List<String> references = new ArrayList<>();
            final Definition first = definitions.get(0);

            // [#6248] We cannot use the members in this class because:
            //         - They are not always available (global object references)
            //         - They may not have been initialised yet!
            //         Referencing the singleton identifier with 2 identifier segments
            //         doesn't work in Kotlin if the identifier conflicts with the
            //         members in this class. Java can resolve the ambiguity.
            if ((scala || kotlin) && first instanceof TableDefinition) {
                final Set<String> memberNames = getMemberNames(first.getSchema());

                for (Definition table : definitions)
                    references.add(getShortId(out, memberNames, table));
            }
            else if ((scala || kotlin) && first instanceof SchemaDefinition) {
                final Set<String> memberNames = getMemberNames(first.getCatalog());

                for (Definition schema : definitions)
                    references.add(getShortId(out, memberNames, schema));
            }
            else {
                references.addAll(out.ref(getStrategy().getFullJavaIdentifiers(definitions), 2));
            }
            out.println();

            if (scala) {
                out.println("override def get%ss: %s[%s%s] = {", type.getSimpleName(), List.class, type, generic);

                if (definitions.size() > INITIALISER_SIZE) {
                    out.println("val result = new %s[%s%s]", ArrayList.class, type, generic);

                    for (int i = 0; i < definitions.size(); i += INITIALISER_SIZE) {
                        out.println("result.addAll(get%ss%s)", type.getSimpleName(), i / INITIALISER_SIZE);
                    }

                    out.println("result");
                }
                else {
                    out.println("return %s.asList[%s%s]([[before=\n\t\t\t][separator=,\n\t\t\t][%s]])", Arrays.class, type, generic, references);
                }

                out.println("}");
            }
            else if (kotlin) {
                if (definitions.size() > INITIALISER_SIZE) {
                    out.println("override fun get%ss(): %s<%s%s> {", type.getSimpleName(), out.ref(KLIST), type, generic);
                    out.println("val result = mutableListOf<%s%s>()", type, generic);

                    for (int i = 0; i < definitions.size(); i += INITIALISER_SIZE) {
                        out.println("result.addAll(get%ss%s())", type.getSimpleName(), i / INITIALISER_SIZE);
                    }

                    out.println("return result");
                    out.println("}");
                }
                else {
                    out.println("override fun get%ss(): %s<%s%s> = listOf(", type.getSimpleName(), out.ref(KLIST), type, generic);
                    out.println("[[separator=,\n\t\t][%s]]", references);
                    out.println(")");
                }
            }
            else {
                out.override();
                out.println("public final %s<%s%s> get%ss() {", List.class, type, generic, type.getSimpleName());

                if (definitions.size() > INITIALISER_SIZE) {
                    out.println("%s result = new %s();", List.class, ArrayList.class);

                    for (int i = 0; i < definitions.size(); i += INITIALISER_SIZE) {
                        out.println("result.addAll(get%ss%s());", type.getSimpleName(), i / INITIALISER_SIZE);
                    }

                    out.println("return result;");
                }
                else {
                    out.println("return %s.<%s%s>asList([[before=\n\t\t\t][separator=,\n\t\t\t][%s]]);", Arrays.class, type, generic, references);
                }

                out.println("}");
            }

            if (definitions.size() > INITIALISER_SIZE) {
                for (int i = 0; i < definitions.size(); i += INITIALISER_SIZE) {
                    out.println();

                    if (scala) {
                        out.println("private def get%ss%s(): %s[%s%s] = %s.asList[%s%s](", type.getSimpleName(), i / INITIALISER_SIZE, List.class, type, generic, Arrays.class, type, generic);
                        out.println("[[before=\n\t\t\t][separator=,\n\t\t\t][%s]]", references.subList(i, Math.min(i + INITIALISER_SIZE, references.size())));
                        out.println(")");
                    }
                    else if (kotlin) {
                        out.println("private fun get%ss%s(): %s<%s%s> = listOf(", type.getSimpleName(), i / INITIALISER_SIZE, out.ref(KLIST), type, generic);
                        out.println("[[before=\t][separator=,\n\t\t\t][%s]]", references.subList(i, Math.min(i + INITIALISER_SIZE, references.size())));
                        out.println(")");
                    }
                    else {
                        out.println("private final %s<%s%s> get%ss%s() {", List.class, type, generic, type.getSimpleName(), i / INITIALISER_SIZE);
                        out.println("return %s.<%s%s>asList([[before=\n\t\t\t][separator=,\n\t\t\t][%s]]);", Arrays.class, type, generic, references.subList(i, Math.min(i + INITIALISER_SIZE, references.size())));
                        out.println("}");
                    }
                }
            }
        }
    }

    protected void printTableJPAAnnotation(JavaWriter out, TableDefinition table) {
        SchemaDefinition schema = table.getSchema();
        int indent = out.indent();

        if (generateJPAAnnotations()) {
            // Since JPA 1.0
            out.println("@%s", out.ref("javax.persistence.Entity"));

            // Since JPA 1.0
            out.print("@%s(name = \"", out.ref("javax.persistence.Table"));
            out.print(escapeString(table.getName()));
            out.print("\"");

            if (!schema.isDefaultSchema()) {
                out.print(", schema = \"");
                out.print(escapeString(schema.getOutputName()));
                out.print("\"");
            }

            StringBuilder sb1 = new StringBuilder();
            String glue1 = generateNewline();

            for (UniqueKeyDefinition uk : table.getUniqueKeys()) {
                sb1.append(glue1);
                sb1.append(out.tabString())
                   .append(scala ? "new " : "@")

                   // Since JPA 1.0
                   .append(out.ref("javax.persistence.UniqueConstraint"))
                   .append("(");

                if (!StringUtils.isBlank(uk.getOutputName()))
                    sb1.append("name = \"" + escapeString(uk.getOutputName()) + "\", ");

                sb1.append("columnNames = ")
                   .append(scala ? "Array(" : "{");

                String glue1Inner = "";
                for (ColumnDefinition column : uk.getKeyColumns()) {
                    sb1.append(glue1Inner);
                    sb1.append("\"");
                    sb1.append(escapeString(column.getName()));
                    sb1.append("\"");

                    glue1Inner = ", ";
                }

                sb1.append(scala ? ")" : "}").append(")");

                glue1 = "," + generateNewline();
            }

            if (sb1.length() > 0) {
                out.println(", uniqueConstraints = %s%s", (scala ? "Array(" : "{"), sb1.toString());
                out.print("%s", (scala ? ")" : "}"));
            }

            if (StringUtils.isBlank(generateJPAVersion()) || "2.1".compareTo(generateJPAVersion()) <= 0) {
                StringBuilder sb2 = new StringBuilder();
                String glue2 = generateNewline();

                for (IndexDefinition index : table.getIndexes()) {
                    sb2.append(glue2);
                    sb2.append(out.tabString())
                       .append(scala ? "new " : "@")
                       .append(out.ref("javax.persistence.Index"))
                       .append("(name = \"").append(escapeString(index.getOutputName())).append("\"");

                    if (index.isUnique())
                        sb2.append(", unique = true");

                    sb2.append(", columnList = \"");

                    String glue2Inner = "";
                    for (IndexColumnDefinition column : index.getIndexColumns()) {
                        sb2.append(glue2Inner)
                           .append(escapeString(column.getOutputName()));

                        if (column.getSortOrder() == SortOrder.ASC)
                            sb2.append(" ASC");
                        else if (column.getSortOrder() == SortOrder.DESC)
                            sb2.append(" DESC");

                        glue2Inner = ", ";
                    }

                    sb2.append("\")");
                    glue2 = "," + generateNewline();
                }

                if (sb2.length() > 0) {
                    out.println(", indexes = %s%s", (scala ? "Array(" : "{"), sb2.toString());
                    out.print("%s", (scala ? ")" : "}"));
                }
            }

            out.println(")");
        }

        // [#10196] The above logic triggers an indent level of -1, incorrectly
        out.indent(indent);
    }

    protected void printColumnJPAAnnotation(JavaWriter out, ColumnDefinition column) {
        int indent = out.indent();

        if (generateJPAAnnotations()) {
            UniqueKeyDefinition pk = column.getPrimaryKey();

            if (pk != null) {
                if (pk.getKeyColumns().size() == 1) {

                    // Since JPA 1.0
                    out.println("@%s", out.ref("javax.persistence.Id"));

                    // Since JPA 1.0
                    if (pk.getKeyColumns().get(0).isIdentity())
                        out.println("@%s(strategy = %s.IDENTITY)",
                            out.ref("javax.persistence.GeneratedValue"),
                            out.ref("javax.persistence.GenerationType"));
                }
            }

            String nullable = "";
            if (!column.getType(resolver()).isNullable())
                nullable = ", nullable = false";

            String length = "";
            String precision = "";
            String scale = "";

            if (column.getType(resolver()).getLength() > 0) {
                length = ", length = " + column.getType(resolver()).getLength();
            }
            else if (column.getType(resolver()).getPrecision() > 0) {
                precision = ", precision = " + column.getType(resolver()).getPrecision();

                if (column.getType(resolver()).getScale() > 0) {
                    scale = ", scale = " + column.getType(resolver()).getScale();
                }
            }

            // [#8535] The unique flag is not set on the column, but only on
            //         the table's @UniqueConstraint section.

            // Since JPA 1.0
            out.print("@%s(name = \"", out.ref("javax.persistence.Column"));
            out.print(escapeString(column.getName()));
            out.print("\"");
            out.print(nullable);
            out.print(length);
            out.print(precision);
            out.print(scale);
            out.println(")");
        }

        // [#10196] The above logic triggers an indent level of -1, incorrectly
        out.indent(indent);
    }

    /**
     * @deprecated - This method is no longer used by the generator.
     */
    @Deprecated
    protected void printColumnValidationAnnotation(JavaWriter out, ColumnDefinition column) {
        printValidationAnnotation(out, column);
    }

    private void printValidationAnnotation(JavaWriter out, TypedElementDefinition<?> column) {
        if (generateValidationAnnotations()) {
            DataTypeDefinition type = column.getType(resolver());

            // [#5128] defaulted columns are nullable in Java
            if (!column.getType(resolver()).isNullable() &&
                !column.getType(resolver()).isDefaulted() &&
                !column.getType(resolver()).isIdentity())
                out.println("@%s", out.ref("javax.validation.constraints.NotNull"));

            String javaType = getJavaType(type);
            if ("java.lang.String".equals(javaType) || "byte[]".equals(javaType)) {
                int length = type.getLength();

                if (length > 0)
                    out.println("@%s(max = %s)", out.ref("javax.validation.constraints.Size"), length);
            }
        }
    }

    private String nullableOrNonnullAnnotation(JavaWriter out, TypedElementDefinition<?> column) {
        return column.getType().isNullable() && generateNullableAnnotation()
             ? out.ref(generatedNullableAnnotationType())
             : !column.getType().isNullable() && generateNonnullAnnotation()
             ? out.ref(generatedNonnullAnnotationType())
             : null;
    }

    private void printNullableOrNonnullAnnotation(JavaWriter out, TypedElementDefinition<?> column) {
        if (column.getType().isNullable())
            printNullableAnnotation(out);
        else
            printNonnullAnnotation(out);
    }

    protected void printNullableAnnotation(JavaWriter out) {
        if (generateNullableAnnotation())
            out.println("@%s", out.ref(generatedNullableAnnotationType()));
    }

    protected void printNonnullAnnotation(JavaWriter out) {
        if (generateNonnullAnnotation())
            out.println("@%s", out.ref(generatedNonnullAnnotationType()));
    }

    private boolean printDeprecationIfUnknownTypes(JavaWriter out, Collection<? extends ParameterDefinition> params) {
        for (ParameterDefinition param : params)
            if (printDeprecationIfUnknownType(out, getJavaType(param.getType(resolver()))))
                return true;

        return false;
    }

    private boolean printDeprecationIfUnknownType(JavaWriter out, String type) {
        if (generateDeprecationOnUnknownTypes() && (Object.class.getName().equals(type) || kotlin && "Any".equals(type))) {
            out.javadoc("@deprecated Unknown data type. "
                + "Please define an explicit {@link org.jooq.Binding} to specify how this "
                + "type should be handled. Deprecation can be turned off using {@literal <deprecationOnUnknownTypes/>} "
                + "in your code generator configuration.");
            out.println("@java.lang.Deprecated");
            return true;
        }
        else {
            return false;
        }
    }

    @SuppressWarnings("unused")
    protected void generateRoutine(SchemaDefinition schema, RoutineDefinition routine) {
        JavaWriter out = newJavaWriter(getFile(routine));
        log.info("Generating routine", out.file().getName());
        generateRoutine(routine, out);
        closeJavaWriter(out);
    }

    protected void generateRoutine(RoutineDefinition routine, JavaWriter out) {
        final SchemaDefinition schema = routine.getSchema();
        final String className = getStrategy().getJavaClassName(routine);
        final String returnTypeFull = (routine.getReturnValue() == null)
            ? Void.class.getName()
            : getJavaType(routine.getReturnType(resolver()));
        final String returnType = (routine.getReturnValue() == null)
            ? Void.class.getName()
            : out.ref(returnTypeFull);
        final List<String> returnTypeRef = list((routine.getReturnValue() != null)
            ? getJavaTypeReference(database, routine.getReturnType(resolver()))
            : null);
        final List<String> returnConverter = out.ref(list(
             (routine.getReturnValue() != null)
            ? routine.getReturnType(resolver()).getConverter()
            : null));
        final List<String> returnBinding = out.ref(list(
             (routine.getReturnValue() != null)
            ? routine.getReturnType(resolver()).getBinding()
            : null));

        final List<String> interfaces = out.ref(getStrategy().getJavaClassImplements(routine, Mode.DEFAULT));
        final String schemaId = out.ref(getStrategy().getFullJavaIdentifier(schema), 2);
        final List<String> packageId = out.ref(getStrategy().getFullJavaIdentifiers(routine.getPackage()), 2);

        printPackage(out, routine);

        if (scala) {
            out.println("object %s {", className);
            for (ParameterDefinition parameter : routine.getAllParameters()) {
                final String paramTypeFull = getJavaType(parameter.getType(resolver()));
                final String paramType = out.ref(paramTypeFull);
                final String paramTypeRef = getJavaTypeReference(parameter.getDatabase(), parameter.getType(resolver()));
                final String paramId = out.ref(getStrategy().getJavaIdentifier(parameter), 2);
                final String paramName = parameter.getName();
                final String isDefaulted = parameter.isDefaulted() ? "true" : "false";
                final String isUnnamed = parameter.isUnnamed() ? "true" : "false";
                final List<String> converter = out.ref(list(parameter.getType(resolver()).getConverter()));
                final List<String> binding = out.ref(list(parameter.getType(resolver()).getBinding()));

                if (!printDeprecationIfUnknownType(out, paramTypeFull))
                    out.javadoc("The parameter <code>%s</code>.[[before= ][%s]]", parameter.getQualifiedOutputName(), list(escapeEntities(comment(parameter))));

                out.println("val %s: %s[%s] = %s.createParameter(\"%s\", %s, %s, %s" + converterTemplate(converter) + converterTemplate(binding) + ")",
                    paramId, Parameter.class, paramType, Internal.class, escapeString(paramName), paramTypeRef, isDefaulted, isUnnamed, converter, binding);
            }

            out.println("}");
            out.println();
        }

        if (!printDeprecationIfUnknownType(out, returnTypeFull))
            generateRoutineClassJavadoc(routine, out);

        printClassAnnotations(out, routine, Mode.DEFAULT);

        if (scala) {
            out.println("class %s extends %s[%s](\"%s\", %s[[before=, ][%s]][[before=, ][%s]]" + converterTemplate(returnConverter) + converterTemplate(returnBinding) + ")[[before= with ][separator= with ][%s]] {",
                className, AbstractRoutine.class, returnType, escapeString(routine.getName()), schemaId, packageId, returnTypeRef, returnConverter, returnBinding, interfaces);
        }
        else {
            if (kotlin) {
                out.println("class %s : %s<%s>(\"%s\", %s[[before=, ][%s]][[before=, ][%s]]" + converterTemplate(returnConverter) + converterTemplate(returnBinding) + ")[[before=, ][%s]] {",
                    className, AbstractRoutine.class, returnType, escapeString(routine.getName()), schemaId, packageId, returnTypeRef, returnConverter, returnBinding, interfaces);
            }
            else {
                out.println("public class %s extends %s<%s>[[before= implements ][%s]] {",
                    className, AbstractRoutine.class, returnType, interfaces);
                out.printSerial();
            }

            if (kotlin)
                out.println("companion object {");

            for (ParameterDefinition parameter : routine.getAllParameters()) {
                final String paramTypeFull = getJavaType(parameter.getType(resolver()));
                final String paramType = out.ref(paramTypeFull);
                final String paramTypeRef = getJavaTypeReference(parameter.getDatabase(), parameter.getType(resolver()));
                final String paramId = getStrategy().getJavaIdentifier(parameter);
                final String paramName = parameter.getName();
                final String isDefaulted = parameter.isDefaulted() ? "true" : "false";
                final String isUnnamed = parameter.isUnnamed() ? "true" : "false";
                final List<String> converter = out.ref(list(parameter.getType(resolver()).getConverter()));
                final List<String> binding = out.ref(list(parameter.getType(resolver()).getBinding()));

                if (!printDeprecationIfUnknownType(out, paramTypeFull))
                    out.javadoc("The parameter <code>%s</code>.[[before= ][%s]]", parameter.getQualifiedOutputName(), list(escapeEntities(comment(parameter))));

                if (kotlin)
                    out.println("val %s: %s<%s?> = %s.createParameter(\"%s\", %s, %s, %s" + converterTemplate(converter) + converterTemplate(binding) + ")",
                        paramId, Parameter.class, paramType, Internal.class, escapeString(paramName), paramTypeRef, isDefaulted, isUnnamed, converter, binding);
                else
                    out.println("public static final %s<%s> %s = %s.createParameter(\"%s\", %s, %s, %s" + converterTemplate(converter) + converterTemplate(binding) + ");",
                        Parameter.class, paramType, paramId, Internal.class, escapeString(paramName), paramTypeRef, isDefaulted, isUnnamed, converter, binding);
            }

            if (kotlin)
                out.println("}").println();
        }

        if (scala) {
            out.println("{");
        }
        else if (kotlin) {
            out.println("init {");
        }
        else {
            out.javadoc("Create a new routine call instance");
            out.println("public %s() {", className);
            out.println("super(\"%s\", %s[[before=, ][%s]][[before=, ][%s]]" + converterTemplate(returnConverter) + converterTemplate(returnBinding) + ");", routine.getName(), schemaId, packageId, returnTypeRef, returnConverter, returnBinding);


            if (routine.getAllParameters().size() > 0)
                out.println();
        }

        for (ParameterDefinition parameter : routine.getAllParameters()) {
            final String paramId = getStrategy().getJavaIdentifier(parameter);

            if (parameter.equals(routine.getReturnValue())) {
                if (parameter.isSynthetic()) {
                    if (scala)
                        out.println("setSyntheticReturnParameter(%s.%s)", className, paramId);
                    else if (kotlin)
                        out.println("syntheticReturnParameter = %s", paramId);
                    else
                        out.println("setSyntheticReturnParameter(%s);", paramId);
                }
                else {
                    if (scala)
                        out.println("setReturnParameter(%s.%s)", className, paramId);
                    else if (kotlin)
                        out.println("returnParameter = %s", paramId);
                    else
                        out.println("setReturnParameter(%s);", paramId);
                }
            }
            else if (routine.getInParameters().contains(parameter)) {
                if (routine.getOutParameters().contains(parameter)) {
                    if (scala)
                        out.println("addInOutParameter(%s.%s)", className, paramId);
                    else if (kotlin)
                        out.println("addInOutParameter(%s)", paramId);
                    else
                        out.println("addInOutParameter(%s);", paramId);
                }
                else {
                    if (scala)
                        out.println("addInParameter(%s.%s)", className, paramId);
                    else if (kotlin)
                        out.println("addInParameter(%s)", paramId);
                    else
                        out.println("addInParameter(%s);", paramId);
                }
            }
            else {
                if (scala)
                    out.println("addOutParameter(%s.%s)", className, paramId);
                else if (kotlin)
                    out.println("addOutParameter(%s)", paramId);
                else
                    out.println("addOutParameter(%s);", paramId);
            }













        }

        if (routine.getOverload() != null) {
            if (scala)
                out.println("setOverloaded(true)");
            else if (kotlin)
                out.println("overloaded = true");
            else
                out.println("setOverloaded(true);");
        }












        out.println("}");

        for (ParameterDefinition parameter : routine.getInParameters()) {
            final String setterReturnType = generateFluentSetters() ? className : tokenVoid;
            final String setter = getStrategy().getJavaSetterName(parameter, Mode.DEFAULT);
            final String numberValue = parameter.getType(resolver()).isGenericNumberType() ? "Number" : "Value";
            final String numberField = parameter.getType(resolver()).isGenericNumberType() ? "Number" : "Field";
            final String paramId = getStrategy().getJavaIdentifier(parameter);
            final String paramName = "value".equals(paramId) ? "value_" : "value";

            out.javadoc("Set the <code>%s</code> parameter IN value to the routine", parameter.getOutputName());

            if (scala) {
                out.println("def %s(%s: %s) : Unit = set%s(%s.%s, %s)",
                    setter, paramName, refNumberType(out, parameter.getType(resolver())), numberValue, className, paramId, paramName);
            }
            else if (kotlin) {
                out.println("fun %s(%s: %s?) = set%s(%s, %s)",
                    setter, paramName, refNumberType(out, parameter.getType(resolver())), numberValue, paramId, paramName);
            }
            else {
                out.println("public void %s(%s %s) {", setter, varargsIfArray(refNumberType(out, parameter.getType(resolver()))), paramName);
                out.println("set%s(%s, %s);", numberValue, paramId, paramName);
                out.println("}");
            }

            if (routine.isSQLUsable()) {
                out.javadoc("Set the <code>%s</code> parameter to the function to be used with a {@link org.jooq.Select} statement", parameter.getOutputName());

                if (scala) {
                    out.println("def %s(field: %s[%s]): %s = {", setter, Field.class, refExtendsNumberType(out, parameter.getType(resolver())), setterReturnType);
                    out.println("set%s(%s.%s, field)", numberField, className, paramId);

                    if (generateFluentSetters())
                        out.println("this");

                    out.println("}");
                }
                else if (kotlin) {
                    out.println("fun %s(field: %s<%s?>): %s {", setter, Field.class, refExtendsNumberType(out, parameter.getType(resolver())), setterReturnType);
                    out.println("set%s(%s, field)", numberField, paramId);

                    if (generateFluentSetters())
                        out.println("return this");

                    out.println("}");
                }
                else {
                    out.println("public %s %s(%s<%s> field) {", setterReturnType, setter, Field.class, refExtendsNumberType(out, parameter.getType(resolver())));
                    out.println("set%s(%s, field);", numberField, paramId);

                    if (generateFluentSetters())
                        out.println("return this;");

                    out.println("}");
                }
            }
        }

        for (ParameterDefinition parameter : routine.getAllParameters()) {
            boolean isReturnValue = parameter.equals(routine.getReturnValue());
            boolean isOutParameter = routine.getOutParameters().contains(parameter);

            if (isOutParameter && !isReturnValue) {
                final String paramName = parameter.getOutputName();
                final String paramTypeFull = getJavaType(parameter.getType(resolver()));
                final String paramType = out.ref(paramTypeFull);
                final String paramGetter = getStrategy().getJavaGetterName(parameter, Mode.DEFAULT);
                final String paramId = getStrategy().getJavaIdentifier(parameter);

                if (!printDeprecationIfUnknownType(out, paramTypeFull))
                    out.javadoc("Get the <code>%s</code> parameter OUT value from the routine", paramName);

                if (scala) {
                    out.println("def %s: %s = get(%s.%s)", paramGetter, paramType, className, paramId);
                }
                else if (kotlin) {
                    out.println("fun %s(): %s? = get(%s)", paramGetter, paramType, paramId);
                }
                else {
                    out.println("public %s %s() {", paramType, paramGetter);
                    out.println("return get(%s);", paramId);
                    out.println("}");
                }
            }
        }

        generateRoutineClassFooter(routine, out);
        out.println("}");
    }

    /**
     * Subclasses may override this method to provide routine class footer code.
     */
    @SuppressWarnings("unused")
    protected void generateRoutineClassFooter(RoutineDefinition routine, JavaWriter out) {}

    /**
     * Subclasses may override this method to provide their own Javadoc.
     */
    protected void generateRoutineClassJavadoc(RoutineDefinition routine, JavaWriter out) {
        if (generateCommentsOnRoutines())
            printClassJavadoc(out, routine);
        else
            printClassJavadoc(out, "The routine <code>" + routine.getQualifiedInputName() + "</code>.");
    }

    protected void printConvenienceMethodFunctionAsField(JavaWriter out, RoutineDefinition function, boolean parametersAsField) {
        // [#281] - Java can't handle more than 255 method parameters
        if (function.getInParameters().size() > 254) {
            log.warn("Too many parameters", "Function " + function + " has more than 254 in parameters. Skipping generation of convenience method.");
            return;
        }

        // Do not generate separate convenience methods, if there are no IN
        // parameters. They would have the same signature and no additional
        // meaning
        if (parametersAsField && function.getInParameters().isEmpty())
            return;

        final String functionTypeFull = getJavaType(function.getReturnType(resolver()));
        final String functionType = out.ref(functionTypeFull);
        final String className = out.ref(getStrategy().getFullJavaClassName(function));
        final String localVar = disambiguateJavaMemberName(function.getInParameters(), "f");
        final String methodName = getStrategy().getJavaMethodName(function, Mode.DEFAULT);

        if (!printDeprecationIfUnknownType(out, functionTypeFull) &&
            !printDeprecationIfUnknownTypes(out, function.getInParameters()))
            out.javadoc("Get <code>%s</code> as a field.", function.getQualifiedOutputName());

        if (scala)
            out.print("def %s(", methodName);
        else if (kotlin)
            out.print("fun %s(", methodName);
        else
            out.print("public static %s<%s> %s(",
                function.isAggregate() ? AggregateFunction.class : Field.class,
                functionType,
                methodName);

        if (!function.getInParameters().isEmpty())
            out.println();

        printParameterDeclarations(out, function.getInParameters(), parametersAsField, "  ");

        if (scala) {
            out.println("): %s[%s] = {",
                function.isAggregate() ? AggregateFunction.class : Field.class,
                functionType);
            out.println("val %s = new %s", localVar, className);
        }
        else if (kotlin) {
            out.println("): %s<%s?> {",
                function.isAggregate() ? AggregateFunction.class : Field.class,
                functionType);
            out.println("val %s = %s()", localVar, className);
        }
        else {
            out.println(") {");
            out.println("%s %s = new %s();", className, localVar, className);
        }

        for (ParameterDefinition parameter : function.getInParameters()) {
            final String paramSetter = getStrategy().getJavaSetterName(parameter, Mode.DEFAULT);
            final String paramMember = getStrategy().getJavaMemberName(parameter);

            if (scala || kotlin)
                out.println("%s.%s(%s)", localVar, paramSetter, paramMember);
            else
                out.println("%s.%s(%s);", localVar, paramSetter, paramMember);
        }

        out.println();

        if (scala)
            out.println("return %s.as%s", localVar, function.isAggregate() ? "AggregateFunction" : "Field");
        else if (kotlin)
            out.println("return %s.as%s()", localVar, function.isAggregate() ? "AggregateFunction" : "Field");
        else
            out.println("return %s.as%s();", localVar, function.isAggregate() ? "AggregateFunction" : "Field");

        out.println("}");
    }

    protected void printConvenienceMethodTableValuedFunctionAsField(JavaWriter out, TableDefinition function, boolean parametersAsField, String methodName) {
        // [#281] - Java can't handle more than 255 method parameters
        if (function.getParameters().size() > 254) {
            log.warn("Too many parameters", "Function " + function + " has more than 254 in parameters. Skipping generation of convenience method.");
            return;
        }

        if (function.getParameters().isEmpty())

            // Do not generate separate convenience methods, if there are no IN
            // parameters. They would have the same signature and no additional
            // meaning
            if (parametersAsField)
                return;

            // [#4883] Scala doesn't have separate namespaces for val and def
            else if (scala)
                return;

        final String className = out.ref(getStrategy().getFullJavaClassName(function));

        // [#5765] To prevent name clashes, this identifier is not imported
        final String functionIdentifier = getStrategy().getFullJavaIdentifier(function);

        if (!printDeprecationIfUnknownTypes(out, function.getParameters()))
            out.javadoc("Get <code>%s</code> as a table.", function.getQualifiedOutputName());

        if (scala)
            out.print("def %s(", methodName);
        else if (kotlin)
            out.print("fun %s(", methodName);
        else
            out.print("public static %s %s(", className, methodName);

        if (!function.getParameters().isEmpty())
            out.println();

        printParameterDeclarations(out, function.getParameters(), parametersAsField, "  ");

        if (scala || kotlin) {
            out.println("): %s = %s.call(", className, functionIdentifier);
        }
        else {
            out.println(") {");
            out.println("return %s.call(", functionIdentifier);
        }

        String separator = "  ";
        for (ParameterDefinition parameter : function.getParameters()) {
            out.println("%s%s", separator, getStrategy().getJavaMemberName(parameter));

            separator = ", ";
        }

        if (scala || kotlin)
            out.println(")");
        else
            out.println(");")
               .println("}");
    }

    private void printParameterDeclarations(JavaWriter out, List<ParameterDefinition> parameters, boolean parametersAsField, String separator) {
        for (ParameterDefinition parameter : parameters) {
            final String memberName = getStrategy().getJavaMemberName(parameter);

            if (scala) {
                if (parametersAsField)
                    out.println("%s%s: %s[%s]", separator, memberName, Field.class, refExtendsNumberType(out, parameter.getType(resolver())));
                else
                    out.println("%s%s: %s", separator, memberName, refNumberType(out, parameter.getType(resolver())));
            }
            else if (kotlin) {
                if (parametersAsField)
                    out.println("%s%s: %s<%s?>", separator, memberName, Field.class, refExtendsNumberType(out, parameter.getType(resolver())));
                else
                    out.println("%s%s: %s?", separator, memberName, refNumberType(out, parameter.getType(resolver())));
            }
            else {
                if (parametersAsField)
                    out.println("%s%s<%s> %s", separator, Field.class, refExtendsNumberType(out, parameter.getType(resolver())), memberName);
                else
                    out.println("%s%s %s", separator, refNumberType(out, parameter.getType(resolver())), memberName);
            }

            separator = ", ";
        }
    }

    private String disambiguateJavaMemberName(Collection<? extends Definition> definitions, String defaultName) {

        // [#2502] - Some name mangling in the event of procedure arguments
        // called "configuration"
        Set<String> names = new HashSet<>();

        for (Definition definition : definitions)
            names.add(getStrategy().getJavaMemberName(definition));

        String name = defaultName;

        while (names.contains(name))
            name += "_";

        return name;
    }

    protected void printConvenienceMethodFunction(JavaWriter out, RoutineDefinition function, boolean instance) {
        // [#281] - Java can't handle more than 255 method parameters
        if (function.getInParameters().size() > 254) {
            log.warn("Too many parameters", "Function " + function + " has more than 254 in parameters. Skipping generation of convenience method.");
            return;
        }

        final String className = out.ref(getStrategy().getFullJavaClassName(function));
        final String functionName = function.getQualifiedOutputName();
        final String functionTypeFull = getJavaType(function.getReturnType(resolver()));
        final String functionType = out.ref(functionTypeFull);
        final String methodName = getStrategy().getJavaMethodName(function, Mode.DEFAULT);

        // [#3456] Local variables should not collide with actual function arguments
        final String configurationArgument = disambiguateJavaMemberName(function.getInParameters(), "configuration");
        final String localVar = disambiguateJavaMemberName(function.getInParameters(), "f");

        if (!printDeprecationIfUnknownType(out, functionTypeFull) &&
            !printDeprecationIfUnknownTypes(out, function.getInParameters()))
            out.javadoc("Call <code>%s</code>", functionName);

        if (scala)
            out.println("def %s(", methodName);
        else if (kotlin)
            out.println("fun %s(", methodName);
        else
            out.println("public %s%s %s(", !instance ? "static " : "", functionType, methodName);

        String separator = "  ";
        if (!instance) {
            if (scala || kotlin)
                out.println("%s%s: %s", separator, configurationArgument, Configuration.class);
            else
                out.println("%s%s %s", separator, Configuration.class, configurationArgument);

            separator = ", ";
        }

        for (ParameterDefinition parameter : function.getInParameters()) {

            // Skip SELF parameter
            if (instance && parameter.equals(function.getInParameters().get(0)))
                continue;

            final String paramType = refNumberType(out, parameter.getType(resolver()));
            final String paramMember = getStrategy().getJavaMemberName(parameter);

            if (scala)
                out.println("%s%s: %s", separator, paramMember, paramType);
            else if (kotlin)
                out.println("%s%s: %s?", separator, paramMember, paramType);
            else
                out.println("%s%s %s", separator, paramType, paramMember);

            separator = ", ";
        }

        if (scala) {
            out.println("): %s = {", functionType);
            out.println("val %s = new %s()", localVar, className);
        }
        else if (kotlin) {
            out.println("): %s? {", functionType);
            out.println("val %s = %s()", localVar, className);
        }
        else {
            out.println(") {");
            out.println("%s %s = new %s();", className, localVar, className);
        }

        for (ParameterDefinition parameter : function.getInParameters()) {
            final String paramSetter = getStrategy().getJavaSetterName(parameter, Mode.DEFAULT);
            final String paramMember = (instance && parameter.equals(function.getInParameters().get(0)))
                ? "this"
                : getStrategy().getJavaMemberName(parameter);

            if (scala || kotlin)
                out.println("%s.%s(%s)", localVar, paramSetter, paramMember);
            else if (kotlin)
                out.println("%s.%s(%s)", localVar, paramSetter, paramMember);
            else
                out.println("%s.%s(%s);", localVar, paramSetter, paramMember);
        }

        out.println();

        if (scala || kotlin)
            out.println("%s.execute(%s)", localVar, instance ? "configuration()" : configurationArgument);
        else
            out.println("%s.execute(%s);", localVar, instance ? "configuration()" : configurationArgument);

        // TODO [#956] Find a way to register "SELF" as OUT parameter
        // in case this is a UDT instance (member) function

        if (scala)
            out.println("%s.getReturnValue", localVar);
        else if (kotlin)
            out.println("return %s.returnValue", localVar);
        else
            out.println("return %s.getReturnValue();", localVar);

        out.println("}");
    }

    protected void printConvenienceMethodProcedure(JavaWriter out, RoutineDefinition procedure, boolean instance) {
        // [#281] - Java can't handle more than 255 method parameters
        if (procedure.getInParameters().size() > 254) {
            log.warn("Too many parameters", "Procedure " + procedure + " has more than 254 in parameters. Skipping generation of convenience method.");
            return;
        }

        final String className = out.ref(getStrategy().getFullJavaClassName(procedure));
        final String configurationArgument = disambiguateJavaMemberName(procedure.getInParameters(), "configuration");
        final String localVar = disambiguateJavaMemberName(procedure.getInParameters(), "p");
        final List<ParameterDefinition> outParams = list(procedure.getReturnValue(), procedure.getOutParameters());
        final String methodName = getStrategy().getJavaMethodName(procedure, Mode.DEFAULT);
        final String firstOutParamType = outParams.size() == 1 ? out.ref(getJavaType(outParams.get(0).getType(resolver()))) : "";

        if (!printDeprecationIfUnknownTypes(out, procedure.getAllParameters()))
            out.javadoc("Call <code>%s</code>", procedure.getQualifiedOutputName());

        if (scala)
            out.println("def %s(", methodName);
        else if (kotlin)
            out.println("fun %s(", methodName);
        else {
            out.println("public %s%s %s(",
                !instance ? "static " : "",
                outParams.size() == 0 ? "void" : outParams.size() == 1 ? firstOutParamType : className,
                methodName
            );
        }

        String separator = "  ";
        if (!instance) {
            if (scala || kotlin)
                out.println("%s%s: %s", separator, configurationArgument, Configuration.class);
            else
                out.println("%s%s %s", separator, Configuration.class, configurationArgument);

            separator = ", ";
        }

        for (ParameterDefinition parameter : procedure.getInParameters()) {

            // Skip SELF parameter
            if (instance && parameter.equals(procedure.getInParameters().get(0)))
                continue;

            final String memberName = getStrategy().getJavaMemberName(parameter);
            final String typeName = refNumberType(out, parameter.getType(resolver()));

            if (scala)
                out.println("%s%s: %s", separator, memberName, typeName);
            else if (kotlin)
                out.println("%s%s: %s?", separator, memberName, typeName);
            else
                out.println("%s%s %s", separator, typeName, memberName);

            separator = ", ";
        }

        if (scala) {
            out.println("): %s = {", outParams.size() == 0 ? "Unit" : outParams.size() == 1 ? firstOutParamType : className);
            out.println("val %s = new %s", localVar, className);
        }
        else if (kotlin) {
            out.println("): %s%s {", outParams.size() == 0 ? "Unit" : outParams.size() == 1 ? firstOutParamType : className, outParams.size() == 1 ? "?" : "");
            out.println("val %s = %s()", localVar, className);
        }
        else {
            out.println(") {");
            out.println("%s %s = new %s();", className, localVar, className);
        }

        for (ParameterDefinition parameter : procedure.getInParameters()) {
            final String setter = getStrategy().getJavaSetterName(parameter, Mode.DEFAULT);
            final String arg = (instance && parameter.equals(procedure.getInParameters().get(0)))
                ? "this"
                : getStrategy().getJavaMemberName(parameter);

            if (scala || kotlin)
                out.println("%s.%s(%s)", localVar, setter, arg);
            else
                out.println("%s.%s(%s);", localVar, setter, arg);
        }

        out.println();

        if (scala || kotlin)
            out.println("%s.execute(%s)", localVar, instance ? "configuration()" : configurationArgument);
        else
            out.println("%s.execute(%s);", localVar, instance ? "configuration()" : configurationArgument);

        if (outParams.size() > 0) {
            final ParameterDefinition parameter = outParams.get(0);

            // Avoid disambiguation for RETURN_VALUE getter
            final String getter = parameter == procedure.getReturnValue()
                ? "getReturnValue"
                : getStrategy().getJavaGetterName(parameter, Mode.DEFAULT);
            final boolean isUDT = parameter.getType(resolver()).isUDT();

            if (instance) {

                // [#3117] Avoid funny call-site ambiguity if this is a UDT that is implemented by an interface
                if (generateInterfaces() && isUDT) {
                    final String columnTypeInterface = out.ref(getJavaType(parameter.getType(resolver(Mode.INTERFACE)), Mode.INTERFACE));

                    if (scala)
                        out.println("from(%s.%s.asInstanceOf[%s])", localVar, getter, columnTypeInterface);
                    else if (kotlin)
                        out.println("from(%s.%s() as %s);", localVar, getter, columnTypeInterface);
                    else
                        out.println("from((%s) %s.%s());", columnTypeInterface, localVar, getter);
                }
                else {
                    if (scala)
                        out.println("from(%s.%s)", localVar, getter);
                    else if (kotlin)
                        out.println("from(%s.%s())", localVar, getter);
                    else
                        out.println("from(%s.%s());", localVar, getter);
                }
            }

            if (outParams.size() == 1) {
                if (scala)
                    out.println("return %s.%s", localVar, getter);
                else if (kotlin)
                    out.println("return %s.%s()", localVar, getter);
                else
                    out.println("return %s.%s();", localVar, getter);
            }
            else if (outParams.size() > 1) {
                if (scala || kotlin)
                    out.println("return %s", localVar);
                else
                    out.println("return %s;", localVar);
            }
        }

        out.println("}");
    }

    protected void printConvenienceMethodTableValuedFunction(JavaWriter out, TableDefinition function, String methodName) {
        // [#281] - Java can't handle more than 255 method parameters
        if (function.getParameters().size() > 254) {
            log.warn("Too many parameters", "Function " + function + " has more than 254 in parameters. Skipping generation of convenience method.");
            return;
        }

        final String recordClassName = out.ref(getStrategy().getFullJavaClassName(function, Mode.RECORD));

        // [#3456] Local variables should not collide with actual function arguments
        final String configurationArgument = disambiguateJavaMemberName(function.getParameters(), "configuration");

        // [#5765] To prevent name clashes, this identifier is not imported
        final String functionName = getStrategy().getFullJavaIdentifier(function);

        if (!printDeprecationIfUnknownTypes(out, function.getParameters()))
            out.javadoc("Call <code>%s</code>.", function.getQualifiedOutputName());

        if (scala)
            out.println("def %s(", methodName);
        else if (kotlin)
            out.println("fun %s(", methodName);
        else
            out.println("public static %s<%s> %s(", Result.class, recordClassName, methodName);

        String separator = "  ";
        if (scala || kotlin)
            out.println("%s%s: %s", separator, configurationArgument, Configuration.class);
        else
            out.println("%s%s %s", separator, Configuration.class, configurationArgument);

        printParameterDeclarations(out, function.getParameters(), false, ", ");

        if (scala) {
            out.println("): %s[%s] = %s.dsl().selectFrom(%s.call(", Result.class, recordClassName, configurationArgument, functionName);
        }
        else if (kotlin) {
            out.println("): %s<%s> = %s.dsl().selectFrom(%s.call(", Result.class, recordClassName, configurationArgument, functionName);
        }
        else {
            out.println(") {");
            out.println("return %s.dsl().selectFrom(%s.call(", configurationArgument, functionName);
        }

        separator = "  ";
        for (ParameterDefinition parameter : function.getParameters()) {
            out.println("%s%s", separator, getStrategy().getJavaMemberName(parameter));

            separator = ", ";
        }

        if (scala || kotlin)
            out.println(")).fetch()");
        else
            out.println(")).fetch();")
               .println("}");
    }

    protected void printRecordTypeMethod(JavaWriter out, Definition definition) {
        final String className = out.ref(getStrategy().getFullJavaClassName(definition, Mode.RECORD));

        out.javadoc("The class holding records for this type");

        if (scala) {
            out.println("override def getRecordType: %s[%s] = classOf[%s]", Class.class, className, className);
        }
        else if (kotlin) {
            out.println("override fun getRecordType(): %s<%s> = %s::class.java", Class.class, className, className);
        }
        else {
            out.override();
            out.println("public %s<%s> getRecordType() {", Class.class, className);
            out.println("return %s.class;", className);
            out.println("}");
        }
    }

    protected void printSingletonInstance(JavaWriter out, Definition definition) {
        final String className = getStrategy().getJavaClassName(definition);
        final String identifier = getStrategy().getJavaIdentifier(definition);

        out.javadoc("The reference instance of <code>%s</code>", definition.getQualifiedOutputName());

        if (scala)
            out.println("val %s = new %s", identifier, className);
        else if (kotlin)
            out.println("val %s = %s()", identifier, className);
        else
            out.println("public static final %s %s = new %s();", className, identifier, className);
    }

    protected final String escapeEntities(String comment) {

        if (comment == null)
            return null;

        // [#5704] Do not allow certain HTML entities
        return comment
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    protected void printClassJavadoc(JavaWriter out, Definition definition) {
        printClassJavadoc(out, escapeEntities(definition.getComment()));
    }

    private String comment(Definition definition) {
        return definition instanceof CatalogDefinition && generateCommentsOnCatalogs()
            || definition instanceof SchemaDefinition && generateCommentsOnSchemas()
            || definition instanceof TableDefinition && generateCommentsOnTables()
            || definition instanceof ColumnDefinition && generateCommentsOnColumns()
            || definition instanceof UDTDefinition && generateCommentsOnUDTs()
            || definition instanceof AttributeDefinition && generateCommentsOnAttributes()
            || definition instanceof PackageDefinition && generateCommentsOnPackages()
            || definition instanceof RoutineDefinition && generateCommentsOnRoutines()
            || definition instanceof ParameterDefinition && generateCommentsOnParameters()
            || definition instanceof SequenceDefinition && generateCommentsOnSequences()
             ? StringUtils.defaultIfBlank(definition.getComment(), "")
             : "";
    }

    protected void printClassJavadoc(JavaWriter out, String comment) {
        if (generateJavadoc()) {
            out.println("/**");

            if (comment != null && comment.length() > 0)
                printJavadocParagraph(out, comment, "");
            else
                out.println(" * This class is generated by jOOQ.");

            out.println(" */");
        }
    }

    /**
     * @deprecated - [#10355] - 3.14.0 - This method is no longer used by the
     *             code generator. Use a
     *             {@link #printClassAnnotations(JavaWriter, Definition, Mode)}
     *             instead.
     */
    @SuppressWarnings("unused")
    @Deprecated
    protected final void printClassAnnotations(JavaWriter out, SchemaDefinition schema) {}

    /**
     * @deprecated - [#10355] - 3.14.0 - This method is no longer used by the
     *             code generator. Use a
     *             {@link #printClassAnnotations(JavaWriter, Definition, Mode)}
     *             instead.
     */
    @SuppressWarnings("unused")
    @Deprecated
    protected final void printClassAnnotations(JavaWriter out, SchemaDefinition schema, CatalogDefinition catalog) {}

    protected void printClassAnnotations(JavaWriter out, Definition definition, Mode mode) {
        if (generateGeneratedAnnotation()) {
            SchemaDefinition schema = definition.getSchema();
            CatalogDefinition catalog = definition.getCatalog();

            // [#7581] The concrete annotation type depends on the JDK, with
            //         javax.annotation.Generated being deprecated in JDK 9
            GeneratedAnnotationType type = generateGeneratedAnnotationType();
            if (type == null)
                type = GeneratedAnnotationType.DETECT_FROM_JDK;

            String generated;
            switch (type) {
                case DETECT_FROM_JDK:
                    try {

                        // Seems more reliable than tampering with java.version
                        Reflect.on("java.util.Optional").call("of", new Object()).call("stream");
                        generated = "javax.annotation.processing.Generated";
                    }
                    catch (ReflectException e) {
                        generated = "javax.annotation.Generated";
                    }

                    break;
                case JAVAX_ANNOTATION_GENERATED:
                    generated = "javax.annotation.Generated";
                    break;
                case JAVAX_ANNOTATION_PROCESSING_GENERATED:
                    generated = "javax.annotation.processing.Generated";
                    break;
                default:
                    throw new IllegalStateException("Unsupported type: " + type);
            }

            out.println("@%s(", out.ref(generated));

            if (useSchemaVersionProvider() || useCatalogVersionProvider()) {
                boolean hasCatalogVersion = !StringUtils.isBlank(catalogVersions.get(catalog));
                boolean hasSchemaVersion = !StringUtils.isBlank(schemaVersions.get(schema));

                if (scala)
                    out.println("value = %s(", out.ref("scala.Array"));
                else
                    out.println("value = {");

                out.println("\"https://www.jooq.org\",");
                out.println("\"jOOQ version:%s\"%s", Constants.VERSION, (hasCatalogVersion || hasSchemaVersion ? "," : ""));

                if (hasCatalogVersion)
                    out.println("\"catalog version:%s\"%s", escapeString(catalogVersions.get(catalog)), (hasSchemaVersion ? "," : ""));
                if (hasSchemaVersion)
                    out.println("\"schema version:%s\"", escapeString(schemaVersions.get(schema)));

                if (scala)
                    out.println("),");
                else
                    out.println("},");

                if (generateGeneratedAnnotationDate())
                    out.println("date = \"" + isoDate + "\",");
                out.println("comments = \"This class is generated by jOOQ\"");
            }
            else {
                if (scala)
                    out.println("value = %s(", out.ref("scala.Array"));
                else
                    out.println("value = {");

                out.println("\"https://www.jooq.org\",");
                out.println("\"jOOQ version:%s\"", Constants.VERSION);

                if (scala)
                    out.println("),");
                else
                    out.println("},");

                out.println("comments = \"This class is generated by jOOQ\"");
            }

            out.println(")");
        }

        if (scala) {}
        else if (kotlin)
            out.println("@Suppress(\"UNCHECKED_CAST\")");
        else
            out.println("@%s({ \"all\", \"unchecked\", \"rawtypes\" })", out.ref("java.lang.SuppressWarnings"));
    }

    private String readVersion(File file, String type) {
        String result = null;

        try {
            RandomAccessFile f = new RandomAccessFile(file, "r");

            try {
                byte[] bytes = new byte[(int) f.length()];
                f.readFully(bytes);
                String string = new String(bytes);

                Matcher matcher = Pattern.compile("@(?:javax\\.annotation\\.)?Generated\\(\\s*?value\\s*?=\\s*?" + (scala ? "Array\\([^)]*?" : "\\{[^}]*?") + "\"" + type + " version:([^\"]*?)\"").matcher(string);
                if (matcher.find()) {
                    result = matcher.group(1);
                }
            }
            finally {
                f.close();
            }
        }
        catch (IOException ignore) {}

        return result;
    }

    /**
     * This method is used to add line breaks in lengthy javadocs
     */
    protected void printJavadocParagraph(JavaWriter out, String comment, String indent) {

        // [#3450] [#4880] [#7693] Must not print */ inside Javadoc
        String escaped = JavaWriter.escapeJavadoc(comment);
        printParagraph(out, escaped, indent + " * ");
    }

    protected void printParagraph(GeneratorWriter<?> out, String comment, String indent) {
        boolean newLine = true;
        int lineLength = 0;

        for (int i = 0; i < comment.length(); i++) {
            if (newLine) {
                out.print(indent);

                newLine = false;
            }

            out.print(comment.charAt(i));
            lineLength++;

            if (comment.charAt(i) == '\n') {
                lineLength = 0;
                newLine = true;
            }
            else if (lineLength > 70 && Character.isWhitespace(comment.charAt(i))) {
                out.println();
                lineLength = 0;
                newLine = true;
            }
        }

        if (!newLine) {
            out.println();
        }
    }

    protected void printPackage(JavaWriter out, Definition definition) {
        printPackage(out, definition, Mode.DEFAULT);
    }

    protected void printPackage(JavaWriter out, Definition definition, Mode mode) {
        printPackageComment(out, definition, mode);

        out.printPackageSpecification(getStrategy().getJavaPackageName(definition, mode));
        out.println();
        out.printImports();
        out.println();
    }

    protected void printPackageComment(JavaWriter out, Definition definition, Mode mode) {
        String header = getStrategy().getFileHeader(definition, mode);

        if (!StringUtils.isBlank(header)) {
            out.println("/*");
            printJavadocParagraph(out, header, "");
            out.println(" */");
        }
    }

    @Deprecated
    protected String getExtendsNumberType(DataTypeDefinition type) {
        return getNumberType(type, scala ? "_ <: " : "? extends ");
    }

    protected String refExtendsNumberType(JavaWriter out, DataTypeDefinition type) {
        if (type.isGenericNumberType()) {
            return (scala ? "_ <: " : "? extends ") + out.ref(Number.class);
        }
        else {
            return out.ref(getJavaType(type));
        }
    }

    @Deprecated
    protected String getNumberType(DataTypeDefinition type) {
        if (type.isGenericNumberType()) {
            return Number.class.getName();
        }
        else {
            return getJavaType(type);
        }
    }

    protected String refNumberType(JavaWriter out, DataTypeDefinition type) {
        if (type.isGenericNumberType()) {
            return out.ref(Number.class);
        }
        else {
            return out.ref(getJavaType(type));
        }
    }

    @Deprecated
    protected String getNumberType(DataTypeDefinition type, String prefix) {
        if (type.isGenericNumberType()) {
            return prefix + Number.class.getName();
        }
        else {
            return getJavaType(type);
        }
    }

    @Deprecated
    protected String getSimpleJavaType(DataTypeDefinition type) {
        return GenerationUtil.getSimpleJavaType(getJavaType(type));
    }

    protected String getJavaTypeReference(Database db, DataTypeDefinition type) {

        // [#4388] TODO: Improve array handling
        if (database.isArrayType(type.getType())) {
            Name baseType = GenerationUtil.getArrayBaseType(db.getDialect(), type.getType(), type.getQualifiedUserType());
            return getTypeReference(
                db,
                type.getSchema(),
                baseType.last(),
                type.getPrecision(),
                type.getScale(),
                type.getLength(),
                true,
                false,
                null,
                baseType
            ) + ".getArrayDataType()";
        }
        else {
            return getTypeReference(
                db,
                type.getSchema(),
                type.getType(),
                type.getPrecision(),
                type.getScale(),
                type.getLength(),
                type.isNullable(),
                type.isIdentity(),
                type.getDefaultValue(),
                type.getQualifiedUserType()
            );
        }
    }

    protected JavaTypeResolver resolver() {
        return new JavaTypeResolver() {
            @Override
            public String resolve(DataTypeDefinition type) {
                return getJavaType(type);
            }
        };
    }

    protected JavaTypeResolver resolver(final Mode udtMode) {
        return new JavaTypeResolver() {
            @Override
            public String resolve(DataTypeDefinition type) {
                return getJavaType(type, udtMode);
            }
        };
    }

    protected String getJavaType(DataTypeDefinition type) {
        return getJavaType(type, Mode.RECORD);
    }

    protected String getJavaType(DataTypeDefinition type, Mode udtMode) {
        return getType(
            type.getDatabase(),
            type.getSchema(),
            type.getType(),
            type.getPrecision(),
            type.getScale(),
            type.getQualifiedUserType(),
            type.getJavaType(),
            Object.class.getName(),
            udtMode);
    }

    /**
     * @deprecated - 3.9.0 - [#330]  - Use {@link #getType(Database, SchemaDefinition, String, int, int, Name, String, String)} instead.
     */
    @Deprecated
    protected String getType(Database db, SchemaDefinition schema, String t, int p, int s, String u, String javaType, String defaultType) {
        return getType(db, schema, t, p, s, name(u), javaType, defaultType);
    }

    protected String getType(Database db, SchemaDefinition schema, String t, int p, int s, Name u, String javaType, String defaultType) {
        return getType(db, schema, t, p, s, u, javaType, defaultType, Mode.RECORD);
    }

    /**
     * @deprecated - 3.9.0 - [#330]  - Use {@link #getType(Database, SchemaDefinition, String, int, int, Name, String, String, Mode)} instead.
     */
    @Deprecated
    protected String getType(Database db, SchemaDefinition schema, String t, int p, int s, String u, String javaType, String defaultType, Mode udtMode) {
        return getType(db, schema, t, p, s, name(u), javaType, defaultType, udtMode);
    }

    protected String getType(Database db, SchemaDefinition schema, String t, int p, int s, Name u, String javaType, String defaultType, Mode udtMode) {
        String type = defaultType;

        // Custom types
        if (javaType != null) {
            type = javaType;
        }

        // Array types
        else if (db.isArrayType(t)) {

            // [#4388] TODO: Improve array handling
            Name baseType = GenerationUtil.getArrayBaseType(db.getDialect(), t, u);

            if (scala)
                type = "scala.Array[" + getType(db, schema, baseType.last(), p, s, baseType, javaType, defaultType, udtMode) + "]";
            else if (kotlin)
                type = "kotlin.Array<" + getType(db, schema, baseType.last(), p, s, baseType, javaType, defaultType, udtMode) + "?>";
            else
                type = getType(db, schema, baseType.last(), p, s, baseType, javaType, defaultType, udtMode) + "[]";
        }

        // Check for Oracle-style VARRAY types
        else if (db.getArray(schema, u) != null) {
            boolean udtArray = db.getArray(schema, u).getElementType(resolver()).isUDT();

            if (udtMode == Mode.POJO || (udtMode == Mode.INTERFACE && !udtArray)) {
                if (scala)
                    type = "java.util.List[" + getJavaType(db.getArray(schema, u).getElementType(resolver(udtMode)), udtMode) + "]";
                else
                    type = "java.util.List<" + getJavaType(db.getArray(schema, u).getElementType(resolver(udtMode)), udtMode) + ">";
            }
            else if (udtMode == Mode.INTERFACE) {
                if (scala)
                    type = "java.util.List[_ <:" + getJavaType(db.getArray(schema, u).getElementType(resolver(udtMode)), udtMode) + "]";
                else
                    type = "java.util.List<? extends " + getJavaType(db.getArray(schema, u).getElementType(resolver(udtMode)), udtMode) + ">";
            }
            else {
                type = getStrategy().getFullJavaClassName(db.getArray(schema, u), Mode.RECORD);
            }
        }

        // Check for ENUM types
        else if (db.getEnum(schema, u) != null) {
            type = getStrategy().getFullJavaClassName(db.getEnum(schema, u), Mode.ENUM);
        }

        // Check for UDTs
        else if (db.getUDT(schema, u) != null) {
            type = getStrategy().getFullJavaClassName(db.getUDT(schema, u), udtMode);
        }

        // [#3942] PostgreSQL treats UDTs and table types in similar ways
        // [#5334] In MySQL, the user type is (ab)used for synthetic enum types. This can lead to accidental matches here
        else if (db.getDialect().family() == POSTGRES && db.getTable(schema, u) != null) {
            type = getStrategy().getFullJavaClassName(db.getTable(schema, u), udtMode);
        }

        // Check for custom types
        else if (u != null && db.getConfiguredCustomType(u.last()) != null) {
            type = u.last();
        }

        // Try finding a basic standard SQL type according to the current dialect
        else {
            try {
                Class<?> clazz = mapJavaTimeTypes(getDataType(db, t, p, s)).getType();
                if (scala && clazz == byte[].class)
                    type = "scala.Array[scala.Byte]";
                else if (kotlin && clazz == byte[].class)
                    type = "kotlin.ByteArray";
                else
                    type = clazz.getCanonicalName();

                if (clazz.getTypeParameters().length > 0) {
                    type += (scala ? "[" : "<");

                    String separator = "";
                    for (TypeVariable<?> var : clazz.getTypeParameters()) {
                        type += separator;
                        type += ((Class<?>) var.getBounds()[0]).getCanonicalName();

                        separator = ", ";
                    }

                    type += (scala ? "]" : ">");
                }
            }
            catch (SQLDialectNotSupportedException e) {
                if (defaultType == null) {
                    throw e;
                }
            }
        }

        if (kotlin && Object.class.getName().equals(type))
            type = "Any";

        return type;
    }

    protected String getTypeReference(Database db, SchemaDefinition schema, String t, int p, int s, int l, boolean n, boolean i, String d, Name u) {
        StringBuilder sb = new StringBuilder();
        if (db.getArray(schema, u) != null) {
            ArrayDefinition array = database.getArray(schema, u);

            sb.append(getJavaTypeReference(db, array.getElementType(resolver())));
            sb.append(".asArrayDataType(");
            sb.append(classOf(getStrategy().getFullJavaClassName(array, Mode.RECORD)));
            sb.append(")");
        }
        else if (db.getUDT(schema, u) != null) {
            sb.append(getStrategy().getFullJavaIdentifier(db.getUDT(schema, u)));
            sb.append(".getDataType()");
        }
        // [#3942] PostgreSQL treats UDTs and table types in similar ways
        // [#5334] In MySQL, the user type is (ab)used for synthetic enum types. This can lead to accidental matches here
        else if (db.getDialect().family() == POSTGRES && db.getTable(schema, u) != null) {
            sb.append(getStrategy().getFullJavaIdentifier(db.getTable(schema, u)));
            sb.append(".getDataType()");
        }
        else if (db.getEnum(schema, u) != null) {
            sb.append(getJavaTypeReference(db, new DefaultDataTypeDefinition(
                db, schema, DefaultDataType.getDataType(db.getDialect(), String.class).getTypeName(), l, p, s, n, d, (Name) null
            )));
            sb.append(".asEnumDataType(");
            sb.append(classOf(getStrategy().getFullJavaClassName(db.getEnum(schema, u), Mode.ENUM)));
            sb.append(")");
        }
        else {
            DataType<?> dataType;
            String sqlDataTypeRef;

            try {
                dataType = mapJavaTimeTypes(getDataType(db, t, p, s));
            }

            // Mostly because of unsupported data types.
            catch (SQLDialectNotSupportedException ignore) {
                dataType = SQLDataType.OTHER.nullable(n).identity(i);

                sb = new StringBuilder();

                sb.append(DefaultDataType.class.getName());
                sb.append(".getDefaultDataType(\"");
                sb.append(escapeString(u != null ? u.toString() : t));
                sb.append("\")");
            }

            dataType = dataType
                .nullable(n)
                .identity(i);

            if (d != null)
                dataType = dataType.defaultValue((Field) DSL.field(d, dataType));

            // If there is a standard SQLDataType available for the dialect-
            // specific DataType t, then reference that one.
            if (dataType.getSQLDataType() != null && sb.length() == 0) {
                DataType<?> sqlDataType = dataType.getSQLDataType();
                String literal = SQLDATATYPE_LITERAL_LOOKUP.get(sqlDataType);
                sqlDataTypeRef =
                    SQLDataType.class.getCanonicalName()
                  + '.'
                  + literal;

                sb.append(sqlDataTypeRef);

                if (dataType.hasPrecision() && (dataType.isTimestamp() || p > 0)) {

                    // [#6411] Call static method if available, rather than instance method
                    if (SQLDATATYPE_WITH_PRECISION.contains(literal))
                        sb.append('(').append(p);
                    else
                        sb.append(".precision(").append(p);

                    if (dataType.hasScale() && s > 0)
                        sb.append(", ").append(s);

                    sb.append(')');
                }

                if (dataType.hasLength() && l > 0)

                    // [#6411] Call static method if available, rather than instance method
                    if (SQLDATATYPE_WITH_LENGTH.contains(literal))
                        sb.append("(").append(l).append(")");
                    else
                        sb.append(".length(").append(l).append(")");
            }
            else {
                sqlDataTypeRef = SQLDataType.class.getCanonicalName() + ".OTHER";

                if (sb.length() == 0)
                    sb.append(sqlDataTypeRef);
            }

            if (!dataType.nullable())
                sb.append(".nullable(false)");

            if (dataType.identity())
                sb.append(".identity(true)");

            // [#5291] Some dialects report valid SQL expresions (e.g. PostgreSQL), others
            //         report actual values (e.g. MySQL).
            if (dataType.defaulted()) {
                sb.append(".defaultValue(");

                if (asList(MYSQL).contains(db.getDialect().family()))

                    // [#5574] While MySQL usually reports actual values, it does report
                    //         a CURRENT_TIMESTAMP expression, inconsistently
                    if (d != null && d.toLowerCase().startsWith("current_timestamp"))
                        sb.append("org.jooq.impl.DSL.field(\"")
                          .append(escapeString(d))
                          .append("\"");
                    else
                        sb.append("org.jooq.impl.DSL.inline(\"")
                          .append(escapeString(d))
                          .append("\"");
                else
                    sb.append("org.jooq.impl.DSL.field(\"")
                      .append(escapeString(d))
                      .append("\"");

                sb.append(", ")
                  .append(sqlDataTypeRef)
                  .append("))");
            }
        }

        return sb.toString();
    }

    private DataType<?> mapJavaTimeTypes(DataType<?> dataType) {
        DataType<?> result = dataType;


        // [#4429] [#5713] This logic should be implemented in Configuration
        if (dataType.isDateTime() && generateJavaTimeTypes) {
            if (dataType.getType() == Date.class)
                result = SQLDataType.LOCALDATE;
            else if (dataType.getType() == Time.class)
                result = SQLDataType.LOCALTIME;
            else if (dataType.getType() == Timestamp.class)
                result = SQLDataType.LOCALDATETIME;
        }


        return result;
    }

    @Deprecated
    protected boolean match(DataTypeDefinition type1, DataTypeDefinition type2) {
        return getJavaType(type1).equals(getJavaType(type2));
    }

    @SafeVarargs
    private static final <T> List<T> list(T... objects) {
        List<T> result = new ArrayList<>();

        if (objects != null)
            for (T object : objects)
                if (object != null && !"".equals(object))
                    result.add(object);

        return result;
    }

    private static final <T> List<T> list(T first, List<T> remaining) {
        List<T> result = new ArrayList<>();

        result.addAll(list(first));
        result.addAll(remaining);

        return result;
    }

    private static final <T> List<T> first(Collection<T> objects) {
        List<T> result = new ArrayList<>();

        if (objects != null) {
            for (T object : objects) {
                result.add(object);
                break;
            }
        }

        return result;
    }

    private static final <T> List<T> remaining(Collection<T> objects) {
        List<T> result = new ArrayList<>();

        if (objects != null) {
            result.addAll(objects);

            if (result.size() > 0)
                result.remove(0);
        }

        return result;
    }

    private String classOf(String string) {
        if (scala)
            return "classOf[" + string + "]";
        else if (kotlin)
            return string + "::class.java";
        else
            return string + ".class";
    }

    private static final Pattern SQUARE_BRACKETS = Pattern.compile("\\[\\]$");

    private String varargsIfArray(String type) {
        if (!generateVarargsSetters())
            return type;
        else if (scala)
            return type;
        else
            return SQUARE_BRACKETS.matcher(type).replaceFirst("...");
    }

    // [#3880] Users may need to call this method
    protected JavaWriter newJavaWriter(File file) {
        file = fixSuffix(file);
        JavaWriter result = new JavaWriter(file, generateFullyQualifiedTypes(), targetEncoding, generateJavadoc(), fileCache);

        if (generateIndentation != null)
            result.tabString(generateIndentation);
        if (generateNewline != null)
            result.newlineString(generateNewline);

        return result;
    }

    protected File getFile(Definition definition) {
        return fixSuffix(getStrategy().getFile(definition));
    }

    protected File getFile(Definition definition, Mode mode) {
        return fixSuffix(getStrategy().getFile(definition, mode));
    }

    private File fixSuffix(File file) {
        if (scala)
            file = new File(file.getParentFile(), file.getName().replace(".java", ".scala"));
        else if (kotlin)
            file = new File(file.getParentFile(), file.getName().replace(".java", ".kt"));

        return file;
    }

    // [#4626] Users may need to call this method
    protected void closeJavaWriter(JavaWriter out) {
        if (out.close())
            files.add(out.file());
    }
}
