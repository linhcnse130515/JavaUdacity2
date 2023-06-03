package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  @Override
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);

    if (!isProfiled(klass)) {
      throw new IllegalArgumentException("doesn't contain profiled method");
    }

    InvocationHandler invocationHandler = new ProfilingMethodInterceptor(clock, delegate, state);

    @SuppressWarnings("unchecked")
    T proxy = (T) Proxy.newProxyInstance(klass.getClassLoader(), new Class[]{klass}, invocationHandler);
    return proxy;
  }

  private boolean isProfiled(Class<?> checkKlass) {
    Method[] methods = checkKlass.getDeclaredMethods();
    if (methods.length == 0) {
      return false;
    }
    for (Method method : methods) {
      if (method.getAnnotation(Profiled.class) != null) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void writeData(Path path) throws IOException{
    try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
      writeData(writer);
    }
  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
