package com.sgitmanagement.expressoext.networkfile;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.StreamingOutput;

public class NetworkFileStreamer implements StreamingOutput {

	private int length;
	private RandomAccessFile raf;
	final byte[] buf = new byte[4096];

	public NetworkFileStreamer(int length, RandomAccessFile raf) {
		this.length = length;
		this.raf = raf;
	}

	@Override
	public void write(OutputStream outputStream) throws IOException, WebApplicationException {
		try {
			while (length != 0) {
				int read = raf.read(buf, 0, buf.length > length ? length : buf.length);
				outputStream.write(buf, 0, read);
				length -= read;
			}
		} finally {
			raf.close();
		}
	}

	public int getLenth() {
		return length;
	}
}