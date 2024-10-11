# iCal4j recurrence bug

This repository contains a minimal example for reproducing what we believe to be a bug in the [ical4j](https://www.ical4j.org/) package.

# What bug?

We encounter an incorrect recurrence set when generating recurrences using the `calculateRecurrenceSet` function. Recurrences generated for a `AEDT` timezone offset (+11:00 GMT) while your system is in a `AEST` timezone offset (+10:00 GMT) are shifted an hour later.

Conditions:
 - Recurrences need to be generated for a period in the `AEDT` timezone offset (+11:00 GMT).
 - Local time needs to be in the `AEST` timezone offset (+10:00 GMT).

Local time is mocked using bash's `date` helper. For example, setting your computer to a `AEST` such as 1:45pm 4th October 2024 can be achieved with:

```bash
sudo date 1004134524
```

We also observed this bug happening at that time - **not just by hacking the local time**.

This might be affecting other timezones with daylight savings changes however we have not tested.

# Setup

Build the project with maven:

```bash
mvn package
```

You can then run the project with:

```bash
java -jar target/ical4j-example-1.0-SNAPSHOT.jar
```

You should see some logging output.

# Steps to reproduce the bug

The code in `src/main/java/com/example/App.java` contains a minimal example, largely inspired by [the example from the documentation](https://www.ical4j.org/examples/recur/). We create a calendar with a single event that recurs between 2pm and 8pm Australia/Sydney.

1. Set local time to `AEST` (+10:00) e.g with `sudo date 1004134524`.
2. Build and run the project (`mvn package; java -jar target/ical4j-example-1.0-SNAPSHOT.jar`).

Observed output:

```
Australia/Sydney: 2024-11-01T14:00+10:00[ical4j~0831be67-2e60-4a77-974e-4878e71e5569]
UTC:              2024-11-01T04:00:00Z
```

You can see that the `ZonedDateTime` timezone offset from the timezone `ical4j~0831be67-2e60-4a77-974e-4878e71e5569` is incorrectly `+10:00`. 1st November 2024 Australia/Sydney is during daylight time (`AEDT`) which is `+11:00`. To confirm this recurrence is wrong, the `UTC` time shows `2024-11-01T04:00:00Z` which converts to **1st November 3pm** AEDT (`+11:00`), even though the recurrence is specified to start at 2pm. The end time is also an hour later (9pm, not 8pm).

Interestingly, the bug **does not occur** when your machine is in the `AEDT` timezone:

1. Set local time to `AEDT` (+11:00) e.g with `sudo date 1104134524`.
2. Run the project (`java -jar target/ical4j-example-1.0-SNAPSHOT.jar`).

Observed output:

```
Australia/Sydney: 2024-11-01T14:00+11:00[ical4j~8335e7f0-7121-4869-b391-9ab64cf53e40]
UTC:              2024-11-01T03:00:00Z
```

Notice how the timezone offset is now `+11:00` and the `UTC` time produced is `2024-11-01T03:00:00Z` which converts to **1st November 2pm** AEDT (`+11:00`).

# Some additional observations

You might think `ical4j` is only using the timezone offset that it's currently in, and ignoring offset changes. This isn't the case, as even if your local time is `AEDT` (+11:00) and you generate recurrences for `AEST` (+10:00), they generate correctly.

To reproduce this:

1. Set local time to `AEDT` (+11:00) e.g with `sudo date 1104134524`.
2. Change lines 28-29 to be a period in `AEST` e.g:
    ```java
    ZonedDateTime from = ZonedDateTime.of(2024, 10, 1, 0, 0, 0, 0, timezone);
    ZonedDateTime to = ZonedDateTime.of(2024, 10, 2, 0, 0, 0, 0, timezone);
    ```
3. Build and run the project (`mvn package; java -jar target/ical4j-example-1.0-SNAPSHOT.jar`).

Observed output:

```
Australia/Sydney: 2024-10-01T14:00+10:00[ical4j~73ee0cde-e57c-44f2-a733-e4d462e2bcd2]
UTC:              2024-10-01T04:00:00Z
```

You can see that the recurrence start was correctly generated at 1st October 2pm AEST (+10:00).

# Our investigation

We've been looking into the `calculateRecurrenceSet` function and timezone instances from the `TimezoneRegistry`. The timezone has the correct `Observance` for Australia/Sydney daylight savings changes (based on `vTimeZone.getApplicableObservance`).

There is some interesting behaviour with the `TimeZone.getOffset(date: Int)` function:

```java
ZoneId timezone = ZoneId.of("Australia/Sydney");
TimeZoneRegistry tzRegistry = TimeZoneRegistryFactory.getInstance().createRegistry();
TimeZone tz = tzRegistry.getTimeZone(timezone.getId());

ZonedDateTime aestDateTime = ZonedDateTime.of(2024, 10, 1, 0, 0, 0, 0, timezone);
ZonedDateTime aedtDateTime = ZonedDateTime.of(2024, 11, 1, 0, 0, 0, 0, timezone);
System.out.println("AEST offset hours: " + (tz.getOffset(aestDateTime.toEpochSecond()) / 1000 / 60 / 60));
System.out.println("AEDT offset hours: " + (tz.getOffset(aedtDateTime.toEpochSecond()) / 1000 / 60 / 60));
```

Observed output:

```
AEST offset hours: 10
AEDT offset hours: 10
```

`AEDT` should be `11`. This happens regardless of your local time. This could be a red herring but it's something we found.

# Version information

Here is some information on most of the dependencies

## ical4j

Version [4.0.4](https://mvnrepository.com/artifact/org.mnode.ical4j/ical4j/4.0.4).

(Also reproduced with `3.2.19`)

## Maven CLI

```bash
mvn --version

Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)
Maven home: /opt/homebrew/Cellar/maven/3.9.9/libexec
Java version: 23, vendor: Homebrew, runtime: /opt/homebrew/Cellar/openjdk/23/libexec/openjdk.jdk/Contents/Home
Default locale: en_AU, platform encoding: UTF-8
OS name: "mac os x", version: "15.0.1", arch: "aarch64", family: "mac"
```

## Java

```bash
java --version

openjdk 17.0.6 2023-01-17
OpenJDK Runtime Environment Temurin-17.0.6+10 (build 17.0.6+10)
OpenJDK 64-Bit Server VM Temurin-17.0.6+10 (build 17.0.6+10, mixed mode)
```

## MacOS

```bash
sw_vers

ProductName:		macOS
ProductVersion:		15.0.1 (Sequoia)
BuildVersion:		24A348
```
