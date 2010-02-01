/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.auth;

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobExpiredException;
import org.apache.shindig.common.crypto.Crypto;
import org.apache.shindig.common.util.FakeTimeSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests BlobCrypterSecurityToken
 */
public class BlobCrypterSecurityTokenTest {

  private static final String CONTAINER = "container";
  private static final String DOMAIN = "example.com";

  private FakeTimeSource timeSource = new FakeTimeSource();
  private BasicBlobCrypter crypter;

  @Before
  public void setUp() {
    crypter = new BasicBlobCrypter(Crypto.getRandomBytes(20));
    crypter.timeSource = timeSource;
  }

  @Test(expected=UnsupportedOperationException.class)
  public void testNullValues() throws Exception {
    BlobCrypterSecurityToken t = new BlobCrypterSecurityToken(crypter, CONTAINER, DOMAIN);
    String token = t.encrypt();
    assertTrue("should start with container: " + token, token.startsWith("container:"));
    String[] fields = StringUtils.split(token, ':');
    BlobCrypterSecurityToken t2 =
        BlobCrypterSecurityToken.decrypt(crypter, CONTAINER, DOMAIN, fields[1], null);
    assertNull(t2.getAppId(), t2.getAppId());
    assertNull(t2.getAppUrl(), t2.getAppUrl());
    assertEquals(DOMAIN, t2.getDomain());
    assertEquals(0, t2.getModuleId());
    assertNull(t2.getOwnerId(), t2.getOwnerId());
    assertNull(t2.getViewerId(), t2.getViewerId());
    assertNull(t2.getTrustedJson(), t2.getTrustedJson());
    assertNull(t2.getUpdatedToken(), t2.getUpdatedToken());
    assertEquals(CONTAINER, t2.getContainer());

    // expect an exception
    t2.getActiveUrl();
  }

  @Test
  public void testRealValues() throws Exception {
    BlobCrypterSecurityToken t = new BlobCrypterSecurityToken(crypter, CONTAINER, DOMAIN);
    t.setAppUrl("http://www.example.com/gadget.xml");
    t.setModuleId(12345L);
    t.setOwnerId("owner");
    t.setViewerId("viewer");
    t.setTrustedJson("trusted");
    String token = t.encrypt();
    assertTrue("should start with container: " + token, token.startsWith("container:"));
    String[] fields = StringUtils.split(token, ':');
    BlobCrypterSecurityToken t2 =
        BlobCrypterSecurityToken.decrypt(crypter, CONTAINER, DOMAIN, fields[1], "active");
    assertEquals("http://www.example.com/gadget.xml", t2.getAppId());
    assertEquals("http://www.example.com/gadget.xml", t2.getAppUrl());
    assertEquals(DOMAIN, t2.getDomain());
    assertEquals(12345L, t2.getModuleId());
    assertEquals("owner", t2.getOwnerId());
    assertEquals("viewer", t2.getViewerId());
    assertEquals("trusted", t2.getTrustedJson());
    assertEquals(CONTAINER, t2.getContainer());
    assertEquals("active", t2.getActiveUrl());
  }

  @Test(expected=BlobExpiredException.class)
  public void testExpired() throws Exception {
    BlobCrypterSecurityToken t = new BlobCrypterSecurityToken(crypter, CONTAINER, DOMAIN);
    String token = t.encrypt();
    // one hour plus clock skew
    timeSource.incrementSeconds(3600 + 181);
    String[] fields = StringUtils.split(token, ':');

    // expect an exception
    BlobCrypterSecurityToken.decrypt(crypter, CONTAINER, DOMAIN, fields[1], "active");
  }
}
