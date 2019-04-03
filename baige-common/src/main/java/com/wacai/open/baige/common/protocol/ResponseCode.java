/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wacai.open.baige.common.protocol;



public class ResponseCode extends RemotingSysResponseCode {
    public static final byte FLUSH_DISK_TIMEOUT = 10;
    public static final byte SLAVE_NOT_AVAILABLE = 11;
    public static final byte FLUSH_SLAVE_TIMEOUT = 12;
    public static final byte MESSAGE_ILLEGAL = 13;
    public static final byte SERVICE_NOT_AVAILABLE = 14;
    public static final byte VERSION_NOT_SUPPORTED = 15;
    public static final byte NO_PERMISSION = 16;
    public static final byte TOPIC_NOT_EXIST = 17;
    public static final byte TOPIC_EXIST_ALREADY = 18;
    public static final byte PULL_NOT_FOUND = 19;
    public static final byte PULL_RETRY_IMMEDIATELY = 20;
    public static final byte PULL_OFFSET_MOVED = 21;
    public static final byte QUERY_NOT_FOUND = 22;
    public static final byte SUBSCRIPTION_PARSE_FAILED = 23;
    public static final byte SUBSCRIPTION_NOT_EXIST = 24;
    public static final byte SUBSCRIPTION_NOT_LATEST = 25;
    public static final byte SUBSCRIPTION_GROUP_NOT_EXIST = 26;
    public static final byte TRANSACTION_SHOULD_COMMIT = 27;
    public static final byte TRANSACTION_SHOULD_ROLLBACK = 28;
    public static final byte TRANSACTION_STATE_UNKNOW = 29;
    public static final byte TRANSACTION_STATE_GROUP_WRONG = 30;
    public static final byte NO_BUYER_ID = 31;

    public static final byte NOT_IN_CURRENT_UNIT = 32;

    public static final byte CONSUMER_NOT_ONLINE = 33;

    public static final byte CONSUME_MSG_TIMEOUT = 34;

    public static final byte NO_MESSAGE = 35;

    public static final byte CLIENT_NOT_AUTHORIZED = 36;

    public static final byte AUTH_SYS_ERROR = 37;

    public static final byte TOPIC_NOT_AUTHORIZED = 38;

    public static final byte MSG_BODY_EXCEED_LIMIT = 39;


    public static final byte TOPIC_NOT_ACK=40;





}
