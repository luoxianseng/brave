package brave.internal.recorder;

import brave.Span;
import brave.Tracing;
import brave.internal.Platform;
import brave.propagation.TraceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Test;
import zipkin2.Endpoint;

import static org.assertj.core.api.Assertions.assertThat;

public class RecorderTest {
  Endpoint localEndpoint = Platform.get().localEndpoint();
  List<zipkin2.Span> spans = new ArrayList<>();
  TraceContext context = Tracing.newBuilder().build().tracer().newTrace().context();
  Recorder recorder = new Recorder(localEndpoint, () -> 0L, spans::add, new AtomicBoolean(false));

  @After public void close() {
    Tracing.current().close();
  }

  @Test public void finish_calculatesDuration() {
    recorder.start(context, 1L);
    recorder.finish(context, 6L);

    assertThat(spans).extracting(zipkin2.Span::duration)
        .containsExactly(5L);
  }

  @Test public void finish_noop_drops() {
    recorder.noop.set(true);

    recorder.start(context, 1L);
    recorder.finish(context, 6L);

    assertThat(spans).isEmpty();
  }

  @Test public void flush_skipsDuration() {
    recorder.kind(context, Span.Kind.CLIENT);
    recorder.start(context, 1L);
    recorder.flush(context);

    assertThat(spans).extracting(zipkin2.Span::duration)
        .containsNull();
  }

  @Test public void flush_noop_drops() {
    recorder.noop.set(true);

    recorder.kind(context, Span.Kind.CLIENT);
    recorder.start(context, 1L);
    recorder.flush(context);

    assertThat(spans).isEmpty();
  }
}
