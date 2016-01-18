# TellMeFirst

This transformer is an implementation of TellMeFirst as a Fusepool Transformer. It takes a plain text as input and responds with a JSON containing DBPedia URIs with a score value, representing the main topics of the input text.

More info about the TellMeFirst project: http://tellmefirst.polito.it. More info about Fusepool P3: https://github.com/fusepoolP3.

## Compiling and Running

Compile the project running the command

    mvn install

Start the component with

    java -cp ./target/transformer-tmf-1.0-jar-with-dependencies.jar eu.fusepool.p3.transformer.tmf.TMFTransformer 
  
You can use the transformer for example with curl:

    curl -XPOST -H 'Content-Type: text/plain' -d 'Your plain text here' 'http://localhost:7101'

The tranformer will respond with a JSON with this structure:

```
{
  "@service": "Classify",
  "Resources":   [
        {
      "@uri": "http://dbpedia.org/...",
      "@label": "",
      "@title": "",
      "@score": ""
      "@mergedTypes": "",
      "@image": ""
    },
    ...
  ]
}
```

## Making `TMFTransformer` Asynchronous

`TMFTransformer` is a _synchronous_ transformer i.e., it does not
reply to the client until it is done transforming. if you need it  _asynchronous_ a very simple way to make `TMFTransformer` asynchronous is to change.

`isLongRunning` so that it always returns `true`:

```java
@Override public boolean isLongRunning() { return true; }
```

This will cause our implementation to be wrapped inside an instance of
[`LongRunningTransformerWrapper`](https://github.com/fusepoolP3/p3-transformer-library/blob/master/src/main/java/eu/fusepool/p3/transformer/LongRunningTransformerWrapper.java)
by the runtime, effectively making it asynchronous. Now, when we post
the transformation with `curl`, instead of an immediate response, we
get:

```bash
curl -i -XPOST -H 'Content-Type: text/plain' -d 'Your plain text here' 'http://localhost:7101'
```
Will return something like:

```bash
HTTP/1.1 202 Accepted
Location: /job/f3b6317f-ad60-4f75-b8f1-00a7f2ec4602
```

which means, as per the asynchronous API, that the transformer is now
working on the transformation, and the client is free to go about its
business in the meantime. The path
`/job/f3b6317f-ad60-4f75-b8f1-00a7f2ec4602` represents the
transformation job, and it can be polled by issuing a GET request:

```bash
curl -i -XGET 'http://localhost:8080/job/f3b6317f-ad60-4f75-b8f1-00a7f2ec4602'
```


