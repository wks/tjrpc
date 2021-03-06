/**
 * Copyright 2010,2011 Kunshan Wang
 * 
 * This file is part of TJRPC
 * 
 * TJRPC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * TJRPC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with TJRPC.  If not, see <http://www.gnu.org/licenses/>.
 */
package tjrpc.simpletcp.server;

import java.io.*;
import java.net.*;

import com.sdicons.json.model.JSONObject;
import com.sdicons.json.model.JSONValue;

import tjrpc.rpc.RpcRequest;
import tjrpc.rpc.RpcResponse;
import tjrpc.server.AbstractRpcServer;
import tjrpc.simpletcp.channel.*;
import tjrpc.simpletcp.util.SimpleTcpServer;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

public class SocketRpcServer extends AbstractRpcServer {
	static final Logger logger = LogManager
			.getLogger(SocketRpcServer.class);

	TCPServer server;
	public SocketRpcServer(int port) throws IOException {
		server = new TCPServer(port);
	}

	public SocketRpcServer(ServerSocket serverSocket) {
		server = new TCPServer(serverSocket);
	}

	private class TCPServer extends SimpleTcpServer {

		public TCPServer(int port) throws IOException {
			super(port);
		}

		public TCPServer(ServerSocket serverSocket) {
			super(serverSocket);
		}

		@Override
		public void handleClient(Socket clientSocket) {
			SocketJsonChannel channel = new SocketJsonChannel(clientSocket);
			try {
				while (true) {
					JSONValue jRequest = channel.read();
					if (jRequest == null) {
						// remote closed
						break;
					}
					RpcRequest request = serializer.jsonToRequest(jRequest);
					logger.info(String.format("Request from %s: %s.%s(...)",
							clientSocket.getInetAddress().toString(),
							request.getObject(), request.getMethod()));
					RpcResponse response = callableService.invoke(request);
					JSONObject jResponse = serializer.responseToJson(response);
					channel.write(jResponse);
				}
			} catch (JsonIOException e) {
				logger.error(String.format("Connection is broken from %s",
						clientSocket.getInetAddress().toString()), e);
			} catch (IllegalArgumentException e) {
				logger.error(String.format("Bad request from %s",
						clientSocket.getInetAddress().toString()), e);
				channel.close();
			} finally {
				logger.info(String.format("Connection closing %s",
						clientSocket.getInetAddress().toString()));
				channel.close();
			}
		}
	}
	
	@Override
	public void start() {
		server.start();
		logger.info("RPC Server started.");
	}

	@Override
	public void stop() {
		server.stop();
		logger.info("RPC Server stopped.");
	}
}