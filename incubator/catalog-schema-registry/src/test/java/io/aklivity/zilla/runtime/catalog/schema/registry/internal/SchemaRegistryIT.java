/*
 * Copyright 2021-2023 Aklivity Inc
 *
 * Licensed under the Aklivity Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 *   https://www.aklivity.io/aklivity-community-license/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.aklivity.zilla.runtime.catalog.schema.registry.internal;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.rules.RuleChain.outerRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

import io.aklivity.zilla.runtime.catalog.schema.registry.internal.config.SchemaRegistryOptionsConfig;

public class SchemaRegistryIT
{
    private final K3poRule k3po = new K3poRule()
        .addScriptRoot("local", "io/aklivity/zilla/runtime/catalog/schema/registry/internal");

    private final TestRule timeout = new DisableOnDebug(new Timeout(10, SECONDS));

    @Rule
    public final TestRule chain = outerRule(k3po).around(timeout);

    private SchemaRegistryOptionsConfig config;

    @Before
    public void setup()
    {
        config = new SchemaRegistryOptionsConfig("http://localhost:8081", "default");
    }

    @Test
    @Specification({
        "${local}/resolve.schema.via.schema.id" })
    public void shouldResolveSchemaViaSchemaId() throws Exception
    {
        String expected = "{\"fields\":[{\"name\":\"id\",\"type\":\"string\"}," +
            "{\"name\":\"status\",\"type\":\"string\"}]," +
            "\"name\":\"Event\",\"namespace\":\"io.aklivity.example\",\"type\":\"record\"}";

        SchemaRegistryCatalogHandler catalog = new SchemaRegistryCatalogHandler(config);

        String schema = catalog.resolve(9);

        k3po.finish();

        assertThat(schema, not(nullValue()));
        assertEquals(expected, schema);
    }

    @Test
    @Specification({
        "${local}/resolve.schema.via.subject.version" })
    public void shouldResolveSchemaViaSubjectVersion() throws Exception
    {
        String expected = "{\"fields\":[{\"name\":\"id\",\"type\":\"string\"}," +
                "{\"name\":\"status\",\"type\":\"string\"}]," +
                "\"name\":\"Event\",\"namespace\":\"io.aklivity.example\",\"type\":\"record\"}";

        SchemaRegistryCatalogHandler catalog = new SchemaRegistryCatalogHandler(config);

        int schemaId = catalog.resolve("items-snapshots-value", "latest");

        String schema = catalog.resolve(schemaId);

        k3po.finish();

        assertEquals(schemaId, 9);
        assertThat(schema, not(nullValue()));
        assertEquals(expected, schema);
    }

    @Test
    @Specification({
        "${local}/register.schema" })
    public void shouldRegisterSchema() throws Exception
    {
        String schema = "{\"type\": \"record\",\"name\": \"test\",\"fields\":[{\"type\": \"string\",\"name\": \"field1\"}," +
                "{\"type\": \"com.acme.Referenced\",\"name\": \"int\"}]}";

        SchemaRegistryCatalogHandler catalog = new SchemaRegistryCatalogHandler(config);

        int schemaId = catalog.register("items-snapshots-value", "avro", schema);

        k3po.finish();

        assertThat(schemaId, not(nullValue()));
        assertEquals(schemaId, 1);
    }

    @Test
    @Specification({
        "${local}/resolve.schema.via.schema.id" })
    public void shouldResolveSchemaViaSchemaIdFromCache() throws Exception
    {
        String expected = "{\"fields\":[{\"name\":\"id\",\"type\":\"string\"}," +
                "{\"name\":\"status\",\"type\":\"string\"}]," +
                "\"name\":\"Event\",\"namespace\":\"io.aklivity.example\",\"type\":\"record\"}";

        SchemaRegistryCatalogHandler catalog = new SchemaRegistryCatalogHandler(config);

        catalog.resolve(9);

        k3po.finish();

        String schema = catalog.resolve(9);

        assertThat(schema, not(nullValue()));
        assertEquals(expected, schema);
    }

    @Test
    @Specification({
        "${local}/resolve.schema.via.subject.version" })
    public void shouldResolveSchemaViaSubjectVersionFromCache() throws Exception
    {
        String expected = "{\"fields\":[{\"name\":\"id\",\"type\":\"string\"}," +
                "{\"name\":\"status\",\"type\":\"string\"}]," +
                "\"name\":\"Event\",\"namespace\":\"io.aklivity.example\",\"type\":\"record\"}";

        SchemaRegistryCatalogHandler catalog = new SchemaRegistryCatalogHandler(config);

        catalog.resolve(catalog.resolve("items-snapshots-value", "latest"));

        k3po.finish();

        int schemaId = catalog.resolve("items-snapshots-value", "latest");

        String schema = catalog.resolve(schemaId);

        assertEquals(schemaId, 9);
        assertThat(schema, not(nullValue()));
        assertEquals(expected, schema);
    }
}
