/** *****************************************************************************
 * Copyright (c) 2009, 2019 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 ****************************************************************************** */
package com.test.jacoco.server;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.ISessionInfoVisitor;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.runtime.RemoteControlReader;
import org.jacoco.core.runtime.RemoteControlWriter;

/**
 * This example starts a socket server to collect coverage from agents that run
 * in output mode <code>tcpclient</code>. The collected data is dumped to a
 * local file.
 *
 * Modified by: cjayawickrema
 */
public final class ExecutionDataServer {

    private static final String DESTFILE = "jacoco-server.exec";

    private static final String ADDRESS = "localhost";

    private static final int PORT = 6300;

    /**
     * Start the server as a standalone program.
     *
     * @param args
     * @throws IOException
     */
    public static void main(final String[] args) throws IOException {
        final ExecutionDataWriter fileWriter = new ExecutionDataWriter(
                new FileOutputStream(DESTFILE));
        final ServerSocket server = new ServerSocket(PORT, 0, InetAddress.getByName(ADDRESS));
        System.out.println("Jacoco Server started");
        while (true) {
            System.out.println("Initiating handler...");
            final Handler handler = new Handler(server.accept(), fileWriter);
            new Thread(handler).start();
            System.out.println("Started new handler");
        }
    }

    private static class Handler implements Runnable, ISessionInfoVisitor, IExecutionDataVisitor {

        private final Socket socket;

        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        private final RemoteControlReader remoteControlReader;
        private final RemoteControlWriter remoteControlWriter;

        private final ExecutionDataWriter fileWriter;

        Handler(final Socket socket, final ExecutionDataWriter fileWriter)
                throws IOException {
            this.socket = socket;
            this.fileWriter = fileWriter;

            remoteControlWriter = new RemoteControlWriter(socket.getOutputStream());
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    System.out.println("Sending dump command...");
                    remoteControlWriter.visitDumpCommand(true, false);
                } catch (IOException ex) {
                    System.out.println("Failed to send dump command: " + ex.getMessage());
                }
            }, 10, 10, TimeUnit.SECONDS);

            remoteControlReader = new RemoteControlReader(socket.getInputStream());
            remoteControlReader.setSessionInfoVisitor(this);
            remoteControlReader.setExecutionDataVisitor(this);
        }

        public void run() {
            try {
                while (remoteControlReader.read()) {
                }
                socket.close();
                synchronized (fileWriter) {
                    fileWriter.flush();
                }
            } catch (final IOException e) {
                System.err.println("Failed to collect: " + e.getMessage());
            }
        }

        public void visitSessionInfo(final SessionInfo info) {
            synchronized (fileWriter) {
                fileWriter.visitSessionInfo(info);
            }
        }

        public void visitClassExecution(final ExecutionData data) {
            synchronized (fileWriter) {
                fileWriter.visitClassExecution(data);
            }
        }
    }

    private ExecutionDataServer() {
    }
}
