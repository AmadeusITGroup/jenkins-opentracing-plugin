package com.amadeus.jenkins.opentracing.test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import javax.annotation.Nonnull;
import org.junit.rules.ExternalResource;

public class OutputCollector extends ExternalResource {
  private PrintStream originalOut;
  private PrintStream originalErr;

  private RecordingStream recordedOut;
  private RecordingStream recordedErr;

  @Override
  protected void before() throws Throwable {
    super.before();
    this.originalOut = System.out;
    this.originalErr = System.err;
    recordedOut = new RecordingStream(System.out);
    System.setOut(recordedOut);
    recordedErr = new RecordingStream(System.err);
    System.setErr(recordedErr);
  }

  @Override
  protected void after() {
    System.setOut(originalOut);
    System.setErr(originalErr);

    originalOut = null;
    originalErr = null;

    recordedOut = null;
    recordedErr = null;

    super.after();
  }

  public ByteArrayOutputStream getOut() {
    return recordedOut.log;
  }

  public ByteArrayOutputStream getErr() {
    return recordedErr.log;
  }

  private static class RecordingStream extends PrintStream {
    private final ByteArrayOutputStream log = new ByteArrayOutputStream();

    private RecordingStream(PrintStream orig) {
      super(orig);
    }

    @Override
    public void write(int b) {
      super.write(b);
      log.write(b);
    }

    @Override
    public void write(@Nonnull byte[] buf, int off, int len) {
      super.write(buf, off, len);
      log.write(buf, off, len);
    }
  }
}
