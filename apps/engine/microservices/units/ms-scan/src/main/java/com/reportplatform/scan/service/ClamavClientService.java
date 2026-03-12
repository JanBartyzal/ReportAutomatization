package com.reportplatform.scan.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Service for interacting with ClamAV daemon via TCP socket.
 */
@Service
public class ClamavClientService {
    private static final Logger logger = LoggerFactory.getLogger(ClamavClientService.class);

    @Value("${clamav.host:localhost}")
    private String host;

    @Value("${clamav.port:3310}")
    private int port;

    @Value("${clamav.timeout:30000}")
    private int timeout;

    /**
     * Scan a file stream for viruses.
     * 
     * @return Scan result - "OK" if clean, otherwise contains threat name
     */
    public ScanResult scanFile(InputStream fileStream) throws IOException {
        logger.info("Scanning file with ClamAV at {}:{}", host, port);

        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(timeout);

            // Send INSTREAM command
            OutputStream out = socket.getOutputStream();
            out.write("zINSTREAM\0".getBytes());
            out.flush();

            // Send file data in chunks
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileStream.read(buffer)) != -1) {
                // Send chunk length + data
                byte[] chunk = new byte[bytesRead + 4];
                chunk[0] = (byte) ((bytesRead >> 24) & 0xFF);
                chunk[1] = (byte) ((bytesRead >> 16) & 0xFF);
                chunk[2] = (byte) ((bytesRead >> 8) & 0xFF);
                chunk[3] = (byte) (bytesRead & 0xFF);
                System.arraycopy(buffer, 0, chunk, 4, bytesRead);
                out.write(chunk);
            }

            // Send end marker (length = 0)
            out.write(new byte[] { 0, 0, 0, 0 });
            out.flush();

            // Read response
            InputStream in = socket.getInputStream();
            StringBuilder response = new StringBuilder();
            int b;
            while ((b = in.read()) != -1 && b != 0) {
                response.append((char) b);
            }

            String result = response.toString().trim();
            logger.info("ClamAV scan result: {}", result);

            if (result.startsWith("OK")) {
                return new ScanResult(true, null);
            } else if (result.startsWith("FOUND")) {
                // Extract threat name
                String threat = result.replace("FOUND", "").trim();
                return new ScanResult(false, threat);
            } else {
                return new ScanResult(false, "UNKNOWN: " + result);
            }
        }
    }

    public record ScanResult(boolean clean, String threatName) {
    }
}
