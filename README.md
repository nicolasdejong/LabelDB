# LabelDB

Simple database to associate a (resource-)index with a date and zero or more labels.
Search queries are then supported to find the indices for the requested labels and date.

### Context

I'm working on a project to store resources, label them and find them back using search-queries
using the labels and dates in a web frontend. This is a major part of this project which is
divided in parts.

### Goal

The goal for this database is to use as little memory as possible and be as fast as possible.
It tries to accomplish this by storing the labels as runlength-compressed (per 1KB) bitmaps
(both stored as in-memory). Because the compression is per 1KB block for equal values (0 or 1)
search will be fast as well.

Memory use depends on dispersion of the labels over the indices (for the run-length encoding).
Assumption is that generally labels will be clumped together leading to a compression ratio of
about 75%. This will lead to an average memory use of 32KB per label for a database with 1M indices.
So storage and memory use of a database with 1M indices and 1000 labels will be about 32MB (and
so 3.2MB for a database size of 100.000 indices).

Performance of the database is reasonably fast. On my low-power i7 (8665U@1.8Ghz) a database
can be filled with 1 million indices each holding 20 (of 1000) random labels in about 9 seconds.
A search for all indices holding a random 20 labels takes about 14ms. This includes compiling
a count of all labels the found records hold. (see below for more details on the find query and
result).

All code is fully unit-tested. LOC is about 1100.

### Use

Create a LabelDB instance and provide it with a directory to store its data. Then add a date
and labels for an index number. Then use a query to find results:

```Java
final LabelDB db = new LabelDB(dir);

db.set(0, 2019_06_01__11_22_33_444L, "a", "b");
db.set("c", 1, 5, 7, 8);

db.find("a b <=2019").indices
db.find("a b <=2019").resultCountPerLabel
```

### Search Queries

Indices (record numbers) can be retrieved by providing a search query.
The search query can contain labels and date ranges. AND, OR and NOT are supported as follows:

```
 AND   - N N N
       - N & N & N
       - N AND N AND N
 OR:   - N, N, N
       - N | N | N
       - N || N || N
       - N OR N OR N
 NOT   - !N
       - NOT N
 GROUP - (N)
 N     - labeltext
       - "quoted spaced label text"
       - DATE
       - (N)
 DATE in the form yyyy.mm.dd.hh.MM.ss.nnn (year,month,day,hour,minutes,seconds,ms)
      where optional separator is one of: . - : _
      where everything is optional, except year
       - date     will be replaced by range, e.g. 201906 will become >=20190601000000 & <=20190630235959
       - <date    will be replaced by the minimum for given, e.g. <2019 will become <20190101000000
       - <=date   will be replaced by the maximum for given, e.g. <=2019 will become <=20191231235959
       - >date    will be replaced by the maximum for given
       - >=date   will be replaced by the minimum for given 
       - date1..date2  equal to >= date1 AND <= date2

 Examples:

 A, B, C           -> A OR B OR C
 A B C             -> A AND B AND C
 A (B, C D)        -> A AND (B OR C AND D)
 (A & B) | (C & D) -> (A AND B) OR (C AND D)
 A B C <2019.6.5   -> A AND B AND C AND <20190605000000000
```

The result of the search queries is indices and label count:

- __indices__ is the list of record indices found for the requested query.
- __resultCountPerLabel__ is the map of label to the number of times that label appears in the records of the
  search results. This probably holds more labels than in the query, because found records typically
  have more labels than the ones searched for.
