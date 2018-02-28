# xke-stream-fighter
[![Build Status](https://travis-ci.org/DivLoic/xke-stream-fighter.svg?branch=master)](https://travis-ci.org/DivLoic/xke-stream-fighter)

This project is related to the talk: [**Processor API**](#/). 
It consist of a few sample of examples demonstrate how the *Streams DSL*,
from [Kafka Streams](https://kafka.apache.org/documentation/streams/),
relies on a lower level api and why this api is expose. While describing
the library, this modules shows a few stream processing concepts. 

### Stream DSL
This higher level API brings the `KStream` & `KTable` abstractions.
It's simple, expressif and declarative. Here is a simple aggregation.

```java
StreamsBuilder builder = new StreamsBuilder();
KTable<String, Arena> arenaTable = builder.table(/** */ "ARENAS", "ARENA-STORE");
KStream<String, Round> winners = builder.stream(/** */ "ROUNDS");
rounds
       .filter((String key, Round round) -> round.getGame() == StreetFighter)
       .map((String key, Round round) -> new KeyValue<>(/*key*/, round.getWinner()))

       .join(arenaTable, Victory::new, Joined.with(/** */))
       .groupByKey().windowedBy(window).count(/** */)
```
But this api won't late you directly access the state of your streaming app. 


### Processor API
```java
public class ProcessPlayer implements Processor<String, Player> {

    private ProcessorContext context;
    private KeyValueStore<String, Arena> store;

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        this.store = (KeyValueStore) context.getStateStore("ARENA-STORE");
        this.context.schedule(500, PunctuationType.WALL_CLOCK_TIME, (timestamp) -> /* do smthg*/ );
    }

    @Override
    public void process(String key, Player value) {
        Arena origin = null;
        KeyValueIterator<String, Arena> iter = this.store.all();
        
        while (iter.hasNext()) {
            KeyValue<String, Arena> entry = iter.next();
            if(entry.key == key){
                origin = entry.value;
            }
        }
        iter.close();
        
        Victory victory = new Victory(value, origin);
        GenericRecord victoryKey = groupedDataKey(victory);
        context.forward(victoryKey, victory);        
    }
}
```

### Token Dispenser

## tl;dr

Prerequisites: 
- sbt
- scala
- docker
```bash
git clone git@github.com:DivLoic/xke-stream-fighter.git
cd xke-stream-fighter
sbt dockerComposeUp
```
This will trigger a set of containers including: the confluent stacks, a dataset generator
and the streaming application example. 

