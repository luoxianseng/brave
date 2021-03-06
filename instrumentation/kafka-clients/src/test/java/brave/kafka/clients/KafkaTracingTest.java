package brave.kafka.clients;

import brave.Span;
import brave.internal.HexCodec;
import brave.propagation.TraceContext;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Test;

import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class KafkaTracingTest extends BaseTracingTest {
  @Test
  public void joinSpan_should_retrieve_span_from_headers() throws Exception {
    addB3Headers(fakeRecord);
    Span span = kafkaTracing.joinSpan(fakeRecord);

    TraceContext context = span.context();
    assertThat(HexCodec.toLowerHex(context.traceId())).isEqualTo(TRACE_ID);
    assertThat(HexCodec.toLowerHex(context.spanId())).isEqualTo(SPAN_ID);
    assertThat(context.sampled()).isEqualTo(true);
  }

  @Test
  public void joinSpan_should_create_span_if_no_headers() throws Exception {
    Span span = kafkaTracing.joinSpan(fakeRecord);

    TraceContext context = span.context();
    assertThat(HexCodec.toLowerHex(context.traceId())).isNotEmpty().isNotEqualTo(TRACE_ID);
    assertThat(HexCodec.toLowerHex(context.spanId())).isNotEmpty().isNotEqualTo(SPAN_ID);
  }

  @Test
  public void nextSpan_should_use_span_from_headers_as_parent() throws Exception {
    addB3Headers(fakeRecord);
    Span span = kafkaTracing.nextSpan(fakeRecord);

    TraceContext context = span.context();
    assertThat(HexCodec.toLowerHex(context.traceId())).isEqualTo(TRACE_ID);
    assertThat(HexCodec.toLowerHex(context.parentId())).isEqualTo(SPAN_ID);
    assertThat(context.sampled()).isEqualTo(true);
  }

  @Test
  public void nextSpan_should_create_span_if_no_headers() throws Exception {
    Span span = kafkaTracing.nextSpan(fakeRecord);

    TraceContext context = span.context();
    assertThat(HexCodec.toLowerHex(context.traceId())).isNotEmpty().isNotEqualTo(TRACE_ID);
    assertThat(HexCodec.toLowerHex(context.spanId())).isNotEmpty().isNotEqualTo(SPAN_ID);
  }

  @Test
  public void nextSpan_should_tag_topic_and_key_when_no_incoming_context() throws Exception {
    kafkaTracing.nextSpan(fakeRecord).start().finish();

    assertThat(spans)
        .flatExtracting(s -> s.tags().entrySet())
        .containsOnly(entry("kafka.topic", TEST_TOPIC), entry("kafka.key", TEST_KEY));
  }

  @Test
  public void nextSpan_shouldnt_tag_null_key() throws Exception {
    fakeRecord = new ConsumerRecord<>(TEST_TOPIC, 0, 1, null, TEST_VALUE);

    kafkaTracing.nextSpan(fakeRecord).start().finish();

    assertThat(spans)
        .flatExtracting(s -> s.tags().entrySet())
        .containsOnly(entry("kafka.topic", TEST_TOPIC));
  }

  @Test
  public void nextSpan_shouldnt_tag_binary_key() throws Exception {
    ConsumerRecord<byte[], String> record =
        new ConsumerRecord<>(TEST_TOPIC, 0, 1, new byte[1], TEST_VALUE);

    kafkaTracing.nextSpan(record).start().finish();

    assertThat(spans)
        .flatExtracting(s -> s.tags().entrySet())
        .containsOnly(entry("kafka.topic", TEST_TOPIC));
  }

  /**
   * We assume topic and key are already tagged by the producer span. However, we can change this
   * policy now, or later when dynamic policy is added to KafkaTracing
   */
  @Test
  public void nextSpan_shouldnt_tag_topic_and_key_when_incoming_context() throws Exception {
    addB3Headers(fakeRecord);
    kafkaTracing.nextSpan(fakeRecord).start().finish();

    assertThat(spans)
        .flatExtracting(s -> s.tags().entrySet())
        .isEmpty();
  }
}