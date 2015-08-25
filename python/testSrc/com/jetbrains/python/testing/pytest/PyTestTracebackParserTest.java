/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.python.testing.pytest;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Base64;
import com.jetbrains.python.traceBackParsers.LinkInTrace;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Ensures we can parse pytest traces for links.
 * This test is JUnit4-based (hence it uses annotations) to support timeout
 *
 * @author Ilya.Kazakevich
 */
public final class PyTestTracebackParserTest {
  private String myStringJunk;
  private String myBase64Junk;

  @Before
  public void setUp() throws Exception {
    // Generate junk to text regexp speed
    final int junkSize = 10000;
    final byte[] junk = new byte[junkSize];
    for (int i = 0; i < junkSize; i++) {
      // We do not care about precision nor security, that junk for tests
      //noinspection NumericCastThatLosesPrecision,UnsecureRandomNumberGeneration
      junk[i] = (byte)(Math.random() * 10);
    }
    final String longString = StringUtil.repeat("1", junkSize);
    myStringJunk = String.format("%s:%s", longString, longString);
    myBase64Junk = Base64.encode(junk);
  }

  /**
   * Ensures we find link in stack trace
   */
  @Test
  public void testLineWithLink() throws Exception {
    final LinkInTrace linkInTrace = new PyTestTracebackParser().findLinkInTrace("foo/bar.py:42 file ");
    Assert.assertNotNull("Failed to parse line", linkInTrace);
    Assert.assertEquals("Bad file name", "foo/bar.py", linkInTrace.getFileName());
    Assert.assertEquals("Bad line number", 42, linkInTrace.getLineNumber());
    Assert.assertEquals("Bad start pos", 0, linkInTrace.getStartPos());
    Assert.assertEquals("Bad end pos", 13, linkInTrace.getEndPos());
  }

  /**
   * lines with out of file references should not have links
   */
  @Test
  public void testLineNoLink() throws Exception {
    Assert.assertNull("File with no lines should not work", new PyTestTracebackParser().findLinkInTrace("foo/bar.py file "));
    Assert.assertNull("No file name provided, but link found", new PyTestTracebackParser().findLinkInTrace(":12 file "));
  }

  /**
   * Ensures
   * Regexp worst cases are limited to prevent freezing on very long lines
   */
  @Test(timeout = 5000)
  public void testExponential() throws Exception {
    Assert
      .assertNull("No link should be found in numbers list", new PyTestTracebackParser().findLinkInTrace(myStringJunk));
    Assert.assertNull("No link should be found in base64 list", new PyTestTracebackParser().findLinkInTrace(myBase64Junk));
  }
}
