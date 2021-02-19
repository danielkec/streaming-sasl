## Build and run

With JDK11+
```bash
mvnd clean install -Pnative-image

# Provide sasl password
export OCI_AUTH_TOKEN=secret_token

# Listening
./target/streaming-sasl

# Sending
./target/streaming-sasl juhuu it works just great
```

```bash 
kec@vulcan:~/tmp/native/streaming-sasl$ ./target/streaming-sasl 
2021.02.19 13:49:37 INFO Multi.log(1) Thread[main,5,main]:  ⇘ onSubscribe(...)
2021.02.19 13:49:37 INFO Multi.log(1) Thread[main,5,main]:  ⇗ request(Long.MAX_VALUE)
2021.02.19 13:49:57 INFO Multi.log(1) Thread[kafka-1,5,main]:  ⇘ onNext(jujuu)
Received> jujuu
2021.02.19 13:49:57 INFO Multi.log(1) Thread[kafka-1,5,main]:  ⇘ onNext(it)
Received> it
2021.02.19 13:49:57 INFO Multi.log(1) Thread[kafka-1,5,main]:  ⇘ onNext(works)
Received> works
2021.02.19 13:49:57 INFO Multi.log(1) Thread[kafka-1,5,main]:  ⇘ onNext(just)
Received> just
2021.02.19 13:49:57 INFO Multi.log(1) Thread[kafka-1,5,main]:  ⇘ onNext(great)
Received> great
```