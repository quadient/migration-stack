package com.quadient.wfdxml.generator.connection;

import com.quadient.wfdxml.exceptions.IpsConnectionException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

class IpsSocketConnector {
    private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 1000;

    private final BufferedReader reader;
    private final Socket clientSocket;

    IpsSocketConnector(String host, int port) {
        try {
            clientSocket = new Socket();
            clientSocket.connect(new InetSocketAddress(host, port), DEFAULT_CONNECTION_TIMEOUT_MS);
            clientSocket.setTcpNoDelay(true);
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IpsConnectionException("Unexpected connection error on IPS", e);
        }
    }

    public String readOneLine() {
        try {
            return reader.readLine();
        } catch (IOException ex) {
            throw new IpsConnectionException("Reading of line data failed", ex);
        }
    }

    public String readCharacters(int charCount) {
        try {
            char[] buffer = new char[charCount];
            int charsIn = reader.read(buffer, 0, charCount);
            StringBuilder data = new StringBuilder(charsIn);
            data.append(buffer, 0, charsIn);
            return data.toString();
        } catch (IOException ex) {
            throw new IpsConnectionException("Reading of characters failed", ex);
        }
    }

    public void close() {
        try {
            reader.close();
            clientSocket.close();
        } catch (IOException ex) {
            throw new IpsConnectionException("Closing of connector failed", ex);
        }
    }

    public void write(String message) {
        try {
            OutputStream clientSocketOutputStream = clientSocket.getOutputStream();
            clientSocketOutputStream.write(message.getBytes());
            clientSocketOutputStream.flush();
        } catch (IOException ex) {
            throw new IpsConnectionException("Writing of data into socket stream failed", ex);
        }
    }
}