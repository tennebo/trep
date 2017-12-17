# InfluxDB Time-series Schema

InfluxDB is schema-less, but conceptually our schema looks like this:

Measurement name: quote

 * The ```time``` column is always there, and its format is ISO 8601 UTC
 * ```last```, ```bid``` and ```ask``` are _fields_, and their field values are the observed quotes
 * ```ticker``` and ```exchange``` are _tags_
 
Tag keys and tag values are strings and record metadata.

Tags are indexed, fields are not. This means that queries on tags will be fast, while queries on fields will result 
in a table scan. In other words, a query on fields will be slow as molasses.

| time | last | bid | ask | ticker | exchange |
| :--- | :---: | :---: | :---: | :--- | :--- |
| 2017-12-31T09:09:00Z | 65.02 | 64.00 | 65.00 | SLB | N |
| 2017-12-31T09:09:02Z | 65.00 | 64.00 | 64.80 | SLB | N |
| 2017-12-31T09:09:03Z | 90.50 | 90.00 | 90.80 | IBM | N |
