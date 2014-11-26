rt-order-reporting
==================

[![Build Status](https://travis-ci.org/prystupa/rt-order-reporting.png)](https://travis-ci.org/prystupa/rt-order-reporting)

The 'rt-order-reporting' POC aims to quickly prototype a distributed realtime solution to large volume (hundreds of millions of events daily) order reporting problem.

Order reporting include the following key steps:
  
  - Ingestion - collecting order events from trading systems (simulated in the POC)
  - Linking - organizing related order events in chains based on their parent/child relationship; think of a chain a potentially very large order trees (thousands of events)
  - Enrichment - computing additional properties for complete chains
  - Record extraction and validation - running external set of rules to validate reported data before delivery
  - FTP delivery - writing validated reporting records to an FTP stream

The idea is to be able to scale inifinitly by processing events on a large distributed grid, in realtime. We also want levarage data locality - we can partition related orders such that all orders from the same chain are stored and processed on the same node - to minimize network chatter.

Hazelcast is used in this POC as in-memory distributed grid.

Currently implemented ideas are documeted below.

## Data model
An order event is modeled as a simple tuple (ID, parent ID, partition key). Partition key is an attribute of an oder that guarantees orders from the same chain map to the same partition. Product ID (ticker) is an example of such a key in real life.

## Ingestion
We use a partition aware command executin (StoreCommand) to siubmit simulated order event to the grid. The command recieves an event tuple and executes on appropriate node. Upon execution it updates two maps:
- parents - ID -> parent ID association
- chains - multimap of (root ID -> ID)
When new event tuple arrives, Store command pessimistically assumes its parent ID is also its chain root ID

