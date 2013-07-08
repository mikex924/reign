/*
 Copyright 2013 Yen Pai ypai@reign.io

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package io.reign;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @author ypai
 * 
 */
public class DefaultPathSchemeTest {
    private DefaultPathScheme pathScheme;

    @Before
    public void setUp() throws Exception {
        pathScheme = new DefaultPathScheme(Reign.DEFAULT_FRAMEWORK_BASE_PATH, Reign.DEFAULT_FRAMEWORK_CLUSTER_ID);
    }

    @Test
    public void testGetParentPath() throws Exception {
        String value;
        value = pathScheme.getParentPath("/a/b/c");
        assertTrue("/a/b".equals(value));

        try {
            value = pathScheme.getParentPath("/a/b/c/");
            assertTrue("Should not get here", false);
        } catch (Exception e) {
        }

        value = pathScheme.getParentPath("a/b/c");
        assertTrue("a/b".equals(value));

        try {
            value = pathScheme.getParentPath("a/b/c/");
            assertTrue("Should not get here", false);
        } catch (Exception e) {
        }

    }

    @Test
    public void testJoin() throws Exception {

        String value;

        value = pathScheme.joinPaths("/this/that", "and/how");
        assertTrue("Unexpected value:  " + value, "/this/that/and/how".equals(value));

        value = pathScheme.joinPaths("/this/that", "/and/how");
        assertTrue("Unexpected value:  " + value, "/this/that/and/how".equals(value));

        try {
            value = pathScheme.joinPaths("/this/that", "/and/how/");
            assertTrue("Should not get here", false);
        } catch (Exception e) {
        }

        try {
            value = pathScheme.joinPaths("/this/that", "and/how/");
            assertTrue("Should not get here", false);
        } catch (Exception e) {
        }
    }

    @Test
    public void testToPathToken() throws Exception {
        String value;
        value = pathScheme.toPathToken(new DefaultCanonicalId("1234", "1.2.3.4", "localhost", 4321, 9876));

        // System.out.println(value);

        CanonicalId id = pathScheme.parseCanonicalId(value);

        assertTrue("1234".equals(id.getProcessId()));
        assertTrue("1.2.3.4".equals(id.getIpAddress()));
        assertTrue("localhost".equals(id.getHost()));
        assertTrue(4321 == id.getPort());
        assertTrue(9876 == id.getMessagingPort());
    }

}