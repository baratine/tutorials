package tutorial.pubsub;

import io.baratine.core.ResultStream;

public interface TweetService
{
  void subscribe(ResultStream<String> result);

  void publish(String tweet);
}
