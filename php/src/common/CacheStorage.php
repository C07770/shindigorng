<?php
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

abstract class CacheStorage {
  abstract public function store($key, $value);

  abstract public function fetch($key);

  abstract public function delete($key);

  abstract public function lock($key);

  abstract public function unlock($key);

  abstract public function isLocked($key);

  public function waitForLock($key) {
    $tries = 10;
    $cnt = 0;
    do {
      usleep(100);
      $cnt ++;
    } while ($cnt <= $tries && $this->isLocked());
    if ($this->isLocked()) {
      $this->unlock($key);
    }
  }
}