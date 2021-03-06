/*
 * This file is generated by jOOQ.
 */
package org.jooq.example.chart.db.tables;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.jooq.Check;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row6;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.example.chart.db.Indexes;
import org.jooq.example.chart.db.Keys;
import org.jooq.example.chart.db.Public;
import org.jooq.example.chart.db.tables.records.PaymentP2007_01Record;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PaymentP2007_01 extends TableImpl<PaymentP2007_01Record> {

    private static final long serialVersionUID = -299896336;

    /**
     * The reference instance of <code>public.payment_p2007_01</code>
     */
    public static final PaymentP2007_01 PAYMENT_P2007_01 = new PaymentP2007_01();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<PaymentP2007_01Record> getRecordType() {
        return PaymentP2007_01Record.class;
    }

    /**
     * The column <code>public.payment_p2007_01.payment_id</code>.
     */
    public final TableField<PaymentP2007_01Record, Integer> PAYMENT_ID = createField(DSL.name("payment_id"), org.jooq.impl.SQLDataType.INTEGER.nullable(false).defaultValue(org.jooq.impl.DSL.field("nextval('payment_payment_id_seq'::regclass)", org.jooq.impl.SQLDataType.INTEGER)), this, "");

    /**
     * The column <code>public.payment_p2007_01.customer_id</code>.
     */
    public final TableField<PaymentP2007_01Record, Integer> CUSTOMER_ID = createField(DSL.name("customer_id"), org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>public.payment_p2007_01.staff_id</code>.
     */
    public final TableField<PaymentP2007_01Record, Integer> STAFF_ID = createField(DSL.name("staff_id"), org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>public.payment_p2007_01.rental_id</code>.
     */
    public final TableField<PaymentP2007_01Record, Integer> RENTAL_ID = createField(DSL.name("rental_id"), org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>public.payment_p2007_01.amount</code>.
     */
    public final TableField<PaymentP2007_01Record, BigDecimal> AMOUNT = createField(DSL.name("amount"), org.jooq.impl.SQLDataType.NUMERIC(5, 2).nullable(false), this, "");

    /**
     * The column <code>public.payment_p2007_01.payment_date</code>.
     */
    public final TableField<PaymentP2007_01Record, LocalDateTime> PAYMENT_DATE = createField(DSL.name("payment_date"), org.jooq.impl.SQLDataType.LOCALDATETIME.nullable(false), this, "");

    /**
     * Create a <code>public.payment_p2007_01</code> table reference
     */
    public PaymentP2007_01() {
        this(DSL.name("payment_p2007_01"), null);
    }

    /**
     * Create an aliased <code>public.payment_p2007_01</code> table reference
     */
    public PaymentP2007_01(String alias) {
        this(DSL.name(alias), PAYMENT_P2007_01);
    }

    /**
     * Create an aliased <code>public.payment_p2007_01</code> table reference
     */
    public PaymentP2007_01(Name alias) {
        this(alias, PAYMENT_P2007_01);
    }

    private PaymentP2007_01(Name alias, Table<PaymentP2007_01Record> aliased) {
        this(alias, aliased, null);
    }

    private PaymentP2007_01(Name alias, Table<PaymentP2007_01Record> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    public <O extends Record> PaymentP2007_01(Table<O> child, ForeignKey<O, PaymentP2007_01Record> key) {
        super(child, key, PAYMENT_P2007_01);
    }

    @Override
    public Schema getSchema() {
        return Public.PUBLIC;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.IDX_FK_PAYMENT_P2007_01_CUSTOMER_ID, Indexes.IDX_FK_PAYMENT_P2007_01_STAFF_ID);
    }

    @Override
    public Identity<PaymentP2007_01Record, Integer> getIdentity() {
        return Keys.IDENTITY_PAYMENT_P2007_01;
    }

    @Override
    public List<ForeignKey<PaymentP2007_01Record, ?>> getReferences() {
        return Arrays.<ForeignKey<PaymentP2007_01Record, ?>>asList(Keys.PAYMENT_P2007_01__PAYMENT_P2007_01_CUSTOMER_ID_FKEY, Keys.PAYMENT_P2007_01__PAYMENT_P2007_01_STAFF_ID_FKEY, Keys.PAYMENT_P2007_01__PAYMENT_P2007_01_RENTAL_ID_FKEY);
    }

    public Customer customer() {
        return new Customer(this, Keys.PAYMENT_P2007_01__PAYMENT_P2007_01_CUSTOMER_ID_FKEY);
    }

    public Staff staff() {
        return new Staff(this, Keys.PAYMENT_P2007_01__PAYMENT_P2007_01_STAFF_ID_FKEY);
    }

    public Rental rental() {
        return new Rental(this, Keys.PAYMENT_P2007_01__PAYMENT_P2007_01_RENTAL_ID_FKEY);
    }

    @Override
    public List<Check<PaymentP2007_01Record>> getChecks() {
        return Arrays.<Check<PaymentP2007_01Record>>asList(
              Internal.createCheck(this, DSL.name("payment_p2007_01_payment_date_check"), "(((payment_date >= '2007-01-01 00:00:00'::timestamp without time zone) AND (payment_date < '2007-02-01 00:00:00'::timestamp without time zone)))", true)
        );
    }

    @Override
    public PaymentP2007_01 as(String alias) {
        return new PaymentP2007_01(DSL.name(alias), this);
    }

    @Override
    public PaymentP2007_01 as(Name alias) {
        return new PaymentP2007_01(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public PaymentP2007_01 rename(String name) {
        return new PaymentP2007_01(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public PaymentP2007_01 rename(Name name) {
        return new PaymentP2007_01(name, null);
    }

    // -------------------------------------------------------------------------
    // Row6 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row6<Integer, Integer, Integer, Integer, BigDecimal, LocalDateTime> fieldsRow() {
        return (Row6) super.fieldsRow();
    }
}
