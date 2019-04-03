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

package com.wacai.open.baige.remoting.netty;

public class NettySystemConfig {
    public static final String SystemPropertyNettyPooledByteBufAllocatorEnable =
            "com.wacai.baige.remoting.nettyPooledByteBufAllocatorEnable";
    public static boolean NettyPooledByteBufAllocatorEnable = //
            Boolean
                .parseBoolean(System.getProperty(SystemPropertyNettyPooledByteBufAllocatorEnable, "false"));

    public static final String SystemPropertySocketSndbufSize = //
            "com.wacai.baige.remoting.socket.sndbuf.size";
    public static int SocketSndbufSize = //
            Integer.parseInt(System.getProperty(SystemPropertySocketSndbufSize, "65535"));

    public static final String SystemPropertySocketRcvbufSize = //
            "com.wacai.baige.remoting.socket.rcvbuf.size";
    public static int SocketRcvbufSize = //
            Integer.parseInt(System.getProperty(SystemPropertySocketRcvbufSize, "65535"));

    public static final String SystemPropertyClientAsyncSemaphoreValue = //
            "com.wacai.baige.remoting.clientAsyncSemaphoreValue";
    public static int ClientAsyncSemaphoreValue = //
            Integer.parseInt(System.getProperty(SystemPropertyClientAsyncSemaphoreValue, "2048"));

    public static final String SystemPropertyClientOnewaySemaphoreValue = //
            "com.wacai.baige.remoting.clientOnewaySemaphoreValue";
    public static int ClientOnewaySemaphoreValue = //
            Integer.parseInt(System.getProperty(SystemPropertyClientOnewaySemaphoreValue, "4096"));
}
