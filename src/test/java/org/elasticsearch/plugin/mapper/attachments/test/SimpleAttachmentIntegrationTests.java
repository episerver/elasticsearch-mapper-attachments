/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.plugin.mapper.attachments.test;

import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import static org.elasticsearch.client.Requests.*;
import static org.elasticsearch.common.io.Streams.copyToBytesFromClasspath;
import static org.elasticsearch.common.io.Streams.copyToStringFromClasspath;
import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.fieldQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.hamcrest.Matchers.equalTo;

/**
 *
 */
public class SimpleAttachmentIntegrationTests extends ElasticsearchIntegrationTest {

    @Before
    public void createEmptyIndex() throws Exception {
        logger.info("creating index [test]");
        createIndex("test");
        ensureGreen();
    }

    @Override
    public Settings indexSettings() {
        return settingsBuilder()
                .put("index.numberOfReplicas", 0)
            .build();
    }

    @Test
    public void testSimpleAttachment() throws Exception {
        String mapping = copyToStringFromClasspath("/org/elasticsearch/index/mapper/xcontent/test-mapping.json");
        byte[] html = copyToBytesFromClasspath("/org/elasticsearch/index/mapper/xcontent/testXHTML.html");

        client().admin().indices().putMapping(putMappingRequest("test").type("person").source(mapping)).actionGet();

        index("test", "person", jsonBuilder().startObject().field("file", html).endObject());
        refresh();

        CountResponse countResponse = client().count(countRequest("test").query(fieldQuery("file.title", "test document"))).actionGet();
        assertThat(countResponse.getCount(), equalTo(1l));

        countResponse = client().count(countRequest("test").query(fieldQuery("file", "tests the ability"))).actionGet();
        assertThat(countResponse.getCount(), equalTo(1l));
    }

    @Test
    public void testSimpleAttachmentContentLengthLimit() throws Exception {
        String mapping = copyToStringFromClasspath("/org/elasticsearch/index/mapper/xcontent/test-mapping.json");
        byte[] txt = copyToBytesFromClasspath("/org/elasticsearch/index/mapper/xcontent/testContentLength.txt");
        final int CONTENT_LENGTH_LIMIT = 20;

        client().admin().indices().putMapping(putMappingRequest("test").type("person").source(mapping)).actionGet();

        index("test", "person", jsonBuilder().startObject().field("file").startObject().field("content", txt).field("_indexed_chars", CONTENT_LENGTH_LIMIT).endObject());
        refresh();

        CountResponse countResponse = client().count(countRequest("test").query(fieldQuery("file", "BeforeLimit"))).actionGet();
        assertThat(countResponse.getCount(), equalTo(1l));

        countResponse = client().count(countRequest("test").query(fieldQuery("file", "AfterLimit"))).actionGet();
        assertThat(countResponse.getCount(), equalTo(0l));
    }

    @Test
    public void testSimpleAttachmentNoContentLengthLimit() throws Exception {
        String mapping = copyToStringFromClasspath("/org/elasticsearch/index/mapper/xcontent/test-mapping.json");
        byte[] txt = copyToBytesFromClasspath("/org/elasticsearch/index/mapper/xcontent/testContentLength.txt");
        final int CONTENT_LENGTH_LIMIT = -1;

        client().admin().indices().putMapping(putMappingRequest("test").type("person").source(mapping)).actionGet();

        index("test", "person", jsonBuilder().startObject().field("file").startObject().field("content", txt).field("_indexed_chars", CONTENT_LENGTH_LIMIT).endObject());
        refresh();

        CountResponse countResponse = client().count(countRequest("test").query(fieldQuery("file", "Begin"))).actionGet();
        assertThat(countResponse.getCount(), equalTo(1l));

        countResponse = client().count(countRequest("test").query(fieldQuery("file", "End"))).actionGet();
        assertThat(countResponse.getCount(), equalTo(1l));
    }

    /**
     * Test case for issue https://github.com/elasticsearch/elasticsearch-mapper-attachments/issues/23
     * <br/>We throw a nicer exception when no content is provided
     * @throws Exception
     */
    @Test(expected = MapperParsingException.class)
    public void testNoContent() throws Exception {
       String mapping = copyToStringFromClasspath("/org/elasticsearch/index/mapper/xcontent/test-mapping.json");

        client().admin().indices().putMapping(putMappingRequest("test").type("person").source(mapping)).actionGet();

        index("test", "person", jsonBuilder().startObject().field("file").startObject().endObject());
    }
    
    @Test
    public void testProblematicAttachment1() throws Exception {
        String mapping = copyToStringFromClasspath("/org/elasticsearch/index/mapper/xcontent/test-mapping.json");
        byte[] html = copyToBytesFromClasspath("/org/elasticsearch/index/mapper/xcontent/9c86adb6adef4c34836f7f4ac072e2c6.pdf");

        client().admin().indices().putMapping(putMappingRequest("test").type("person").source(mapping)).actionGet();

        index("test", "person", jsonBuilder().startObject().field("file", html).endObject());
        refresh();

        CountResponse countResponse = client().count(countRequest("test").query(fieldQuery("file", "fire protection y"))).actionGet();
        assertThat(countResponse.getCount(), equalTo(1l));
    }
    
    @Test
    public void testProblematicAttachment2() throws Exception {
        String mapping = copyToStringFromClasspath("/org/elasticsearch/index/mapper/xcontent/test-mapping.json");
        byte[] html = copyToBytesFromClasspath("/org/elasticsearch/index/mapper/xcontent/b05277ceb7a949268c5a973802603b4e.pdf");

        client().admin().indices().putMapping(putMappingRequest("test").type("person").source(mapping)).actionGet();

        index("test", "person", jsonBuilder().startObject().field("file", html).endObject());
        refresh();

        CountResponse countResponse = client().count(countRequest("test").query(fieldQuery("file", "cargo transport"))).actionGet();
        assertThat(countResponse.getCount(), equalTo(1l));
    }
    
    @Test
    public void testProblematicMP3() throws Exception {
        String mapping = copyToStringFromClasspath("/org/elasticsearch/index/mapper/xcontent/test-mapping.json");
        byte[] html = copyToBytesFromClasspath("/org/elasticsearch/index/mapper/xcontent/corrupt.mp3");

        client().admin().indices().putMapping(putMappingRequest("test").type("person").source(mapping)).actionGet();

        index("test", "person", jsonBuilder().startObject().field("file", html).endObject());
        refresh();

        CountResponse countResponse = client().count(countRequest("test").query(matchAllQuery())).actionGet();
        assertThat(countResponse.getCount(), equalTo(1l));
    }
}
